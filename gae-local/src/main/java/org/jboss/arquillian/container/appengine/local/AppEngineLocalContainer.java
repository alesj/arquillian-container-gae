/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jboss.arquillian.container.appengine.cli.AppEngineCLIContainer;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Local / development AppEngine container.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineLocalContainer extends AppEngineCLIContainer<AppEngineLocalConfiguration> {
    private static final String JVM_FLAG = "jvm_flag";
    private static final String APPENGINE_TEST = "appengine.test.";

    private AppEngineLocalConfiguration configuration;

    public Class<AppEngineLocalConfiguration> getConfigurationClass() {
        return AppEngineLocalConfiguration.class;
    }

    public void setup(AppEngineLocalConfiguration configuration) {
        this.configuration = configuration;

        if (configuration.getSdkDir() == null)
            throw new ConfigurationException("AppEngine SDK root is null.");

        System.setProperty(AppEngineLocalConfiguration.SDK_ROOT, configuration.getSdkDir());
    }

    protected ProtocolMetaData doDeploy(Archive<?> archive) throws DeploymentException {
        final String classpath = System.getProperty("java.class.path");
        try {
            List<String> args = new ArrayList<String>();
            args.add("com.google.appengine.tools.development.DevAppServerMain"); // dev app server

            String sdkDir = (String) addArg(args, "sdk_root", configuration.getSdkDir(), false);
            if (new File(sdkDir).isDirectory() == false)
                throw new DeploymentException("SDK root is not a directory: " + sdkDir);

            String toolsJar = sdkDir + "/lib/appengine-tools-api.jar";
            if (new File(toolsJar).exists() == false)
                throw new DeploymentException("No appengine-tools-api.jar: " + toolsJar);

            System.setProperty("java.class.path", toolsJar);

            addArg(args, "server", configuration.getServer(), true);

            addArg(args, "address", configuration.getAddress(), false);
            addArg(args, "port", configuration.getPort(), false);
            addArg(args, "startOnFirstThread", configuration.isDisableUpdateCheck(), false);
            addArg(args, "disable_update_check", configuration.isStartOnFirstThread());
            boolean isJavaAgentSet = (configuration.getJavaAgent() != null);
            if (isJavaAgentSet) {
                jvm_flag(args, "-noverify");
                jvm_flag(args, "-javaagent:" + configuration.getJavaAgent());
            }

            // add any system properties starting with appengine.test.
            Properties properties = System.getProperties();
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith(APPENGINE_TEST)) {
                    jvm_flag(args, "-D" + key.substring(APPENGINE_TEST.length()) + "=" + properties.getProperty(key));
                }
            }

            if (configuration.getJvmFlags() != null) {
                String[] jvmFlags = configuration.getJvmFlags().split(configuration.getJvmFlagsSeparator());
                for (String jvmFlag : jvmFlags) {
                    jvm_flag(args, jvmFlag);
                }
            }

            args.add(getAppLocation().getCanonicalPath());

            invokeAppEngine(sdkDir, "com.google.appengine.tools.KickStart", args.toArray(new String[args.size()]));

            String serverURL = configuration.getServerTestURL();
            if (serverURL == null)
                serverURL = "http://localhost:" + configuration.getPort() + "/_ah/admin";

            delayArchiveDeploy(serverURL, configuration.getStartupTimeout(), 1000L);

            return getProtocolMetaData(configuration.getAddress(), configuration.getPort(), archive);
        } catch (Exception e) {
            throw new DeploymentException("Cannot deploy to local GAE.", e);
        } finally {
            System.setProperty("java.class.path", classpath);
        }
    }

    protected static void jvm_flag(List<String> args, String value) {
        addArg(args, JVM_FLAG, value, false);
    }
}
