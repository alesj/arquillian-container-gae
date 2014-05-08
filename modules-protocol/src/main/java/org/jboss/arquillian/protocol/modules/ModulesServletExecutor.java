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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;
import org.jboss.arquillian.protocol.servlet.ServletMethodExecutor;

/**
 * Adjust ServletMethodExecutor to Modular.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 */
public class ModulesServletExecutor extends ServletMethodExecutor {
    public ModulesServletExecutor(ModulesProtocolConfiguration configuration, ProtocolMetaData protocolMetaData, CommandCallback callback) {
        this.config = configuration;
        this.callback = callback;
        this.uriHandler = new ModulesServletURIHandler(configuration, protocolMetaData);
    }

    @Override // TODO -- remove this once ARQ ServletMethodExecutor has prepareHttpConnection
    protected <T> T execute(String url, Class<T> returnType, Object requestObject) throws Exception {
        URLConnection connection = new URL(url).openConnection();
        if (!(connection instanceof HttpURLConnection)) {
            throw new IllegalStateException("Not an http connection! " + connection);
        }
        HttpURLConnection httpConnection = (HttpURLConnection) connection;
        httpConnection.setUseCaches(false);
        httpConnection.setDefaultUseCaches(false);
        httpConnection.setDoInput(true);

        prepareHttpConnection(httpConnection);

        try {

            if (requestObject != null) {
                httpConnection.setRequestMethod("POST");
                httpConnection.setDoOutput(true);
                httpConnection.setRequestProperty("Content-Type", "application/octet-stream");
            }

            if (requestObject != null) {
                ObjectOutputStream ous = new ObjectOutputStream(httpConnection.getOutputStream());
                try {
                    ous.writeObject(requestObject);
                } catch (Exception e) {
                    throw new RuntimeException("Error sending request Object, " + requestObject, e);
                } finally {
                    ous.flush();
                    ous.close();
                }
            }

            try {
                httpConnection.getResponseCode();
            } catch (ConnectException e) {
                return null; // Could not connect
            }
            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                ObjectInputStream ois = new ObjectInputStream(httpConnection.getInputStream());
                Object o;
                try {
                    o = ois.readObject();
                } finally {
                    ois.close();
                }

                if (!returnType.isInstance(o)) {
                    throw new IllegalStateException("Error reading results, expected a " + returnType.getName() + " but got " + o);
                }
                return returnType.cast(o);
            } else if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                return null;
            } else if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
                throw new IllegalStateException(
                    "Error launching test at " + url + ". " + "Got " + httpConnection.getResponseCode() + " (" + httpConnection.getResponseMessage() + ")"
                );
            }
        } finally {
            httpConnection.disconnect();
        }
        return null;
    }

    @SuppressWarnings("UnusedParameters")
    protected void prepareHttpConnection(HttpURLConnection connection) {
        if (ModulesApi.hasCookies()) {
            try {
                String cookies = ModulesApi.getCookies();
                connection.setRequestProperty("Cookie", cookies);
            } finally {
                ModulesApi.removeCookies();
            }
        }
    }
}