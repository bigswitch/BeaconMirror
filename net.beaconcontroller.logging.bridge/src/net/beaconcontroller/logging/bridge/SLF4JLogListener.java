package net.beaconcontroller.logging.bridge;

import org.eclipse.equinox.log.ExtendedLogEntry;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class SLF4JLogListener implements LogListener {
    private static Logger logger = LoggerFactory.getLogger(SLF4JLogListener.class);

    public void logged(LogEntry entry) {
        Logger logger = SLF4JLogListener.logger;
        if (entry instanceof ExtendedLogEntry) {
            ExtendedLogEntry extendedEntry = (ExtendedLogEntry) entry;
            String loggerName = extendedEntry.getLoggerName();
            if (loggerName != null)
                logger = LoggerFactory.getLogger(loggerName);
        }

        switch (entry.getLevel()) {
            case LogService.LOG_DEBUG:
                logger.debug(entry.getMessage(), entry.getException());
                break;
            case LogService.LOG_ERROR:
                logger.error(entry.getMessage(), entry.getException());
                break;
            case LogService.LOG_INFO:
                logger.info(entry.getMessage(), entry.getException());
                break;
            case LogService.LOG_WARNING:
                logger.warn(entry.getMessage(), entry.getException());
                break;
        }
    }
}
