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

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.shared.utils.io.DirectoryScanner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class XtmBaseMojo extends AbstractMojo {

    private static final String[] EMPTY_ARRAY = new String[0];

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter
    protected String projectId;

    @Parameter(defaultValue = "https://www.xtm-cloud.com/rest-api", property = "xtm.baseUrl")
    protected String baseUrl;

    @Parameter(defaultValue = "xtm", property = "xtm.serverId")
    protected String serverId;

    @Parameter(property = "xtm.userId")
    protected long userId;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "**/main/resources/**/*.properties", property = "xtm.includes")
    protected Collection<String> includes;

    @Parameter(defaultValue = "**/log4j.properties,**/_en.properties,**/_fr.properties,**/_ja.properties", property = "xtm.excludes")
    protected Collection<String> excludes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Client client = ClientBuilder.newClient().register(new JsonbJaxrsProvider<>());
        try {
            final WebTarget target = client.target(baseUrl);
            final Optional<String> token = getToken(target);
            if (!token.isPresent()) {
                getLog().warn("No xtm token, skipping task");
                return;
            }

            doExecute(target, "XTM-Basic " + token.get());
        } finally {
            client.close();
        }
    }

    protected abstract void doExecute(WebTarget target, String token) throws MojoExecutionException;

    protected Collection<File> collectTranslatedFiles() {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(project.getBasedir());
        if (includes != null) {
            scanner.setIncludes(includes.toArray(EMPTY_ARRAY));
        }
        if (excludes != null) {
            scanner.setExcludes(excludes.toArray(EMPTY_ARRAY));
        }
        scanner.addDefaultExcludes();
        scanner.scan();
        return Stream.of(scanner.getIncludedFiles()).map(f -> new File(scanner.getBasedir(), f)).collect(toList());
    }

    protected long findProjectId(final WebTarget client, final String token) {
        return findProject(client.path("projects"), token, 100, 0).map(Project::getId)
                .orElseThrow(() -> new IllegalArgumentException("No project found with name: " + xtmProjectId()));
    }

    private Optional<Project> findProject(final WebTarget target, final String token, final int pageSize, final int currentPage) {
        final Response response = getProjectsPage(target, token, currentPage, pageSize);
        final List<Project> projects = response.readEntity(new GenericType<List<Project>>() {
        });
        final String name = xtmProjectId();
        return projects.stream().filter(p -> name.equals(p.getName()) && "ACTIVE".equalsIgnoreCase(p.getActivity())).findFirst()
                .map(Optional::of).orElseGet(() -> {
                    if (ofNullable(response.getHeaderString("xtm-total-items-count")).map(Integer::valueOf)
                            .filter(i -> i > pageSize).isPresent()) {
                        return findProject(target, token, pageSize, currentPage + 1);
                    }
                    return empty();
                });
    }

    private Response getProjectsPage(final WebTarget target, final String token, final int page, final int pageSize) {
        return target.queryParam("status", "STARTED").queryParam("page", page).queryParam("pageSize", pageSize)
                .request(APPLICATION_JSON_TYPE).header(HttpHeaders.AUTHORIZATION, token).get();
    }

    protected String xtmProjectId() {
        return ofNullable(projectId).orElseGet(() -> {
            final String[] version = project.getVersion().split("\\.");
            return String.format("%s-%s.x", project.getArtifactId(), version[0]);
        });
    }

    protected Optional<String> getToken(final WebTarget target) {
        final Optional<Server> serverOpt = findServer(serverId);
        if (!serverOpt.isPresent()) {
            getLog().warn(String.format("No server '%s', skipping %s execution", serverId, getClass().getSimpleName()));
            return Optional.empty();
        }
        final Server server = serverOpt.get();
        return ofNullable(target.path("auth/token").request(APPLICATION_JSON_TYPE).cacheControl(CacheControl.valueOf("no-cache"))
                .post(entity(new TokenRequest(server.getUsername(), server.getPassword(), userId), APPLICATION_JSON_TYPE),
                        TokenResponse.class)
                .getToken());
    }

    private Optional<Server> findServer(final String serverId) {
        return session.getSettings().getServers().stream().filter(s -> serverId.equals(s.getId())).findFirst();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenRequest {

        private String client;

        private String password;

        private long userId;
    }

    @Data
    public static class TokenResponse {

        private String token;
    }

    @Data
    public static class Project {

        private long id;

        private String name;

        private String activity;

        private String status;
    }
}
