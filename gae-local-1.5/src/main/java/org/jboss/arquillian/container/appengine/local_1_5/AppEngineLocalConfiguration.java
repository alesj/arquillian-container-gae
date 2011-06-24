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

package org.jboss.arquillian.container.appengine.local_1_5;

import org.jboss.arquillian.spi.ConfigurationException;
import org.jboss.arquillian.spi.client.container.ContainerConfiguration;

/**
 * AppEngine CLI configuration.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineLocalConfiguration implements ContainerConfiguration
{
   private String sdkDir;
   private String server;
   private String address = "0.0.0.0";
   private int port = 8080;
   private boolean disableUpdateCheck;
   private String javaAgent;
   private String jvmFlags; //

   public void validate() throws ConfigurationException
   {
   }

   public void setSdkDir(String sdkDir)
   {
      this.sdkDir = sdkDir;
   }

   public String getSdkDir()
   {
      return sdkDir;
   }

   public String getServer()
   {
      return server;
   }

   public void setServer(String server)
   {
      this.server = server;
   }

   public String getAddress()
   {
      return address;
   }

   public void setAddress(String address)
   {
      this.address = address;
   }

   public int getPort()
   {
      return port;
   }

   public void setPort(int port)
   {
      this.port = port;
   }

   public boolean isDisableUpdateCheck()
   {
      return disableUpdateCheck;
   }

   public void setDisableUpdateCheck(boolean disableUpdateCheck)
   {
      this.disableUpdateCheck = disableUpdateCheck;
   }

   public String getJavaAgent()
   {
      return javaAgent;
   }

   public void setJavaAgent(String javaAgent)
   {
      this.javaAgent = javaAgent;
   }

   public String getJvmFlags()
   {
      return jvmFlags;
   }

   public void setJvmFlags(String jvmFlags)
   {
      this.jvmFlags = jvmFlags;
   }
}
