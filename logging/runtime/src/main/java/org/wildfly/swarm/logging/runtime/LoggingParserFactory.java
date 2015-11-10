package org.wildfly.swarm.logging.runtime;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ProfileParsingCompletionHandler;
import org.jboss.as.logging.LoggingExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The actual logging parsers are package protected, hence this intermediary.
 *
 * @author Heiko Braun
 * @since 10/11/15
 */
public class LoggingParserFactory {
    public XMLElementReader<List<ModelNode>> create() {

        ParsingContext ctx = new ParsingContext();
        LoggingExtension ext = new LoggingExtension();
        ext.initializeParsers(ctx);
        return (XMLElementReader<List<ModelNode>>) ctx.getParser().get(new QName("urn:jboss:domain:logging:3.0", "logging"));
    }

    class ParsingContext implements ExtensionParsingContext {

        Map<QName,  XMLElementReader<List<ModelNode>>> parsers = new HashMap<>();

        public Map<QName, XMLElementReader<List<ModelNode>>> getParser() {
            return parsers;
        }

        @Override
        public ProcessType getProcessType() {
            return ProcessType.STANDALONE_SERVER;
        }

        @Override
        public RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }

        @Override
        public void setSubsystemXmlMapping(String localName, String namespace, XMLElementReader<List<ModelNode>> parser) {
            parsers.put(new QName(namespace, localName), parser);

        }

        @Override
        public void setProfileParsingCompletionHandler(ProfileParsingCompletionHandler profileParsingCompletionHandler) {
            // ignore
        }
    }
}
