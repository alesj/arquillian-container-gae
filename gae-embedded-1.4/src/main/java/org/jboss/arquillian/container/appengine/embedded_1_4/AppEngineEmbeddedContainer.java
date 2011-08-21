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

package org.jboss.arquillian.container.appengine.embedded_1_4;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.logging.Level;

import com.google.appengine.tools.development.AppContext;
import com.google.appengine.tools.development.DevAppServer;
import org.jboss.arquillian.container.appengine.embedded_1_4.hack.AppEngineHack;
import org.jboss.arquillian.container.appengine.embedded_1_4.hack.DevAppServerFactoryHack;
import org.jboss.arquillian.container.common.AppEngineCommonContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Start AppEngine Embedded Container.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineEmbeddedContainer extends AppEngineCommonContainer<AppEngineEmbeddedConfiguration>
{
   private AppEngineEmbeddedConfiguration containerConfig;
   private DevAppServer server;

   public Class<AppEngineEmbeddedConfiguration> getConfigurationClass()
   {
      return AppEngineEmbeddedConfiguration.class;
   }

   public void setup(AppEngineEmbeddedConfiguration configuration)
   {
      this.containerConfig = configuration;
   }

   protected void prepareArchive(Archive<?> archive)
   {
      // add a GAE libs
      AppEngineSetup.prepare(archive);
   }

   protected ProtocolMetaData doDeploy(Archive<?> archive) throws DeploymentException
   {
      try
      {
         server = AccessController.doPrivileged(new PrivilegedExceptionAction<DevAppServer>()
         {
            public DevAppServer run() throws Exception
            {
               return DevAppServerFactoryHack.createDevAppServer(getAppLocation(), containerConfig.getBindAddress(), containerConfig.getBindHttpPort());
            }
         });
         Map properties = System.getProperties();
         //noinspection unchecked
         server.setServiceProperties(properties);
         server.start();
      }
      catch (Exception e)
      {
         server = null;
         deleteAppLocation();

         throw new DeploymentException("Error starting AppEngine.", e);
      }

      try
      {
         setup("start", getAppLocation(), containerConfig.getBindHttpPort(), containerConfig.getBindAddress());
      }
      catch (Exception e)
      {
         shutdownServer();
         deleteAppLocation();

         throw new DeploymentException("Cannot setup GAE Api Environment", e);
      }

      try
      {
         HTTPContext httpContext = new HTTPContext(containerConfig.getBindAddress(), containerConfig.getBindHttpPort());
         AppContext context = server.getAppContext();
         WebAppContextUtil wctx = new WebAppContextUtil(context.getContainerContext());
         for (WebAppContextUtil.ServletHolder servlet : wctx.getServlets())
         {
            httpContext.add(new Servlet(servlet.getName(), wctx.getContextPath()));
         }
         return new ProtocolMetaData().addContext(httpContext);
      }
      catch (Exception e)
      {
         teardown();
         shutdownServer();
         deleteAppLocation();

         throw new DeploymentException("Could not create ContainerMethodExecutor", e);
      }
   }

   /**
    * Hack with ApiProxy.
    *
    * @param methodName the method name
    * @param args the arguments
    * @throws Exception for any error
    */
   private void setup(String methodName, Object... args) throws Exception
   {
      AppContext appContext = server.getAppContext();
      ClassLoader cl = appContext.getClassLoader();
      Class<?> clazz = cl.loadClass(AppEngineHack.class.getName());
      Class[] classes = new Class[0];
      if (args != null && args.length > 0)
      {
         classes = new Class[args.length];
         for (int i = 0; i < args.length; i++)
            classes[i] = args[i].getClass();
      }
      Method method = clazz.getMethod(methodName, classes);
      Object instance = clazz.newInstance();
      method.invoke(instance, args);
   }

   /**
    * Shutdown server.
    */
   protected void shutdownServer()
   {
      if (server == null)
         return;

      try
      {
         server.shutdown();
      }
      catch (Exception e)
      {
         log.log(Level.SEVERE, "Error shutting down AppEngine", e);
      }
      finally
      {
         server = null;
      }
   }

   /**
    * Teardown GAE Api Env.
    */
   protected void teardown()
   {
      try
      {
         setup("stop");
      }
      catch (Exception ignored)
      {
      }
   }
}
