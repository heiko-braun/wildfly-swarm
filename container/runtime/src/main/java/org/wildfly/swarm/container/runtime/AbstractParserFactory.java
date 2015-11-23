package org.wildfly.swarm.container.runtime;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ProfileParsingCompletionHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @since 23/11/15
 */
public abstract class AbstractParserFactory {

    public abstract XMLElementReader<List<ModelNode>> create();

    public class ParsingContext implements ExtensionParsingContext {

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
