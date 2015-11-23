package org.wildfly.swarm.datasources.runtime;

import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.wildfly.swarm.container.runtime.AbstractParserFactory;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author Heiko Braun
 * @since 23/11/15
 */
public class DatasourceParserFactory extends AbstractParserFactory {

    @Override
    public XMLElementReader<List<ModelNode>> create() {
        ParsingContext ctx = new ParsingContext();
        DataSourcesExtension ext = new DataSourcesExtension();
        ext.initializeParsers(ctx);
        return (XMLElementReader<List<ModelNode>>) ctx.getParser().get(
                new QName("urn:jboss:domain:datasources:4.0", "datasources")
        );
    }
}
