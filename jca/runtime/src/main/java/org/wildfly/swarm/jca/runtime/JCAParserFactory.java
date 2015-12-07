package org.wildfly.swarm.jca.runtime;

import org.jboss.as.connector.subsystems.jca.JcaExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.wildfly.swarm.container.runtime.AbstractParserFactory;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @since 23/11/15
 */
public class JCAParserFactory extends AbstractParserFactory {

    @Override
    public Map<QName, XMLElementReader<List<ModelNode>>> create() {
        ParsingContext ctx = new ParsingContext();
        JcaExtension ext = new JcaExtension();
        ext.initializeParsers(ctx);
        return ctx.getParser();
    }
}
