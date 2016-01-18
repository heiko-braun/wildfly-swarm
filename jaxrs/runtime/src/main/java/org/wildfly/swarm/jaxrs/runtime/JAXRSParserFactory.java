package org.wildfly.swarm.jaxrs.runtime;

import org.jboss.as.jaxrs.JaxrsExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.wildfly.swarm.container.runtime.AbstractParserFactory;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @since 24/11/15
 */
public class JAXRSParserFactory extends AbstractParserFactory {
    @Override
    public Map<QName, XMLElementReader<List<ModelNode>>> create() {
        ParsingContext ctx = new ParsingContext();
        JaxrsExtension ext = new JaxrsExtension();
        ext.initializeParsers(ctx);
        return ctx.getParser();

    }
}
