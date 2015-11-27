package org.wildfly.swarm.container.runtime;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.net.URL;

/**
 * @author Heiko Braun
 * @since 26/11/15
 */
public class ParserTestCase {

    private static URL xml;

    @BeforeClass
    public static void init() {
        ClassLoader cl = ParserTestCase.class.getClassLoader();
        xml = cl.getResource("standalone.xml");
    }

    @Test
    public void testCustomParser() throws Exception {
        StandaloneXmlParser parser = new StandaloneXmlParser();
        parser.parse(xml);
    }

    @Test
    public void testDelegatingParser() throws Exception {
        StandaloneXmlParser2 parser = new StandaloneXmlParser2();

        //parser.addDelegate(new QName("urn:jboss:domain:logging:3.0", "subsystem"), StandaloneXmlParser2.NOOP_READER);
        parser.parse(xml);
    }
}
