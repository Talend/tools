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

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Intended to be used in a release profile, this will download the zip of the translations.
 * Then through Java resource bundle SPI it can be injected without having modified the sources.
 */
@Mojo(name = "xtm-download", defaultPhase = PACKAGE, requiresOnline = true)
public class XtmDownloadMojo extends XtmBaseMojo {

    /**
     * Where to output the downloaded file.
     */
    @Parameter(defaultValue = "${project.build.directory}/xtm.zip", property = "xtm.download.output")
    private File output;

    @Override
    protected void doExecute(final WebTarget target, final String token) throws MojoExecutionException {
        final long projectId = findProjectId(target, token);
        try (final InputStream stream = target.path("projects/{projectId}/files/download").resolveTemplate("projectId", projectId)
                .queryParam("fileType", "TARGET").request(APPLICATION_OCTET_STREAM_TYPE).header(HttpHeaders.AUTHORIZATION, token)
                .get(InputStream.class)) {
            Files.copy(stream, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
