/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.arquillian.container.appengine.embedded;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

import org.jboss.arquillian.container.appengine.embedded.hack.AppEngineHack;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Setup AppEngine libs.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class AppEngineSetup {
    private static Logger log = Logger.getLogger(AppEngineSetup.class.getName());

    private static final String LOCAL_MAVEN_REPO = System.getProperty("user.home") + File.separatorChar + ".m2" + File.separatorChar + "repository";

    private static final String GROUP_ID = "com.google.appengine";
    private static final String ARTIFACT_ID = "appengine-api-1.0-sdk";
    private static final String VERSION = System.getProperty("appengine.version", "1.9.17");

    /**
     * Add class owner location to java.ext.dirs.
     * This method should be invoked in privileged block.
     *
     * @param className the class name
     */
    static void addToJavaExtDirs(String className) {
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            addToJavaExtDirs(cl.loadClass(className));
        } catch (Throwable ignored) {
        }
    }

    /**
     * Add class owner location to java.ext.dirs.
     * This method should be invoked in privileged block.
     *
     * @param clazz the class
     */
    static void addToJavaExtDirs(Class<?> clazz) {
        if (clazz == null)
            throw new IllegalArgumentException("Null class");

        ProtectionDomain domain = clazz.getProtectionDomain();
        CodeSource source = domain.getCodeSource();
        URL location = source.getLocation();
        String addon = location.getPath();
        String ext = System.getProperty("java.ext.dirs");
        if (ext == null) {
            System.setProperty("java.ext.dirs", addon);
        } else {
            System.setProperty("java.ext.dirs", ext + File.pathSeparator + addon);
        }
    }

    /**
     * Prepare AppEngine libs.
     *
     * @param archive the current archive
     */
    static void prepare(AppEngineEmbeddedConfiguration configuration, Archive archive) {
        File gaeAPI = getFile(configuration);

        WebArchive webArchive = archive.as(WebArchive.class);
        webArchive.addAsLibraries(gaeAPI);
        webArchive.addClass(AppEngineHack.class); // hack

        log.info(webArchive.toString(true));
    }

    private static File getFile(AppEngineEmbeddedConfiguration configuration) {
        File file = null;

        String sdkRoot = configuration.getSdkDir();
        if (sdkRoot == null) {
            file = resolve(GROUP_ID, ARTIFACT_ID, VERSION);
        } else {
            File userLib = new File(sdkRoot, "lib/user/");
            for (File lib : userLib.listFiles()) {
                if (lib.getName().startsWith(ARTIFACT_ID)) {
                    file = lib;
                    break;
                }
            }
        }

        if (file == null || file.exists() == false)
            throw new IllegalArgumentException("Missing AppEngine library: " + file);

        return file;
    }

    private static File resolve(String groupId, String artifactId, String version) {
        return new File(LOCAL_MAVEN_REPO + File.separatorChar +
            groupId.replace(".", File.separator) + File.separatorChar +
            artifactId + File.separatorChar +
            version + File.separatorChar +
            artifactId + "-" + version + ".jar");
    }
}
