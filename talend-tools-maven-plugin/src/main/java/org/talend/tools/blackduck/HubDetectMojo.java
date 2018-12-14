/**
 * Copyright (C) 2017 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.tools.blackduck;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.VERIFY;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.shared.artifact.deploy.ArtifactDeployer;
import org.apache.maven.shared.artifact.deploy.ArtifactDeployerException;
import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.maven.shared.utils.io.IOUtil;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.google.gson.GsonBuilder;

/**
 * Download if not already cached in maven repository and execute blackduck hub-detect.
 */
@Mojo(name = "hub-detect", defaultPhase = VERIFY, threadSafe = true)
public class HubDetectMojo extends BlackduckBase {

    /**
     * Where the hub-detect jar will be put for the execution.
     */
    @Parameter(property = "hub-detect.hubDetectCache", defaultValue = "${project.build.directory}/blackduck/hub-detect.jar")
    private File hubDetectCache;

    /**
     * Where the scan-cli binary will be put for the execution.
     */
    @Parameter(property = "hub-detect.hubDetectCache", defaultValue = "${project.build.directory}/blackduck/scan-cli")
    private File scanCliCache;

    /**
     * In which (artifactory) repository the jar can be found.
     */
    @Parameter(property = "hub-detect.artifactoryBase", defaultValue = "https://test-repo.blackducksoftware.com/artifactory")
    private String artifactoryBase;

    /**
     * What is the query used to get the last version of hub-detect. Passed variables are the repository base, group, artifact and
     * repo.
     */
    @Parameter(property = "hub-detect.latestVersionUrl", defaultValue = "%s/api/search/latestVersion?g=%s&a=%s&repos=%s")
    private String latestVersionUrl;

    /**
     * The jar coordinates. You can use it to fix the version of hub-detect.
     */
    @Parameter(property = "hub-detect.executableGav", defaultValue = "com.blackducksoftware.integration:hub-detect:latest")
    private String executableGav;

    /**
     * The scan cli (cache) coordinates.
     */
    @Parameter(property = "hub-detect.scanCliDownloadUrl", defaultValue = "https://blackduck.talend.com/download/scan.cli.zip")
    private String scanCliDownloadUrl;

    /**
     * Should scan cli be used offline.
     */
    @Parameter(property = "hub-detect.scanCliOffline", defaultValue = "false")
    private boolean scanCliOffline;

    /**
     * The jar coordinates. You can use it to fix the version of hub-detect.
     */
    @Parameter(property = "hub-detect.scanCliGav", defaultValue = "com.blackducksoftware.integration:scan-cli:latest")
    private String scanCliGav;

    /**
     * Allows to force to redownload scancli.
     */
    @Parameter(property = "hub-detect.forceScanCliDownload", defaultValue = "false")
    private boolean forceScanCliDownload;

    /**
     * The repository to use to download the executable jar.
     */
    @Parameter(property = "hub-detect.artifactRepositoryName", defaultValue = "bds-integrations-release")
    private String artifactRepositoryName;

    /**
     * The log level used for the inspection.
     */
    @Parameter(property = "hub-detect.logLevel", defaultValue = "INFO")
    private String logLevel;

    /**
     * Should the exit code of hub-detect be validated. Can be true or any int. If true, 0 will be tested otherwise
     * the passed value. Any other value will be considered as no validation to execute.
     */
    @Parameter(property = "hub-detect.validateExitCode", defaultValue = "0")
    private String validateExitCode;

    /**
     * The scope used for the detection. It is common to not desire provided.
     */
    @Parameter(property = "hub-detect.scope", defaultValue = "runtime")
    private String scope;

    /**
     * Let you add system properties on hub-detect execution.
     */
    @Parameter
    private Map<String, String> systemVariables;

    /**
     * Let you add environment variables on hub-detect execution.
     */
    @Parameter
    private Map<String, String> environment;

    /**
     * Let you exclude files with an absolute path resolution from relative path.
     * Avoid headache with hub-detect configuration.
     */
    @Parameter
    private Collection<String> exclusions;

    @Component
    private ArtifactResolver resolver;

    @Component
    private ArtifactDeployer deployer;

