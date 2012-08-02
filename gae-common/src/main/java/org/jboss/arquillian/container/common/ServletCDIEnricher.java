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
package org.jboss.arquillian.container.common;

import java.util.logging.Logger;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.arquillian.testenricher.cdi.CDIInjectionEnricher;

/**
 * ServletCDIEnricher
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ServletCDIEnricher extends CDIInjectionEnricher {
    private static final Logger log = Logger.getLogger(ServletCDIEnricher.class.getName());

    @Override
    public BeanManager getBeanManager() {
        try {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            Class<?> beanManagerAccessor = tccl.loadClass("org.jboss.seam.solder.beanManager.BeanManagerLocator");
            return BeanManager.class.cast(beanManagerAccessor.getMethod("lookupBeanManager").invoke(null));
        } catch (Throwable t) {
            log.info("Skipping CDI injections. Either beans.xml is not present or the BeanManager could not be located using BeanManagerLocator from Seam Solder.");
        }
        return null;
    }
}
