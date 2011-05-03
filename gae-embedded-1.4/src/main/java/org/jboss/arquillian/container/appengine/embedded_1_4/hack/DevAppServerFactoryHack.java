/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.appengine.embedded_1_4.hack;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.util.PropertyPermission;

import com.google.appengine.tools.development.AppContext;
import com.google.appengine.tools.development.DevAppServer;
import com.google.appengine.tools.development.DevAppServerClassLoaderExposed;
import com.google.appengine.tools.development.DevAppServerFactory;

/**
 * DevAppServerFactory
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: $
 */
public class DevAppServerFactoryHack
{
   public static DevAppServer createDevAppServer(File appLocation, String bindAddress, int bindHttpPort)
   {
      return createDevAppServer(new Class[]{File.class, String.class, Integer.TYPE}, new Object[]{appLocation, bindAddress, bindHttpPort});
   }

   private static DevAppServer createDevAppServer(Class<?>[] ctorArgTypes, Object[] ctorArgs)
   {
      ClassLoader loader = DevAppServerClassLoaderExposed.newClassLoader(DevAppServerFactory.class.getClassLoader());

      DevAppServer devAppServer;
      try
      {
         Class<?> devAppServerClass = Class.forName("com.google.appengine.tools.development.DevAppServerImpl", true, loader);

         Constructor<?> cons = devAppServerClass.getConstructor(ctorArgTypes);
         cons.setAccessible(true);
         devAppServer = (DevAppServer) cons.newInstance(ctorArgs);
      }
      catch (Exception e)
      {
         Throwable t = e;
         if (e instanceof InvocationTargetException)
         {
            t = e.getCause();
         }
         throw new RuntimeException("Unable to create a DevAppServer", t);
      }
      System.setSecurityManager(new CustomSecurityManager(devAppServer));
      return devAppServer;
   }

   private static class CustomSecurityManager extends SecurityManager
   {
      private static final RuntimePermission PERMISSION_MODIFY_THREAD_GROUP = new RuntimePermission("modifyThreadGroup");

      private static final RuntimePermission PERMISSION_MODIFY_THREAD = new RuntimePermission("modifyThread");

      private static final String KEYCHAIN_JNILIB = "/libkeychain.jnilib";

      private static final Object PERMISSION_LOCK = new Object();

      private final DevAppServer devAppServer;

      public CustomSecurityManager(DevAppServer devAppServer)
      {
         this.devAppServer = devAppServer;
      }

      private synchronized boolean appHasPermission(Permission perm)
      {
         synchronized (PERMISSION_LOCK)
         {
            AppContext context = this.devAppServer.getAppContext();
            if ((context.getUserPermissions().implies(perm)) || (context.getApplicationPermissions().implies(perm)))
            {
               return true;
            }
         }
         return ("read".equals(perm.getActions())) && (perm.getName().endsWith(KEYCHAIN_JNILIB));
      }

      public void checkPermission(Permission perm)
      {
         if (perm instanceof PropertyPermission)
         {
            return;
         }

         if (isDevAppServerThread())
         {
            if (appHasPermission(perm))
            {
               return;
            }
            super.checkPermission(perm);
         }
      }

      public void checkPermission(Permission perm, Object context)
      {
         if (isDevAppServerThread())
         {
            if (appHasPermission(perm))
            {
               return;
            }
            super.checkPermission(perm, context);
         }
      }

      public void checkAccess(ThreadGroup g)
      {
         if (g == null)
         {
            throw new NullPointerException("thread group can't be null");
         }
         checkPermission(PERMISSION_MODIFY_THREAD_GROUP);
      }

      public void checkAccess(Thread t)
      {
         if (t == null)
         {
            throw new NullPointerException("thread can't be null");
         }
         checkPermission(PERMISSION_MODIFY_THREAD);
      }

      public boolean isDevAppServerThread()
      {
         return Boolean.getBoolean("devappserver-thread-" + Thread.currentThread().getName());
      }
   }
}
