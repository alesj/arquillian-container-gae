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

package org.jboss.arquillian.container.common;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.protocol.modules.ModuleMetaData;
import org.jboss.arquillian.protocol.modules.ModulesServletProtocol;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.application5.ApplicationDescriptor;
import org.jboss.shrinkwrap.descriptor.api.application5.ModuleType;

/**
 * Common GAE Arquillian container.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AppEngineCommonContainer<T extends ContainerConfiguration> implements DeployableContainer<T> {
    protected static final String DEFAULT = "default";
    protected final Logger log = Logger.getLogger(getClass().getName());

    private File appLocation;

    protected File getAppLocation() {
        return appLocation;
    }

    public void start() throws LifecycleException {
    }

    public void stop() throws LifecycleException {
    }

    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription(ModulesServletProtocol.PROTOCOL_NAME);
    }

    protected void prepareArchive(Archive<?> archive) {
    }

    protected void delayArchiveDeploy(String serverURL, long startupTimeout, long checkPeriod) throws Exception {
        delayArchiveDeploy(serverURL, startupTimeout, checkPeriod, new URLChecker() {
            public boolean check(URL stream) {
                try {
                    //noinspection EmptyTryBlock,UnusedDeclaration
                    try (InputStream is = stream.openStream()) {}
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        });
    }

    protected void delayArchiveDeploy(String serverURL, long startupTimeout, long checkPeriod, URLChecker checker) throws Exception {
        if (serverURL == null)
            throw new IllegalArgumentException("Null server url");

        final URL server = new URL(serverURL);
        log.info("Pinging server url: " + serverURL);

        long timeout = startupTimeout * 1000;
        while (timeout > 0) {
            Thread.sleep(checkPeriod);

            if (checker.check(server)) {
                break;
            }

            timeout -= checkPeriod;
        }
        if (timeout <= 0)
            throw new IllegalStateException("Cannot connect to managed AppEngine, timed out.");
    }

    protected abstract ProtocolMetaData doDeploy(Archive<?> archive) throws DeploymentException;

    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        prepareArchive(archive);

        try {
            appLocation = export(archive);
        } catch (Exception e) {
            throw new DeploymentException("Cannot export archive " + archive.getName() + ".", e);
        }

        return doDeploy(archive);
    }

    protected File getTempRoot() {
        return AccessController.doPrivileged(new PrivilegedAction<File>() {
            public File run() {
                File root = new File(System.getProperty("java.io.tmpdir"));
                log.info(String.format("Get temp root: %s", root));
                return root;
            }
        });
    }

    protected File export(Archive<?> archive) throws Exception {
        File root = getTempRoot();
        return export(archive, root);
    }

    protected File export(Archive<?> archive, final File root) throws Exception {
        FixedExplodedExporter exporter = new FixedExplodedExporter(archive, root);
        return exporter.export();
    }

    public void undeploy(Archive<?> archive) throws DeploymentException {
        teardown();
        shutdownServer();
        deleteAppLocation();
    }

    public void deploy(Descriptor descriptor) throws DeploymentException {
    }

    public void undeploy(Descriptor descriptor) throws DeploymentException {
    }

    protected ProtocolMetaData getProtocolMetaData(String host, int port, Archive<?> archive) {
        return getProtocolMetaData(host, port, extractModules(host, port, archive));
    }

    protected ProtocolMetaData getProtocolMetaData(String host, int port) {
        return getProtocolMetaData(host, port, new String[0]); // empty modules
    }

    protected ProtocolMetaData getProtocolMetaData(String host, int port, String... modules) {
        List<ModuleMetaData> list = new ArrayList<>();
        for (String module : modules) {
            ModuleMetaData mmd = new ModuleMetaData(module, host, port);
            list.add(mmd);
        }
        return getProtocolMetaData(host, port, list);
    }

    protected ProtocolMetaData getProtocolMetaData(String host, int port, List<ModuleMetaData> modules) {
        final ProtocolMetaData protocolMetaData = new ProtocolMetaData();
        // (@ArquillianResource URL url) support
        protocolMetaData.addContext(new HTTPContext(host, port));
        // modules
        for (ModuleMetaData mmd : modules) {
            protocolMetaData.addContext(mmd);
        }
        return protocolMetaData;
    }

    protected static List<ModuleMetaData> extractModules(String host, int port, Archive<?> archive) {
        final List<ModuleMetaData> list = new ArrayList<>();
        if (archive instanceof EnterpriseArchive) {
            final EnterpriseArchive ear = (EnterpriseArchive) archive;
            final Node appXml = archive.get(ParseUtils.APPLICATION_XML);
            if (appXml != null) {
                InputStream stream = appXml.getAsset().openStream();
                try {
                    ApplicationDescriptor ad = Descriptors.importAs(ApplicationDescriptor.class).fromStream(stream);
                    List<ModuleType<ApplicationDescriptor>> allModules = ad.getAllModule();
                    for (ModuleType<ApplicationDescriptor> mt : allModules) {
                        String uri = mt.getOrCreateWeb().getWebUri();
                        if (uri != null) {
                            WebArchive war = ear.getAsType(WebArchive.class, uri);
                            handleWar(host, port, war, list);
                        } else {
                            mt.removeWeb();
                        }
                    }
                } finally {
                    safeClose(stream);
                }
            }
        } else if (archive instanceof WebArchive) {
            final WebArchive war = (WebArchive) archive;
            handleWar(host, port, war, list);
        }
        return list;
    }

    protected static void handleWar(String host, int port, WebArchive war, List<ModuleMetaData> list) {
        String module;
        try {
            module = parseModuleRaw(war);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        if (module == null) {
            if (list.isEmpty()) {
                module = DEFAULT; // first one is default
            } else {
                throw new IllegalStateException("Missing module info in appengine-web.xml!");
            }
        }
        list.add(new ModuleMetaData(module, host, port));
    }

    protected static String parseModule(WebArchive war) throws Exception {
        final String module = parseModuleRaw(war);
        return (module != null) ? module : DEFAULT;
    }

    protected static String parseModuleRaw(WebArchive war) throws Exception {
        final Node awXml = war.get(ParseUtils.APPENGINE_WEB_XML);
        if (awXml == null) {
            throw new IllegalStateException("Missing appengine-web.xml: " + war.toString(true));
        }
        final Map<String, String> results = ParseUtils.parseTokens(awXml, ParseUtils.MODULE);
        return results.get(ParseUtils.MODULE);
    }

    protected static void safeClose(Closeable closeable) {
        ParseUtils.safeClose(closeable);
    }

    /**
     * Delete app location.
     */
    protected void deleteAppLocation() {
        if (appLocation == null)
            return;

        try {
            deleteRecursively(appLocation);
        } catch (IOException e) {
            log.log(Level.WARNING, "Cannot delete app location.", e);
        } finally {
            appLocation = null;
        }
    }

    /**
     * Shutdown server.
     */
    protected void shutdownServer() {
    }

    /**
     * Teardown GAE Api Env.
     */
    protected void teardown() throws DeploymentException {
    }

    static void deleteRecursively(File file) throws IOException {
        if (file.isDirectory())
            deleteDirectoryContents(file);

        if (file.delete() == false) {
            throw new IOException("Failed to delete " + file);
        }
    }

    static void deleteDirectoryContents(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files == null)
            throw new IOException("Error listing files for " + directory);

        for (File file : files) {
            deleteRecursively(file);
        }
    }
}
