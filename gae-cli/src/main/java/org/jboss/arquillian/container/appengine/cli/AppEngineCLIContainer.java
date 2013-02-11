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

package org.jboss.arquillian.container.appengine.cli;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.jboss.arquillian.container.common.AppEngineCommonContainer;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/**
 * AppEngine CLI container.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AppEngineCLIContainer<T extends ContainerConfiguration> extends AppEngineCommonContainer<T> {
    private Thread appEngineThread;

    protected void invokeAppEngine(String sdkDir, String appEngineClass, final Object args) throws Exception {
        invokeAppEngine(null, sdkDir, appEngineClass, args);
    }

    protected void invokeAppEngine(ThreadGroup threads, String sdkDir, String appEngineClass, final Object args) throws Exception {
        File lib = new File(sdkDir, "lib");
        File tools = new File(lib, "appengine-tools-api.jar");
        if (tools.exists() == false)
            throw new IllegalArgumentException("No AppEngine tools jar: " + tools);

        URL url = tools.toURI().toURL();
        URL[] urls = new URL[]{url};
        ClassLoader cl = new URLClassLoader(urls);
        Class<?> kickStartClass = cl.loadClass(appEngineClass);
        final Method main = kickStartClass.getMethod("main", String[].class);

        Runnable runnable = createRunnable(threads, main, args);
        appEngineThread = new Thread(threads, runnable, "AppEngine thread: " + getClass().getSimpleName());
        appEngineThread.start();

    }

    protected Runnable createRunnable(final ThreadGroup threads, final Method main, final Object args) {
        return new Runnable() {
            public void run() {
                try {
                    main.invoke(null, args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    protected void delayArchiveDeploy(String serverURL, long startupTimeout, long checkPeriod) throws Exception {
        if (serverURL == null)
            throw new IllegalArgumentException("Null server url");

        final URL server = new URL(serverURL);
        log.info("Pinging server url: " + serverURL);

        long timeout = startupTimeout * 1000;
        while (timeout > 0) {
            Thread.sleep(checkPeriod);
            try {
                server.openStream();
                break;
            } catch (Throwable ignored) {
                timeout -= checkPeriod;
            }
        }
        if (timeout <= 0)
            throw new IllegalStateException("Cannot connect to managed AppEngine, timed out.");
    }

    @Override
    protected void shutdownServer() {
        if (appEngineThread != null) {
            appEngineThread.interrupt();
            appEngineThread = null;
        }
    }

    protected static void addArg(List<String> args, String key, boolean condition) {
        if (condition)
            args.add("--" + key);
    }

    protected static Object addArg(List<String> args, String key, Object value, boolean optional) {
        if (value == null && optional == false)
            throw new IllegalArgumentException("Missing argument value: " + key);

        if (value != null)
            args.add("--" + key + "=" + value);

        return value;
    }
}
