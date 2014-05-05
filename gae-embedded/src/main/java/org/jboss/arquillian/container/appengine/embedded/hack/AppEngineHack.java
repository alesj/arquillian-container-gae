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

package org.jboss.arquillian.container.appengine.embedded.hack;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.apphosting.api.ApiProxy;

/**
 * AppEngine ApiProxy hack.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineHack {
    public void start(final File appLocation, final Integer port, final String address) throws Exception {
        ApiProxy.setEnvironmentForCurrentThread(new ApiProxy.Environment() {
            public String getAppId() {
                return appLocation.getName();
            }

            public String getModuleId() {
                return "default";
            }

            public String getVersionId() {
                return "1.0";
            }

            public String getEmail() {
                return null;
            }

            public boolean isLoggedIn() {
                return false;
            }

            public boolean isAdmin() {
                return false;
            }

            public String getAuthDomain() {
                return "jboss.org";
            }

            public String getRequestNamespace() {
                return null;
            }

            public Map<String, Object> getAttributes() {
                return new ConcurrentHashMap<String, Object>();
            }

            public long getRemainingMillis() {
                return 0L;
            }
        });

        ClassLoader serverCL = ApiProxy.class.getClassLoader();
        Class<?> apfClass = serverCL.loadClass("com.google.appengine.tools.development.ApiProxyLocalFactory");
        Class<?> envClass = serverCL.loadClass("com.google.appengine.tools.development.LocalServerEnvironment");
        Method create = apfClass.getMethod("create", envClass);
        Object target = Proxy.newProxyInstance(serverCL, new Class<?>[]{envClass}, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("getAppDir".equals(name))
                    return appLocation;
                else if ("getPort".equals(name))
                    return port;
                else if ("getAddress".equals(name))
                    return address;
                else if ("waitForServerToStart".equals(name))
                    return null;
                else if ("enforceApiDeadlines".equals(name))
                    return false;
                else if ("simulateProductionLatencies".equals(name))
                    return false;
                else
                    throw new IllegalArgumentException("No such method supported: " + name);
            }
        });
        Object result = create.invoke(apfClass.newInstance(), target);
        ApiProxy.Delegate delegate = ApiProxy.Delegate.class.cast(result);
        ApiProxy.setDelegate(delegate);
    }

    public void stop() {
        ApiProxy.setDelegate(null);
        ApiProxy.clearEnvironmentForCurrentThread();
    }
}
