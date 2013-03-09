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

package org.jboss.arquillian.container.common;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Useful LogManager impl to check GAE logging.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class HackLogManager extends LogManager {
    public synchronized Logger getLogger(String name) {
        if (name.startsWith("com.google.")) {
            Logger logger = new HackLogger(name, null);
            logger.setLevel(Level.ALL);
            addLogger(logger);
            return logger;
        }
        return super.getLogger(name);
    }

    private static class HackLogger extends Logger {
        private HackLogger(String name, String resourceBundleName) {
            super(name, resourceBundleName);
        }

        @Override
        public void log(LogRecord record) {
            record.setLevel(Level.INFO);
            super.log(record);
        }
    }
}
