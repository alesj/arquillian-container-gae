/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.tools.admin.AppAdmin;
import com.google.appengine.tools.admin.AppAdminFactory;
import com.google.appengine.tools.admin.Application;
import com.google.appengine.tools.admin.UpdateFailureEvent;
import com.google.appengine.tools.admin.UpdateListener;
import com.google.appengine.tools.admin.UpdateProgressEvent;
import com.google.appengine.tools.admin.UpdateSuccessEvent;
import com.google.appengine.tools.info.SdkInfo;
import com.google.appengine.tools.info.UpdateCheck;
import com.google.apphosting.utils.config.AppEngineConfigException;
import org.jboss.arquillian.container.common.AppEngineCommonContainer;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.xml.sax.SAXParseException;

/**
 * Tools AppEngine container.
 * <p/>
 * Code taken from "http://code.google.com/p/google-plugin-for-eclipse/source/browse/trunk/plugins/com.google.appengine.eclipse.core/proxy_src/com/google/appengine/eclipse/core/proxy/AppEngineBridgeImpl.java?r=2"
 * with permission from GAE team.
 *
 * @author GAE Team
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineToolsContainer extends AppEngineCommonContainer<AppEngineToolsConfiguration> {
    private AppEngineToolsConfiguration configuration;

    public Class<AppEngineToolsConfiguration> getConfigurationClass() {
        return AppEngineToolsConfiguration.class;
    }

    public void setup(AppEngineToolsConfiguration configuration) {
        this.configuration = configuration;

        final String sdkDir = configuration.getSdkDir();
        if (sdkDir == null)
            throw new ConfigurationException("AppEngine SDK root is null.");

        SdkInfo.setSdkRoot(new File(sdkDir));
    }

    protected ProtocolMetaData doDeploy(Archive<?> archive) throws DeploymentException {
        try {
            UpdateCheck updateCheck = new UpdateCheck(SdkInfo.getDefaultServer());
            if (updateCheck.allowedToCheckForUpdates()) {
                updateCheck.maybePrintNagScreen(new PrintStream(System.out, true));
            }

            final AppAdmin appAdmin;
            try {
                appAdmin = createAppAdmin();
            } catch (AppEngineConfigException e) {
                if (e.getCause() instanceof SAXParseException) {
                    String msg = e.getCause().getMessage();

                    // have to check what the message says to distinguish a file-not-found
                    // problem from some other xml problem.
                    if (msg.contains("Failed to read schema document") && msg.contains("backends.xsd")) {
                        throw new IllegalArgumentException("Deploying a project with backends requires App Engine SDK 1.5.0 or greater.", e);
                    } else {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }

            final DeployUpdateListener listener = new DeployUpdateListener(
                    new PrintWriter(System.out, true),
                    new PrintWriter(System.err, true)
            );

            getExecutor().execute(new Runnable() {
                public void run() {
                    appAdmin.update(listener);
                }
            });

            Status status = null;
            while (status == null) {
                Thread.sleep(configuration.getStartupTimeout());
                status = listener.getStatus();
            }

            if (status != Status.OK) {
                throw new DeploymentException("Cannot deploy via GAE tools: " + status);
            }

            String appId = parseAppId(archive);

            return getProtocolMetaData("http://" + appId + ".appspot.com", configuration.getPort(), archive);
        } catch (DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException("Cannot deploy via GAE tools.", e);
        }
    }

    protected Executor getExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    AppAdmin createAppAdmin() throws IOException {
        AppAdminFactory appAdminFactory = new AppAdminFactory();

        /**
         if (options.getJavaExecutableOSPath() != null) {
         appAdminFactory.setJavaExecutable(new File(options.getJavaExecutableOSPath()));
         }

         if (options.getJavaCompilerExecutableOSPath() != null) {
         appAdminFactory.setJavaCompiler(new File(options.getJavaCompilerExecutableOSPath()));
         }
         */

        AppAdminFactory.ConnectOptions appEngineConnectOptions = new AppAdminFactory.ConnectOptions();
        if (configuration.getOAuthToken2() != null) {
            appEngineConnectOptions.setOauthToken(configuration.getOAuthToken2());
        }
        String appengineServer = System.getenv("APPENGINE_SERVER");
        if (appengineServer != null) {
            appEngineConnectOptions.setServer(appengineServer);
        }

        Application app = Application.readApplication(getAppLocation().getCanonicalPath());
        PrintWriter errorWriter = new PrintWriter(System.err, true);

        return appAdminFactory.createAppAdmin(appEngineConnectOptions, app, errorWriter);
    }

    private static final class DeployUpdateListener implements UpdateListener {
        private static final Logger log = Logger.getLogger(DeployUpdateListener.class.getName());

        /**
         * Class for getting headers representing different stages of deployments
         * bassed on console messages from the gae sdk.
         */
        private static class MessageHeaders {

            // Headers should go in the order specified in this array.
            private static final PrefixHeaderPair[] prefixHeaderPairs = new PrefixHeaderPair[]{
                    new PrefixHeaderPair("Preparing to deploy", null, "Created staging directory", "Scanning files on local disk"),
                    new PrefixHeaderPair("Deploying", null, "Uploading"),
                    new PrefixHeaderPair("Verifying availability", "Verifying availability of", "Will check again in 1 seconds."),
                    new PrefixHeaderPair("Updating datastore", null, "Uploading index")};

            /*
             * The headers should go in the sequence specified in the array,
             * so keep track of which header we're currently looking for.
             */
            private int currentPrefixHeaderPair;

            PrefixHeaderPair getMessageHeader(String msg) {
                PrefixHeaderPair php = prefixHeaderPairs[currentPrefixHeaderPair];
                for (String prefix : php.msgPrefixes) {
                    if (msg.startsWith(prefix)) {
                        currentPrefixHeaderPair = (currentPrefixHeaderPair + 1) % prefixHeaderPairs.length;
                        return php;
                    }
                }
                return null;
            }
        }

        /**
         * Class for holding the different gae sdk messages that are associated with
         * different "headers", representing the stages of deployment.
         */
        private static class PrefixHeaderPair {
            // the header that should be displayed on the console
            final String header;

            // the prefixes of console messages that trigger this header
            final String[] msgPrefixes;

            // the header that should be displayed on the progress dialog, mainly for
            // displaying "verifying availability" on the console and displaying
            // "verifying availability of "backend""
            final String taskHeader;

            PrefixHeaderPair(String header, String taskHeader, String... msgPrefixes) {
                this.msgPrefixes = msgPrefixes;
                this.header = header;
                if (taskHeader == null) {
                    this.taskHeader = header;
                } else {
                    this.taskHeader = taskHeader;
                }
            }
        }

        /**
         * Attempts to reflectively call getDetails() on the event object received
         * by the onFailure or onSuccess callback. That method is only supported by
         * App Engine SDK 1.2.1 or later. If we are able to call getDetails we
         * return the details message; otherwise we return <code>null</code>.
         */
        private static String getDetailsIfSupported(Object updateEvent) {
            try {
                Method method = updateEvent.getClass().getDeclaredMethod("getDetails");
                return (String) method.invoke(updateEvent);
            } catch (NoSuchMethodException e) {
                // Expected on App Engine SDK 1.2.0; no need to log
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            return null;
        }

        /**
         * Reflectively checks to see if an exception is a JspCompilationException,
         * which is only supported by App Engine SDK 1.2.1 or later.
         */
        private static boolean isJspCompilationException(Throwable ex) {
            if (ex != null) {
                try {
                    Class<?> jspCompilationExceptionClass = Class.forName("com.google.appengine.tools.admin.JspCompilationException");
                    return jspCompilationExceptionClass.isAssignableFrom(ex.getClass());
                } catch (ClassNotFoundException e) {
                    // Expected on App Engine SDK 1.2.0; no need to log
                }
            }
            return false;
        }

        private final PrintWriter errorWriter;
        private MessageHeaders messageHeaders;
        private final PrintWriter outputWriter;

        private int percentDone = 0;

        private Status status = null;

        private DeployUpdateListener(PrintWriter outputWriter, PrintWriter errorWriter) {
            this.outputWriter = outputWriter;
            this.errorWriter = errorWriter;
            messageHeaders = new MessageHeaders();
        }

        public Status getStatus() {
            return status;
        }

        public void onFailure(UpdateFailureEvent event) {
            // Create status object and print error message to the writer
            status = Status.ERROR;
            outputWriter.println(event.getFailureMessage());

            // Only print the details for JSP compilation errors
            if (isJspCompilationException(event.getCause())) {
                String details = getDetailsIfSupported(event);
                if (details != null) {
                    outputWriter.println(details);
                }
            }
        }

        public void onProgress(UpdateProgressEvent event) {
            // Update the progress monitor
            int worked = event.getPercentageComplete() - percentDone;
            percentDone += worked;

            String msg = event.getMessage();
            PrefixHeaderPair php = messageHeaders.getMessageHeader(msg);

            if (php != null) {
                outputWriter.println("\n" + php.header + ":");
            }
            outputWriter.println("\t" + msg);
        }

        public void onSuccess(UpdateSuccessEvent event) {
            status = Status.OK;

            percentDone = 0; // reset

            String details = getDetailsIfSupported(event);
            if (details != null) {
                // Note that unlike in onFailure, we're writing to the log file here,
                // not to the console. This is so we don't clutter our deployment
                // console with a bunch of info or warning messages from JSP
                // compilation, if we deployed successfully.
                errorWriter.println(details);
            }

            outputWriter.println("\nDeployment completed successfully");
        }

        /**
         * Println's the given string to this DeployUpdateListener's output writer.
         */
        public void println(String s) {
            outputWriter.println(s);
        }
    }

    private static final String APPLICATION = "<application>";

    protected String parseAppId(Archive<?> archive) throws Exception {
        Node node = archive.get("WEB-INF/appengine-web.xml");
        if (node == null) {
            throw new IllegalArgumentException("No appengine-web.xml file exists!");
        }

        Set<String> tokens = new HashSet<String>(Collections.singleton(APPLICATION));
        Map<String, String> map = parseTokens(node.getAsset().openStream(), tokens);
        return map.get(APPLICATION);
    }

    private Map<String, String> parseTokens(InputStream is, final Set<String> tokens) throws Exception {
        final Map<String, String> results = new HashMap<String, String>();
        try {
            StringBuilder builder = new StringBuilder();
            int x;
            String token = null;
            StringBuilder tokenBuilder = new StringBuilder();
            while ((x = is.read()) != -1) {
                char ch = (char) x;
                if (token != null) {
                    if (ch == '<') {
                        results.put(token, tokenBuilder.toString());
                        if (tokens.isEmpty()) {
                            break;
                        } else {
                            token = null;
                            tokenBuilder.setLength(0); // reset builder
                        }
                    } else {
                        tokenBuilder.append(ch);
                    }
                } else {
                    builder.append(ch);
                }
                // check if we hit any token
                if (token == null) {
                    for (String t : tokens) {
                        if (builder.toString().endsWith(t)) {
                            token = t;
                            break;
                        }
                    }
                    if (token != null) {
                        tokens.remove(token);
                    }
                }
            }
            return results;
        } finally {
            safeClose(is);
        }
    }
}