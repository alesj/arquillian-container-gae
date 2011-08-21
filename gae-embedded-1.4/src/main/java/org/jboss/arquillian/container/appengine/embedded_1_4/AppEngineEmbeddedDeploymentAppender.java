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

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.testenricher.cdi.CDIInjectionEnricher;
import org.jboss.arquillian.testenricher.resource.ResourceInjectionEnricher;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.kohsuke.MetaInfServices;


/**
 * Package the test enrichers supported by the AppEngine Embedded 1.4.x Container plugin.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@MetaInfServices
public class AppEngineEmbeddedDeploymentAppender implements AuxiliaryArchiveAppender
{
   public Archive<?> createAuxiliaryArchive()
   {
      return ShrinkWrap.create(JavaArchive.class, "arquillian-appengine-testenrichers.jar")
                        .addPackages(
                              false,
                              ServletCDIEnricher.class.getPackage(),
                              CDIInjectionEnricher.class.getPackage(),
                              ResourceInjectionEnricher.class.getPackage())
                        .addAsServiceProvider(
                              TestEnricher.class,
                              ServletCDIEnricher.class,
                              ResourceInjectionEnricher.class);
   }
}

