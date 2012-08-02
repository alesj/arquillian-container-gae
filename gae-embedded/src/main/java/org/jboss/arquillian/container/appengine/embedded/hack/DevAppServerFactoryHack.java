package org.jboss.arquillian.container.appengine.embedded.hack;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;

import com.google.appengine.tools.development.AppContext;
import com.google.appengine.tools.development.DevAppServer;
import com.google.appengine.tools.development.DevAppServerClassLoaderExposed;
import com.google.appengine.tools.development.DevAppServerFactory;

/**
 * DevAppServerFactory
 *
 * @author Google GAE team
 */
public class DevAppServerFactoryHack {
    private static final Class[] DEV_APPSERVER_CTOR_ARG_TYPES = {File.class, File.class, File.class, File.class, String.class, Integer.TYPE, Boolean.TYPE, Map.class};

    public static DevAppServer createDevAppServer(File appLocation, String bindAddress, int bindHttpPort) {
        return createDevAppServer(new Object[]{appLocation, null, null, null, bindAddress, bindHttpPort, true, new HashMap()});
    }

    private static DevAppServer createDevAppServer(Object[] ctorArgs) {
        ClassLoader loader = DevAppServerClassLoaderExposed.newClassLoader(DevAppServerFactory.class.getClassLoader());

        DevAppServer devAppServer;
        try {
            Class<?> devAppServerClass = Class.forName("com.google.appengine.tools.development.DevAppServerImpl", true, loader);
            Constructor<?> cons = devAppServerClass.getConstructor(DEV_APPSERVER_CTOR_ARG_TYPES);
            cons.setAccessible(true);
            devAppServer = (DevAppServer) cons.newInstance(ctorArgs);
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof InvocationTargetException) {
                t = e.getCause();
            }
            throw new RuntimeException("Unable to create a DevAppServer", t);
        }
        System.setSecurityManager(new CustomSecurityManager(devAppServer));
        return devAppServer;
    }

    private static class CustomSecurityManager extends SecurityManager {
        private static final RuntimePermission PERMISSION_MODIFY_THREAD_GROUP = new RuntimePermission("modifyThreadGroup");

        private static final RuntimePermission PERMISSION_MODIFY_THREAD = new RuntimePermission("modifyThread");

        private static final String KEYCHAIN_JNILIB = "/libkeychain.jnilib";

        private static final Object PERMISSION_LOCK = new Object();

        private final DevAppServer devAppServer;

        public CustomSecurityManager(DevAppServer devAppServer) {
            this.devAppServer = devAppServer;
        }

        private synchronized boolean appHasPermission(Permission perm) {
            synchronized (PERMISSION_LOCK) {
                AppContext context = this.devAppServer.getAppContext();
                if ((context.getUserPermissions().implies(perm)) || (context.getApplicationPermissions().implies(perm))) {
                    return true;
                }
            }
            return ("read".equals(perm.getActions())) && (perm.getName().endsWith(KEYCHAIN_JNILIB));
        }

        public void checkPermission(Permission perm) {
            if (perm instanceof PropertyPermission) {
                return;
            }

            if (isDevAppServerThread()) {
                if (appHasPermission(perm)) {
                    return;
                }
                super.checkPermission(perm);
            }
        }

        public void checkPermission(Permission perm, Object context) {
            if (isDevAppServerThread()) {
                if (appHasPermission(perm)) {
                    return;
                }
                super.checkPermission(perm, context);
            }
        }

        public void checkAccess(ThreadGroup g) {
            if (g == null) {
                throw new NullPointerException("thread group can't be null");
            }
            checkPermission(PERMISSION_MODIFY_THREAD_GROUP);
        }

        public void checkAccess(Thread t) {
            if (t == null) {
                throw new NullPointerException("thread can't be null");
            }
            checkPermission(PERMISSION_MODIFY_THREAD);
        }

        public boolean isDevAppServerThread() {
            return Boolean.getBoolean("devappserver-thread-" + Thread.currentThread().getName());
        }
    }
}
