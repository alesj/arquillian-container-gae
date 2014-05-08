/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other
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

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;

/**
 * The only class meant to be exposed as public API;
 * e.g. to be consumed by external non-ARQ artifacts.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ModulesApi {
    private static final ThreadLocal<String> cookies = new ThreadLocal<String>();

    public static void addCookie(String key, String value) {
        String current = getCookies();
        if (current == null) {
            cookies.set(String.format("%s=%s", key, value));
        } else {
            cookies.set(String.format("%s=%s;%s", key, value, current));
        }
    }

    public static boolean hasCookies() {
        return (cookies.get() != null);
    }

    public static String getCookies() {
        return cookies.get();
    }

    public static void removeCookies() {
        cookies.remove();
    }

    public static HTTPContext findHTTPContext(ModulesProtocolConfiguration configuration, ProtocolMetaData protocolMetaData, Method method) {
        ModulesServletURIHandler handler = new ModulesServletURIHandler(configuration, protocolMetaData);
        return handler.locateHTTPContext(method);
    }
}
