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

package org.jboss.arquillian.container.appengine.tools;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.protocol.modules.OperateOnModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
public class AppEngineToolsModulesTestCase {
    /**
     * Deployment for the test
     *
     * @return web archive
     */
    @Deployment
    public static EnterpriseArchive getModulesArchive() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "simple.ear");

        ear.addAsManifestResource("application.xml");
        ear.addAsManifestResource("appengine-application.xml");

        WebArchive war1 = getTestArchive(1).addAsWebInfResource("appengine-web.xml");
        WebArchive war2 = getTestArchive(2).addAsWebInfResource("m2-appengine-web.xml", "appengine-web.xml");

        ear.addAsModule(war1);
        ear.addAsModule(war2);

        return ear;
    }

    protected static WebArchive getTestArchive(int i) {
        return ShrinkWrap.create(WebArchive.class, String.format("simple%s.war", i))
                .addClass(AppEngineToolsModulesTestCase.class)
                .addClass(TestServlet.class)
                .setWebXML("gae-web.xml")
                .addAsWebInfResource("logging.properties");
    }

    @Test
    public void testPing1() throws Exception {
        System.out.println("Ping-1!");
    }

    @Test
    @OperateOnModule("m2")
    public void testPing2() throws Exception {
        System.out.println("Ping-2!");
    }
}
