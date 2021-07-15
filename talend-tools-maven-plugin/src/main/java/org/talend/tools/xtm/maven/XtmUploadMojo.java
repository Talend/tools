/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.tools.xtm.maven;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import javax.activation.FileDataSource;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "xtm-upload", defaultPhase = PACKAGE, inheritByDefault = false, requiresOnline = true)
public class XtmUploadMojo extends XtmBaseMojo {

    @Override
    protected void doExecute(final WebTarget target, final String token) throws MojoExecutionException {
        final Collection<File> files = collectTranslatedFiles();
        final File zip;
        try {
            zip = zipFiles(files);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        final long projectId = findProjectId(target, token);
        final MultipartBody form = new MultipartBody(
                asList(new Attachment("files[0].file", new FileDataSource(zip), new MultivaluedHashMap<>()),
                        new Attachment("files[0].name", projectId), new Attachment("matchType", "MATCH_NAMES")));
        final Response response = target.path("projects/{projectId}/files/upload").resolveTemplate("projectId", projectId)
                .request(APPLICATION_JSON_TYPE).header(HttpHeaders.AUTHORIZATION, token)
                .post(entity(form, MULTIPART_FORM_DATA_TYPE));
        if (!zip.delete()) {
            zip.deleteOnExit();
        }
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new MojoExecutionException("Bad returned status: " + response.getStatus());
        }
        final Log log = getLog();
        log.info("Upload successful of " + files.size() + " files");
        if (log.isDebugEnabled()) {
            log.debug(response.readEntity(String.class));
        }
    }

    private File zipFiles(final Collection<File> files) throws IOException {
        final File tempXtmFile = new File(project.getBuild().getDirectory(), "xtm-temp_" + UUID.randomUUID().toString() + ".zip");
        final Collection<String> folderCreated = new HashSet<>();
        try (final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempXtmFile))) {
            files.forEach(file -> {
                try {
                    writeFile(folderCreated, zos, file);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        return tempXtmFile;
    }

    private void writeFile(final Collection<String> folderCreated, final ZipOutputStream zos, final File file)
            throws IOException {
        final String path = project.getBasedir().getAbsoluteFile().toPath().relativize(file.getAbsoluteFile().toPath()).toString()
                .replace(File.separatorChar, '/');
        final String[] segments = path.split("/");
        for (int i = 1; i <= segments.length; i++) {
            final String folder = Stream.of(segments).limit(i).collect(joining("/"));
            if (folderCreated.add(folder)) {
                zos.putNextEntry(new JarEntry(folder + '/'));
                zos.closeEntry();
            }
        }

        zos.putNextEntry(new JarEntry(path));
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
    }
}
