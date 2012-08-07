package org.jboss.arquillian.container.appengine.remote;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Add index.html, if it doesn't exist yet.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class IndexHtmlAppender implements ApplicationArchiveProcessor {
    private static final String INDEX_HTML = "index.html";

    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            WebArchive war = (WebArchive) archive;
            if (war.contains(INDEX_HTML) == false) {
                war.add(new StringAsset(
                        "<html>" +
                                "<head>" +
                                "<title>Arquillian GAE Remote Container</title>" +
                                "</head>" +
                                "<body>Ping?</body>" +
                                "</html>"),
                        INDEX_HTML);
            }
        }
    }
}
