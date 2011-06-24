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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.common.AppEngineCommonContainer;
import org.jboss.arquillian.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;

/**
 * AppEngine CLI container.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AppEngineCLIContainer<T extends ContainerConfiguration> extends AppEngineCommonContainer<T>
{
   protected static ProtocolMetaData getProtocolMetaData(String host, int port, Archive<?> archive)
   {
      HTTPContext httpContext = new HTTPContext(host, port);
      Map<String, String> servlets = extractServlets(archive);
      for (Map.Entry<String, String> entry : servlets.entrySet())
      {
         String name = entry.getKey();
         String contextPath = entry.getValue();
         httpContext.add(new Servlet(name, contextPath));
      }
      return new ProtocolMetaData().addContext(httpContext);
   }

   protected static Map<String, String> extractServlets(Archive<?> archive)
   {
      return Collections.emptyMap(); // TODO
   }

   protected static void addArg(List<String> args, String key, boolean condition)
   {
      if (condition)
         args.add("--" + key);
   }

   protected static void addArg(List<String> args, String key, Object value, boolean optional)
   {
      if (value == null && optional == false)
         throw new IllegalArgumentException("Missing argument value: " + key);

      args.add("--" + key + "=" + value);
   }

   protected static void addArg(List<String> args, String key, Object value, String sysKey)
   {
      if (sysKey != null)
         value = System.getProperty(sysKey, String.valueOf(value));

      if (value == null)
         throw new IllegalArgumentException("Missing argument value: " + key + " / " + sysKey);

      if (sysKey != null)
         System.setProperty(sysKey, String.valueOf(value));

      args.add("--" + key + "=" + value);
   }
}