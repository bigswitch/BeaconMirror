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
    private static Logger log = LoggerFactory.getLogger(BeaconFrameworkListener.class);

    public void frameworkEvent(FrameworkEvent event) {
        switch (event.getType()) {
            case FrameworkEvent.ERROR:
                log.error("Error: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.INFO:
                log.info("Info: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.PACKAGES_REFRESHED:
                log.info("Packages Refreshed: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.STARTED:
                log.info("Started: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.STARTLEVEL_CHANGED:
                log.info("Startlevel Changed: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.STOPPED:
                log.info("Stopped: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
                log.info("Stopped Boot-Classpath Modified: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.STOPPED_UPDATE:
                log.info("Stopped Update: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.WAIT_TIMEDOUT:
                log.info("Wait Timedout: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
            case FrameworkEvent.WARNING:
                log.error("Warning: {}", event.getBundle().getSymbolicName(), event.getThrowable());
                break;
        }
    }

}
