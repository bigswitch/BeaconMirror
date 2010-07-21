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
    private static Logger log = LoggerFactory.getLogger(BeaconBundleListener.class);

    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                log.info("Installed: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.LAZY_ACTIVATION:
                log.info("Lazy Activation: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.RESOLVED:
                log.info("Resolved: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.STARTED:
                log.info("Started: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.STARTING:
                log.info("Starting: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.STOPPED:
                log.info("Stopped: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.STOPPING:
                log.info("Stopping: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.UNINSTALLED:
                log.info("Uninstalled: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.UNRESOLVED:
                log.info("Unresolved: {}", event.getBundle().getSymbolicName());
                break;
            case BundleEvent.UPDATED:
                log.info("Updated: {}", event.getBundle().getSymbolicName());
                break;
        }
    }
}
