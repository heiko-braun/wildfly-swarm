package org.wildfly.swarm.ee.runtime;

import org.jboss.as.ee.subsystem.EeExtension;
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
public class EEParserFactory extends AbstractParserFactory {
    @Override
    public Map<QName, XMLElementReader<List<ModelNode>>> create() {
        ParsingContext ctx = new ParsingContext();
        EeExtension ext = new EeExtension();
        ext.initializeParsers(ctx);
        return ctx.getParser();
    }
}
