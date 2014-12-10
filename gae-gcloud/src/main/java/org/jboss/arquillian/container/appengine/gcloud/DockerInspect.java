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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.jboss.dmr.ModelNode;

/**
 * Docker utils.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class DockerInspect {
    private DockerRoot root;

    public static DockerInspect getLast() {
        return null;
    }

    public static DockerInspect getContainer(String id) {
        return null;
    }

    int getPort() {
        return root.getNode("", "").asInt();
    }

    private static class DockerRoot {
        private ModelNode root;

        public DockerRoot(InputStream content) throws IOException {
            root = ModelNode.fromJSONStream(content);
        }

        private ModelNode getNode(String... names) {
            ModelNode current = root;
            for (String name : names) {
                if (current == null) {
                    throw new IllegalArgumentException("Invalid names, no such node: " + Arrays.toString(names));
                }
                current = current.get(name);
            }
            return current;
        }
    }
}