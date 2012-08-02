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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * WebAppContext reflection util
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class WebAppContextUtil {
    private Object context;
    private String contextPath;

    WebAppContextUtil(Object context) {
        this.context = context;
    }

    private static <T> T invoke(Object target, Class<T> clazz, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            return clazz.cast(m.invoke(target));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    String getContextPath() {
        if (contextPath == null)
            contextPath = invoke(context, String.class, "getContextPath");

        return contextPath;
    }

    public Iterable<? extends ServletHolder> getServlets() {
        List<ServletHolder> holders = new ArrayList<ServletHolder>();
        Object holder = invoke(context, Object.class, "getServletHandler");
        Object[] servlets = invoke(holder, Object[].class, "getServlets");
        for (Object servlet : servlets)
            holders.add(new ServletHolder(servlet));
        return holders;
    }

    public static class ServletHolder {
        private Object servlet;

        ServletHolder(Object servlet) {
            this.servlet = servlet;
        }

        String getName() {
            return invoke(servlet, String.class, "getName");
        }
    }
}
