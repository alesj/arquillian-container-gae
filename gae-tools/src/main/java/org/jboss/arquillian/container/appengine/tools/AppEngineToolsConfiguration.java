/*
* JBoss, Home of Professional Open Source
* Copyright $today.year Red Hat Inc. and/or its affiliates and other
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

package org.jboss.arquillian.container.appengine.tools;


import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/**
 * AppEngine CLI configuration.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineToolsConfiguration implements ContainerConfiguration {
    public static final String SDK_ROOT = "appengine.sdk.root";

    private String sdkDir = System.getProperty(SDK_ROOT);
    private int port = 80;
    private long startupTimeout = 3000L; // 3sec
    private String oAuthToken2;

    public void validate() throws ConfigurationException {
    }

    public void setSdkDir(String sdkDir) {
        this.sdkDir = sdkDir;
    }

    public String getSdkDir() {
        return sdkDir;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(long startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    public String getOAuthToken2() {
        return oAuthToken2;
    }

    public void setOAuthToken2(String oauthToken2) {
        this.oAuthToken2 = oauthToken2;
    }
}
