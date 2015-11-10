package org.jboss.as.logging;

/**
 * The actual logging parsers are package protected, hence this intermediary.
 *
 * @author Heiko Braun
 * @since 10/11/15
 */
public class LoggingParserFactory {
    public LoggingSubsystemParser_3_0 create() {
        return new LoggingSubsystemParser_3_0();
    }
}
