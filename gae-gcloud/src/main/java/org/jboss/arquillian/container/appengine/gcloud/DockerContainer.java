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

package org.jboss.arquillian.container.appengine.gcloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.dmr.ModelNode;

/**
 * Docker utils.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class DockerContainer {
    private static final Logger log = Logger.getLogger(DockerContainer.class.getName());
    private DockerRoot root;

    private DockerContainer(String... args) throws Exception {
        root = execute("inspect", new AbstractStreamHandler<DockerRoot>() {
            public void handle(InputStream stream) throws IOException {
                set(new DockerRoot(stream));
            }
        }, args);
    }

    static <T> T execute(String op, final StreamHandler<T> handler, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add(op);
        command.addAll(Arrays.asList(args));

        log.info("Docker command: " + command);

        ProcessBuilder builder = new ProcessBuilder(command);
        final Process process = builder.start();
        try {
            Thread contentThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        try (InputStream stream = process.getInputStream()) {
                            handler.handle(stream);
                        }
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            });
            contentThread.start();

            Thread errorThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        try (InputStream stream = process.getErrorStream()) {
                            int b;
                            while ((b = stream.read()) != -1) {
                                System.err.write(b);
                            }
                        }
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            });
            errorThread.start();

            contentThread.join();
            errorThread.join();

            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("Bad exit code: " + exit);
            }

            return handler.get();
        } finally {
            process.destroy();
        }
    }

    public static void removeAll() throws Exception {
        List<String> containers = execute("ps", new AbstractStreamHandler<List<String>>() {
            public void handle(InputStream stream) throws IOException {
                List<String> ids = new ArrayList<>();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    ids.add(line);
                }
                set(ids);
            }
        }, "-a", "-q");

        if (containers.size() > 0) {
            List<String> args = new ArrayList<>();
            args.add("-f");
            args.addAll(containers);
            execute("rm", new AbstractStreamHandler<Void>() {
                public void handle(InputStream stream) throws IOException {
                }
            }, args.toArray(new String[args.size()]));
        }
    }

    public static DockerContainer getLast() throws Exception {
        return new DockerContainer(execute("ps", new AbstractStreamHandler<String>() {
            public void handle(InputStream stream) throws IOException {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                set(reader.readLine());
            }
        }, "-l", "-q"));
    }

    public static DockerContainer getContainer(String id) throws Exception {
        return new DockerContainer(id);
    }

    int getPort() {
        ModelNode tcp8080 = root.getNode("NetworkSettings", "Ports", "8080/tcp");
        List<ModelNode> modelNodes = tcp8080.asList();
        ModelNode portNode = modelNodes.get(0).get("HostPort");
        return portNode.asInt();
    }

    private static class DockerRoot {
        private ModelNode root;

        public DockerRoot(InputStream content) throws IOException {
            ModelNode inspectNode = ModelNode.fromJSONStream(content);
            root = inspectNode.asList().get(0);
        }

        private ModelNode getNode(String... names) {
            ModelNode current = root;

            for (String name : names) {
                if (current == null || !current.isDefined()) {
                    throw new IllegalArgumentException("Invalid names, no such node: " + Arrays.toString(names));
                }
                current = current.get(name);
            }
            return current;
        }
    }

    private static interface StreamHandler<T> {
        T get();

        void set(T value);

        void handle(InputStream stream) throws IOException;
    }

    private static abstract class AbstractStreamHandler<T> implements StreamHandler<T> {
        private T value;

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }

}