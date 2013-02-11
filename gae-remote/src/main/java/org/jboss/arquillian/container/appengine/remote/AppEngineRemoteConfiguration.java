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

package org.jboss.arquillian.container.appengine.remote;


import org.jboss.arquillian.container.common.AppEngineCommonConfiguration;

/**
 * AppEngine remote configuration.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineRemoteConfiguration extends AppEngineCommonConfiguration {
    private String email = System.getProperty(PREFIX + "email");
    private String password = System.getProperty(PREFIX + "password"); // TODO better?
    private String host;
    private String encoding;
    private String proxy;
    private boolean passIn = true;
    private boolean prompt;
    private boolean splitJars;
    private boolean keepTempUploadDir = Boolean.getBoolean(PREFIX + "keepTempUploadDir");
    private String serverURL = System.getProperty(PREFIX + "server.url");
    private long startupTimeout = 600; // 10min by default

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public boolean isPassIn() {
        return passIn;
    }

    public void setPassIn(boolean passIn) {
        this.passIn = passIn;
    }

    public boolean isPrompt() {
        return prompt;
    }

    public void setPrompt(boolean prompt) {
        this.prompt = prompt;
    }

    public boolean isSplitJars() {
        return splitJars;
    }

    public void setSplitJars(boolean splitJars) {
        this.splitJars = splitJars;
    }

    public boolean isKeepTempUploadDir() {
        return keepTempUploadDir;
    }

    public void setKeepTempUploadDir(boolean keepTempUploadDir) {
        this.keepTempUploadDir = keepTempUploadDir;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public long getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(long startupTimeout) {
        this.startupTimeout = startupTimeout;
    }
}
