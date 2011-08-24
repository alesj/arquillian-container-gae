/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.arquillian.container.appengine.local_1_5;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.appengine.cli.AppEngineCLIContainer;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Local / development AppEngine container.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineLocalContainer extends AppEngineCLIContainer<AppEngineLocalConfiguration>
{
   private AppEngineLocalConfiguration configuration;
   private Thread kickStartThread;

   public Class<AppEngineLocalConfiguration> getConfigurationClass()
   {
      return AppEngineLocalConfiguration.class;
   }

   public void setup(AppEngineLocalConfiguration configuration)
   {
      this.configuration = configuration;

      if (configuration.getSdkDir() == null)
         throw new ConfigurationException("AppEngine SDK root is null.");

      System.setProperty(AppEngineLocalConfiguration.SDK_ROOT, configuration.getSdkDir());
   }

   protected ProtocolMetaData doDeploy(Archive<?> archive) throws DeploymentException
   {
      try
      {
         List<String> args = new ArrayList<String>();
         args.add("com.google.appengine.tools.development.DevAppServerMain"); // dev app server

         String sdkDir = (String) addArg(args, "sdk_root", configuration.getSdkDir(), false);
         if (new File(sdkDir).isDirectory() == false)
            throw new DeploymentException("SDK root is not a directory: " + sdkDir);

         addArg(args, "server", configuration.getServer(), true);

         addArg(args, "address", configuration.getAddress(), false);
         addArg(args, "port", configuration.getPort(), false);
         addArg(args, "startOnFirstThread", configuration.isDisableUpdateCheck(), false);
         addArg(args, "disable_update_check", configuration.isStartOnFirstThread());
         boolean isJavaAgentSet = (configuration.getJavaAgent() != null);
         if (isJavaAgentSet)
         {
            addArg(args, "jvm_flag", "-noverify", false);
            addArg(args, "jvm_flag", "-javaagent:" + configuration.getJavaAgent(), false);
         }
         // TODO -- JVM FLAGS
         args.add(getAppLocation().getCanonicalPath());

         invokeKickStart(args.toArray(new String[args.size()]));

         delayArchiveDeploy(1000L);

         return getProtocolMetaData(configuration.getAddress(), configuration.getPort(), archive);
      }
      catch (Exception e)
      {
         throw new DeploymentException("Cannot deploy to local GAE.", e);
      }
   }

   @Override
   protected void shutdownServer()
   {
      if (kickStartThread != null)
      {
         kickStartThread.interrupt();
         kickStartThread = null;
      }
   }

   protected void invokeKickStart(final Object args) throws Exception
   {
      File lib = new File(configuration.getSdkDir(), "lib");
      File tools = new File(lib, "appengine-tools-api.jar");
      if (tools.exists() == false)
         throw new IllegalArgumentException("No AppEngine tools jar: " + tools);

      URL url = tools.toURI().toURL();
      URL[] urls = new URL[]{url};
      ClassLoader cl = new URLClassLoader(urls);
      Class<?> kickStartClass = cl.loadClass("com.google.appengine.tools.KickStart");
      final Method main = kickStartClass.getMethod("main", String[].class);

      Runnable runnable = new Runnable()
      {
         public void run()
         {
            try
            {
               main.invoke(null, args);
            }
            catch (Exception e)
            {
               throw new RuntimeException(e);
            }
         }
      };
      kickStartThread = new Thread(runnable, "AppEngine Local KickStart thread");
      kickStartThread.start();
   }

   protected void delayArchiveDeploy(long checkPeriod) throws Exception
   {
      String serverURL = configuration.getServerTestURL();
      if (serverURL == null)
         serverURL = "http://localhost:" + configuration.getPort() + "/test";
      URL server = new URL(serverURL);
      long startupTimeout = configuration.getStartupTimeout();
      long timeout = startupTimeout * 1000;

      while (timeout > 0)
      {
         Thread.sleep(checkPeriod);
         try
         {
            server.openStream();
            break;
         }
         catch (Throwable ignored)
         {
            timeout -= checkPeriod;
         }
      }
      if (timeout <= 0)
         throw new IllegalStateException("Cannot connect to managed AppEngine, timed out.");
   }
}
