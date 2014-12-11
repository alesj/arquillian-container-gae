/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.arquillian.container.appengine.gcloud;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.common.AppEngineCommonContainer;
import org.jboss.arquillian.container.common.URLChecker;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Container that uses gcloud tool to deploy tests.
 * Atm we only support .war deployments.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineGCloudContainer extends AppEngineCommonContainer<AppEngineGCloudConfiguration> {
    private AppEngineGCloudConfiguration configuration;
    private Process process;

    public Class<AppEngineGCloudConfiguration> getConfigurationClass() {
        return AppEngineGCloudConfiguration.class;
    }

    public void setup(AppEngineGCloudConfiguration configuration) {
        log.info("Docker host: " + configuration.getHost()); // test host
        this.configuration = configuration;
    }

    protected File export(Archive<?> archive) throws Exception {
        if (archive instanceof WebArchive == false) {
            throw new IllegalArgumentException("Can only handle .war deployments: " + archive);
        }

        Node dockerFile = archive.get("Dockerfile");
        if (dockerFile == null) {
            if (configuration.getFrom() == null) {
                throw new IllegalArgumentException("Missing Docker's FROM value!");
            }

            log.info("Using Docker FROM: " + configuration.getFrom());

            String content = "FROM " + configuration.getFrom() + "\n" + "ADD . /app" + "\n";
            archive.add(new StringAsset(content), "Dockerfile");
        }

        return super.export(archive);
    }

    protected ProtocolMetaData doDeploy(Archive<?> archive) throws DeploymentException {
        List<String> command = new ArrayList<>();
        command.add("gcloud");
        if (configuration.isDebug()) {
            command.add("--verbosity");
            command.add("debug");
        }
        command.add("preview");
        command.add("app");
        command.add("run");
        command.add(getAppLocation().getPath());

        log.info("GCloud command: " + command);

        try {
            DockerContainer.removeAll();
        } catch (Exception e) {
            throw new DeploymentException("Cannot remove all previous Docker containers.", e);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IllegalStateException("Error running gcloud!", e);
        }

        Thread streamThread = new Thread(new Runnable() {
            public void run() {
                try {
                    try (InputStream ps = process.getInputStream()) {
                        int x;
                        while ((x = ps.read()) != -1) {
                            System.out.write(x);
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        });
        streamThread.start();

        String host = configuration.getHost();
        int port = readPort();

        String serverUrl = "http://" + host + ":" + port + "/_ah/health";
        try {
            delayArchiveDeploy(serverUrl, configuration.getStartupTimeout(), 3000L, new GCloudURLChecker());
        } catch (Exception e) {
            throw new DeploymentException("Error delaying archive deployment.", e);
        }

        return getProtocolMetaData(host, port, DEFAULT);
    }

    protected int readPort() throws DeploymentException {
        int retry = 10;
        Exception e = null;
        while (retry > 0) {
            try {
                DockerContainer utils = DockerContainer.getLast();
                return utils.getPort();
            } catch (Exception ex) {
                e = ex;
            }
            try {
                Thread.sleep(1000); // wait 1sec
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ie);
            }
            retry--;
        }
        throw new DeploymentException("Cannot read port.", e);
    }

    @Override
    protected void shutdownServer() {
        if (process != null) {
            try {
                process.destroy();
            } finally {
                try {
                    DockerContainer.removeAll();
                } catch (Exception e) {
                    log.warning("Could not cleanup Docker containers.");
                }
            }
        }
    }

    private static class GCloudURLChecker implements URLChecker {
        public boolean check(URL url) {
            try {
                String result = "";
                try (InputStream is = url.openStream()) {
                    int x;
                    while ((x = is.read()) != -1) {
                        result += (char) (x & 0xFF);
                    }
                }
                return "ok".equalsIgnoreCase(result);
            } catch (IOException e) {
                return false;
            }
        }
    }
}