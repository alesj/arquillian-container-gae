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

package org.jboss.arquillian.protocol.modules;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.protocol.servlet.ServletMethodExecutor;
import org.jboss.arquillian.protocol.servlet.ServletURIHandler;

/**
 * Adjust ServletMethodExecutor to Modular.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 */
public class ModulesServletURIHandler extends ServletURIHandler {
    private ModulesProtocolConfiguration configuration;
    private Collection<ModuleMetaData> modules;

    public ModulesServletURIHandler(ModulesProtocolConfiguration config, Collection<ModuleMetaData> modules) {
        super(config, Collections.<HTTPContext>singleton(null)); // push non-empty http contexts
        this.configuration = config;
        this.modules = modules;
    }

    @Override
    protected HTTPContext locateHTTPContext(Method method) {
        final ModuleContext mc = locateModuleContext(method);
        HTTPContext context = new HTTPContext(mc.getHost(), mc.getPort());
        Servlet servlet = new Servlet(ServletMethodExecutor.ARQUILLIAN_SERVLET_NAME, "");
        context.add(servlet);
        return context;
    }

    protected ModuleContext locateModuleContext(Method method) {
        String module = "default";

        OperateOnModule oom = method.getAnnotation(OperateOnModule.class);
        if (oom != null) {
            module = oom.value();
        }

        for (ModuleMetaData mmd : modules) {
            if (module.equals(mmd.getModule())) {
                return new ModuleContextWrapper(mmd, oom);
            }
        }

        if (configuration.isFailOnMissingModule()) {
            throw new IllegalStateException(String.format("No matching module %s - %s", module, modules));
        }

        String h = configuration.getProperty(module + ".host");
        String p = configuration.getProperty(module + ".port");
        String host = (h != null) ? h : "localhost";
        int port = (p != null) ? Integer.parseInt(p) : (configuration.getPort() != null ? configuration.getPort() : 8080);
        ModuleMetaData mmd = new ModuleMetaData(null, host, port); // ignore module info
        return new ModuleContextWrapper(mmd, oom);
    }
}