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

package org.jboss.arquillian.container.appscale.remote;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.container.spi.client.deployment.Validate;

/**
 * AppScale configuration.
 *
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
public class AppScaleRemoteConfiguration implements ContainerConfiguration {
    private static final String APPSCALE = "appscale.";

    private long uploadTimeout = 30 * 1000L;
    private long removeTimeout = 15 * 1000L;
    private long deployTimeout = 150 * 1000L;
    private long undeployTimeout = 75 * 1000L;

    /**
     * The e-mail address to use as the app's admin.
     * Use AppScale's admin email, otherwise tests block on password prompt.
     */
    private String email = System.getProperty(APPSCALE + "email");

    /**
     * Host runing AppScale.
     */
    private String host = System.getProperty(APPSCALE + "host");

    @Override
    public void validate() throws ConfigurationException {
        Validate.notNullOrEmpty(email, "The e-mail address to use as the app's admin must be specified.");
        Validate.notNullOrEmpty(host, "Host running AppScale must be specified.");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public long getUploadTimeout() {
        return uploadTimeout;
    }

    public void setUploadTimeout(long uploadTimeout) {
        this.uploadTimeout = uploadTimeout;
    }

    public long getRemoveTimeout() {
        return removeTimeout;
    }

    public void setRemoveTimeout(long removeTimeout) {
        this.removeTimeout = removeTimeout;
    }

    public long getDeployTimeout() {
        return deployTimeout;
    }

    public void setDeployTimeout(long deployTimeout) {
        this.deployTimeout = deployTimeout;
    }

    public long getUndeployTimeout() {
        return undeployTimeout;
    }

    public void setUndeployTimeout(long undeployTimeout) {
        this.undeployTimeout = undeployTimeout;
    }
}
