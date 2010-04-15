package net.beaconcontroller.logging.bridge;

import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class BeaconBundleListener implements BundleListener {
    private static Logger logger = LoggerFactory.getLogger(BeaconBundleListener.class);

    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                logger.info("Installed: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.LAZY_ACTIVATION:
                logger.info("Lazy Activation: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.RESOLVED:
                logger.info("Resolved: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.STARTED:
                logger.info("Started: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.STARTING:
                logger.info("Starting: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.STOPPED:
                logger.info("Stopped: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.STOPPING:
                logger.info("Stopping: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.UNINSTALLED:
                logger.info("Uninstalled: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.UNRESOLVED:
                logger.info("Unresolved: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.UPDATED:
                logger.info("Updated: {}", event.getBundle().getSymbolicName());
                break;
        }
    }
}
