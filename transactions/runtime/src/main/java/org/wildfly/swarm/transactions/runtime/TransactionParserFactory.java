package org.wildfly.swarm.transactions.runtime;

import org.jboss.as.txn.subsystem.TransactionExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.wildfly.swarm.container.runtime.AbstractParserFactory;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author Heiko Braun
 * @since 23/11/15
 */
public class TransactionParserFactory extends AbstractParserFactory {

    public XMLElementReader<List<ModelNode>> create() {
        ParsingContext ctx = new ParsingContext();
        TransactionExtension ext = new TransactionExtension();
        ext.initializeParsers(ctx);
        return (XMLElementReader<List<ModelNode>>) ctx.getParser().get(
                new QName("urn:jboss:domain:transactions:3.0", "transactions")
        );
    }


}
