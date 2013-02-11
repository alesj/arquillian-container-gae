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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.ServletMappingDef;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.WebAppDescriptor;

/**
 * Common GAE Arquillian container.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AppEngineCommonContainer<T extends ContainerConfiguration> implements DeployableContainer<T> {
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
        return new ProtocolDescription("Servlet 2.5");
    }

    protected void prepareArchive(Archive<?> archive) {
    }

    protected abstract ProtocolMetaData doDeploy(Archive<?> archive) throws DeploymentException;

    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        prepareArchive(archive);

        FixedExplodedExporter exporter = new FixedExplodedExporter(archive,
                AccessController.doPrivileged(new PrivilegedAction<File>() {
                    public File run() {
                        return new File(System.getProperty("java.io.tmpdir"));
                    }
                })
        );
        appLocation = exporter.export();

        return doDeploy(archive);
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

    protected static ProtocolMetaData getProtocolMetaData(String host, int port, Archive<?> archive) {
        HTTPContext httpContext = new HTTPContext(host, port);
        List<String> servlets = extractServlets(archive);
        for (String name : servlets) {
            httpContext.add(new Servlet(name, "")); // GAE apps have root context
        }
        return new ProtocolMetaData().addContext(httpContext);
    }

    protected static List<String> extractServlets(Archive<?> archive) {
        Node webXml = archive.get("WEB-INF/web.xml");
        InputStream stream = webXml.getAsset().openStream();
        try {
            WebAppDescriptor wad = Descriptors.importAs(WebAppDescriptor.class).fromStream(stream);
            List<ServletMappingDef> mappings = wad.getServletMappings();
            List<String> list = new ArrayList<String>();
            for (ServletMappingDef smd : mappings) {
                list.add(smd.getServletName());
            }
            return list;
        } finally {
            safeClose(stream);
        }
    }

    protected static void safeClose(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
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
    protected void teardown() {
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