    @Override
    public void doExecute(final MavenProject rootProject, final Server server)
            throws MojoExecutionException, MojoFailureException {

        if (blackduckName == null) {
            getLog().error("No name specified, please set blackduckName");
            return;
        }

        final List<RemoteRepository> repositories = new ArrayList<>(rootProject.getRemoteProjectRepositories().size() + 1);
        repositories.add(new RemoteRepository.Builder("blackduck_" + getClass().getName(), "default",
                artifactoryBase + '/' + artifactRepositoryName).build());
        repositories.addAll(rootProject.getRemoteProjectRepositories());

        final String hubDetectVersion;
        final File jar;
        {
            final String[] gav = executableGav.split(":");
            if (!hubDetectCache.exists()) {
                hubDetectVersion = getHubDetectVersion(gav);

                hubDetectCache.getParentFile().mkdirs();

                try {
                    final ArtifactResult artifactResult = resolver.resolveArtifact(session.getRepositorySession(),
                            new ArtifactRequest(new DefaultArtifact(gav[0], gav[1], "jar", hubDetectVersion), repositories,
                                    null));
                    if (artifactResult.isMissing()) {
                        throw new IllegalStateException(String.format("Didn't find '%s'", executableGav));
                    }
                    jar = artifactResult.getArtifact().getFile();
                } catch (final ArtifactResolutionException e) {
                    throw new IllegalStateException(String.format("Didn't find '%s'", executableGav), e);
                }
                try {
                    FileUtils.copyFile(jar, hubDetectCache);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                hubDetectVersion = getHubDetectVersion(gav);
            }
        }

        final File explodedScanCli;
        if (scanCliOffline) {
            final String[] gav = scanCliGav.split(":");
            if (!scanCliCache.exists()) {
                boolean downloaded = false;
                File scanCliZip;
                if (forceScanCliDownload) {
                    downloaded = true;
                    scanCliZip = downloadScanCli(rootProject);
                } else {
                    try {
                        final ArtifactResult artifactResult = resolver.resolveArtifact(session.getRepositorySession(),
                                new ArtifactRequest(new DefaultArtifact(gav[0], gav[1], "zip", hubDetectVersion), emptyList(),
                                        null));
                        if (artifactResult.isMissing()) {
                            downloaded = true;
                            scanCliZip = downloadScanCli(rootProject);
                        } else {
                            scanCliZip = artifactResult.getArtifact().getFile();
                        }
                    } catch (final ArtifactResolutionException e) {
                        scanCliZip = downloadScanCli(rootProject);
                        downloaded = true;
                    }
                }
                if (downloaded) {
                    try {
                        final org.apache.maven.artifact.DefaultArtifact artifact = new org.apache.maven.artifact.DefaultArtifact(
                                gav[0], gav[1], hubDetectVersion, "compile", "zip", null, new DefaultArtifactHandler("zip"));
                        artifact.setFile(scanCliZip);
                        deployer.deploy(session.getProjectBuildingRequest(), session.getLocalRepository(),
                                singletonList(artifact));
                    } catch (final ArtifactDeployerException e) {
                        throw new MojoExecutionException(e.getMessage(), e);
                    }
                }
                try {
                    FileUtils.copyFile(scanCliZip, scanCliCache);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }

            explodedScanCli = new File(rootProject.getBuild().getDirectory(),
                    "blackduck/" + getClass().getSimpleName() + "_scancli");
            if (!explodedScanCli.exists()) {
                unzip(scanCliCache, explodedScanCli, true);
            }
        } else {
            explodedScanCli = null;
        }

        final String rootPath = rootProject.getBasedir().getAbsolutePath();
        final File java = new File(System.getProperty("java.home"), "bin/java");
        final List<String> command = new ArrayList<>();
        command.add(java.getAbsolutePath());
        if (systemVariables != null) {
            command.addAll(systemVariables.entrySet().stream()
                    .map(e -> String.format("-D%s=%s", e.getKey(), handlePlaceholders(rootPath, e.getValue())))
                    .collect(toList()));
        }
        final ProcessBuilder processBuilder = new ProcessBuilder().inheritIO().command(command);
        final Map<String, String> environment = processBuilder.environment();
        if (this.environment != null) {
            environment.putAll(this.environment);
        }
        final Map<String, String> config = new HashMap<>();
        // https://blackducksoftware.atlassian.net/wiki/spaces/INTDOCS/pages/68878339/Hub+Detect+Properties
        config.put("blackduck.url", blackduckUrl);
        config.put("blackduck.username", server.getUsername());
        config.put("blackduck.password", server.getPassword());
        config.put("logging.level.com.blackducksoftware.integration", logLevel);
        config.put("detect.project.name", blackduckName);
        config.put("detect.source.path", rootPath);
        config.put("detect.maven.scope", scope);
        if (scanCliOffline) {
            config.put("detect.hub.signature.scanner.offline.local.path", explodedScanCli.getAbsolutePath());
        }
        if (systemVariables == null || !systemVariables.containsKey("detect.output.path")) {
            config.put("detect.output.path", new File(rootProject.getBuild().getDirectory(), "blackduck").getAbsolutePath());
        }
        final String enforcedExcluded = "/blackduck/";
        if (exclusions != null) {
            config.put("detect.hub.signature.scanner.exclusion.patterns",
                    Stream.concat(Stream.of(enforcedExcluded), exclusions.stream().filter(Objects::nonNull).map(String::trim))
                            .collect(joining(",")));
        } else {
            config.put("detect.hub.signature.scanner.exclusion.patterns", "/blackduck/");
        }
        environment.put("SPRING_APPLICATION_JSON", new GsonBuilder().create().toJson(config));
        command.add("-jar");
        command.add(hubDetectCache.getAbsolutePath());
        getLog().info("Launching: " + processBuilder.command());

        final int exitStatus;
        try {
            exitStatus = processBuilder.start().waitFor();
        } catch (final InterruptedException e) {
            Thread.interrupted();
            getLog().error(e);
            throw new IllegalStateException(e);
        } catch (final IOException e) {
            getLog().error(e);
            throw new IllegalStateException(e);
        }

        getLog().info(String.format("Output: %d", exitStatus));

        int expectedExitCode;
        try {
            expectedExitCode = Integer.parseInt(validateExitCode);
        } catch (final NumberFormatException nfe) {
            if (Boolean.parseBoolean(validateExitCode)) {
                expectedExitCode = 0;
            } else {
                return;
            }
        }
        if (exitStatus != expectedExitCode) {
            throw new IllegalStateException(String.format("Invalid exit status: %d", exitStatus));
        }
    }

    private File downloadScanCli(final MavenProject rootProject) { // todo: use wagon to have progress
        getLog().info("Downloading scan.cli.zip, can take some time...\r");
        try {
            final URL url = new URL(scanCliDownloadUrl);
            final HttpURLConnection connection = HttpURLConnection.class.cast(url.openConnection());
            final File zip = new File(rootProject.getBuild().getDirectory(),
                    "blackduck/" + getClass().getSimpleName() + "/scan.cli.zip");
            zip.getParentFile().mkdirs();
            final int bufferSize = 819200;
            long downloaded = 0;
            final long start = System.nanoTime();
            final long length = connection.getContentLengthLong();
            try (final OutputStream os = new BufferedOutputStream(new FileOutputStream(zip), bufferSize)) {
                final byte[] buffer = new byte[bufferSize];
                int read;
                int percentage = -1;
                final InputStream inputStream = connection.getInputStream();
                while ((read = inputStream.read(buffer)) >= 0) {
                    downloaded += read;
                    if (read > 0) {
                        os.write(buffer, 0, read);
                        final float value = downloaded * 1f / length;
                        final int pcTracker = (int) (value * 100);
                        if (percentage != pcTracker) {
                            System.out.printf("Downloading scan.cli.zip - %2.2f%%\r", value);
                            System.out.flush();
                            percentage = pcTracker;
                        }
                    }
                }
            } finally {
                connection.disconnect();
            }
            final long end = System.nanoTime();
            getLog().info(String.format("Downloaded scan.cli.zip in %d seconds", TimeUnit.NANOSECONDS.toSeconds(end - start)));
            return zip;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getHubDetectVersion(final String[] gav) {
        String hubDetectVersion;
        if (!"latest".equalsIgnoreCase(gav[2])) {
            hubDetectVersion = gav[2];
        } else {
            try {
                final URL versionUrl = new URL(
                        String.format(latestVersionUrl, artifactoryBase, gav[0], gav[1], artifactRepositoryName));
                try (final InputStream stream = versionUrl.openStream()) {
                    hubDetectVersion = IOUtil.toString(stream);
                }
            } catch (final IOException e) {
                throw new IllegalArgumentException(e); // unlikely
            }
        }
        return hubDetectVersion;
    }

    private String handlePlaceholders(final String rootPath, final String value) {
        return value.replace("$rootProject", rootPath);
    }

    private void unzip(final File zipFile, final File destination, final boolean noparent) {
        getLog().info(String.format("Extracting '%s' to '%s'", zipFile.getAbsolutePath(), destination.getAbsolutePath()));
        try {
            final ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String path = entry.getName();
                if (noparent) {
                    path = path.replaceFirst("^[^/]+/", "");
                }
                final File file = new File(destination, path);

                if (entry.isDirectory()) {
                    file.mkdirs();
                    continue;
                }

                file.getParentFile().mkdirs();
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            in.close();
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to unzip " + zipFile.getAbsolutePath(), e);
        }
    }

}
