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


import org.jboss.arquillian.container.common.AppEngineCommonConfiguration;

/**
 * AppEngine GCloud configuration.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineGCloudConfiguration extends AppEngineCommonConfiguration {
    private String from = System.getProperty("docker.from");
    private boolean debug = Boolean.getBoolean("gcloud.debug");
    private String host;
    private long startupTimeout = 30; // 30sec

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getHost() {
        if (host == null) {
            String dockerHost = System.getenv("DOCKER_HOST");
            if (dockerHost == null) {
                throw new IllegalArgumentException("Missing host or DOCKER_HOST!");
            }
            int p = dockerHost.indexOf("//"); // after schema
            int r = dockerHost.lastIndexOf(':'); // before port
            host = dockerHost.substring(p + 2, r);
        }
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public long getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(long startupTimeout) {
        this.startupTimeout = startupTimeout;
    }
}
