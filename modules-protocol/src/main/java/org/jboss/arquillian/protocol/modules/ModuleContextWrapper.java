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

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class ModuleContextWrapper implements ModuleContext {
    private static final String DOT = "-dot-";

    private ModuleContext context;
    private OperateOnModule oom;

    ModuleContextWrapper(ModuleContext context, OperateOnModule oom) {
        this.context = context;
        this.oom = oom;
    }

    public String getModule() {
        return context.getModule();
    }

    public String getHost() {
        if (oom == null) {
            return context.getHost();
        }

        StringBuilder host = new StringBuilder();
        int instance = oom.instance();
        if (instance > 0) {
            host.append(instance).append(DOT);
        }
        String version = oom.version();
        if (version != null && version.trim().length() > 0) {
            host.append(version).append(DOT);
        }
        String module = getModule();
        if (module != null) {
            host.append(module).append(DOT);
        }
        host.append(context.getHost());

        return host.toString();
    }

    public int getPort() {
        return context.getPort();
    }
}
