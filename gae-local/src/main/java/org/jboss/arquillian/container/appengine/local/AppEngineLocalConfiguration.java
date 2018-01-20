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

package org.jboss.arquillian.container.appengine.local;


import org.jboss.arquillian.container.common.AppEngineCommonConfiguration;

/**
 * AppEngine CLI configuration.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineLocalConfiguration extends AppEngineCommonConfiguration {
    private String server;
    private String address = "localhost";
    private int port = 8080;
    private boolean disableUpdateCheck;
    private boolean startOnFirstThread = true;
    private String javaAgent;
    private String jvmFlags; //
    private String jvmFlagsSeparator = "\\|";
    private String serverTestURL;
    private long startupTimeout = 30; // 30sec

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isDisableUpdateCheck() {
        return disableUpdateCheck;
    }

    public void setDisableUpdateCheck(boolean disableUpdateCheck) {
        this.disableUpdateCheck = disableUpdateCheck;
    }

    public boolean isStartOnFirstThread() {
        return startOnFirstThread;
    }

    public void setStartOnFirstThread(boolean startOnFirstThread) {
        this.startOnFirstThread = startOnFirstThread;
    }

    public String getJavaAgent() {
        return javaAgent;
    }

    public void setJavaAgent(String javaAgent) {
        this.javaAgent = javaAgent;
    }

    public String getJvmFlags() {
        return jvmFlags;
    }

    public void setJvmFlags(String jvmFlags) {
        this.jvmFlags = jvmFlags;
    }

    public String getJvmFlagsSeparator() {
        return jvmFlagsSeparator;
    }

    public void setJvmFlagsSeparator(String jvmFlagsSeparator) {
        this.jvmFlagsSeparator = jvmFlagsSeparator;
    }

    public String getServerTestURL() {
        return serverTestURL;
    }

    public void setServerTestURL(String serverTestURL) {
        this.serverTestURL = serverTestURL;
    }

    public long getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(long startupTimeout) {
        this.startupTimeout = startupTimeout;
    }
}
