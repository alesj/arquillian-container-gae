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

package org.jboss.arquillian.container.appengine.remote_1_5;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.appengine.cli.AppEngineCLIContainer;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Remote / production AppEngine container.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineRemoteContainer extends AppEngineCLIContainer<AppEngineRemoteConfiguration>
{
   private AppEngineRemoteConfiguration configuration;

   public Class<AppEngineRemoteConfiguration> getConfigurationClass()
   {
      return AppEngineRemoteConfiguration.class;
   }

   public void setup(AppEngineRemoteConfiguration configuration)
   {
      this.configuration = configuration;

      if (configuration.getSdkDir() == null)
         throw new ConfigurationException("AppEngine SDK root is null.");

      System.setProperty(AppEngineRemoteConfiguration.SDK_ROOT, configuration.getSdkDir());
   }

   protected ProtocolMetaData doDeploy(Archive<?> archive) throws DeploymentException
   {
      try
      {
         List<String> args = new ArrayList<String>();

         String sdkDir = configuration.getSdkDir();
         if (new File(sdkDir).isDirectory() == false)
            throw new DeploymentException("SDK root is not a directory: " + sdkDir);

         addArg(args, "email", configuration.getEmail(), false);
         addArg(args, "host", configuration.getHost(), true);
         addArg(args, "compile_encoding", configuration.getEncoding(), true);
         addArg(args, "proxy", configuration.getProxy(), true);
         addArg(args, "passin", configuration.isPassIn());
         if (configuration.isPassIn() == false)
            addArg(args, "disable_prompt", configuration.isPrompt());
         addArg(args, "enable_jar_splitting", configuration.isSplitJars());
         addArg(args, "retain_upload_dir", configuration.isKeepTempUploadDir());
         args.add("update");
         args.add(getAppLocation().getCanonicalPath());

         invokeAppEngine(sdkDir, "com.google.appengine.tools.admin.AppCfg", args.toArray(new String[args.size()]));

         delayArchiveDeploy(configuration.getServerURL(), configuration.getStartupTimeout(), 5000L);

         return getProtocolMetaData(configuration.getServerURL(), 80, archive);
      }
      catch (Exception e)
      {
         throw new DeploymentException("Cannot deploy to local GAE.", e);
      }
   }
}
