package net.beaconcontroller.logging.bridge;

import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class BeaconFrameworkListener implements FrameworkListener {
    private static Logger logger = LoggerFactory.getLogger(BeaconFrameworkListener.class);

    public void frameworkEvent(FrameworkEvent event) {
        switch (event.getType()) {
            case FrameworkEvent.ERROR:
                logger.error("Error: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.INFO:
                logger.info("Info: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.PACKAGES_REFRESHED:
                logger.info("Packages Refreshed: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.STARTED:
                logger.info("Started: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.STARTLEVEL_CHANGED:
                logger.info("Startlevel Changed: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.STOPPED:
                logger.info("Stopped: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
                logger.info("Stopped Boot-Classpath Modified: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.STOPPED_UPDATE:
                logger.info("Stopped Update: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.WAIT_TIMEDOUT:
                logger.info("Wait Timedout: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.WARNING:
                logger.error("Warning: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
        }
    }

}
