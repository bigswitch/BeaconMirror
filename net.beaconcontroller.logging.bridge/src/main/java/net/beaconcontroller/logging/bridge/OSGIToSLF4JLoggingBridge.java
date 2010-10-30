package net.beaconcontroller.logging.bridge;

import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OSGIToSLF4JLoggingBridge {
    private static Logger log = LoggerFactory.getLogger(OSGIToSLF4JLoggingBridge.class);
    protected BeaconBundleListener bundleListener;
    protected BeaconFrameworkListener frameworkListener;
    protected LogListener logListener;

    /**
     * 
     */
    public OSGIToSLF4JLoggingBridge() {
        this.bundleListener = new BeaconBundleListener();
        this.frameworkListener = new BeaconFrameworkListener();
        this.logListener = new SLF4JLogListener();
    }
    

    public void startUp(BundleContext context) throws Exception {
        log.trace("StartUp");
        context.addBundleListener(this.bundleListener);
        context.addFrameworkListener(this.frameworkListener);
    }

    public void shutDown(BundleContext context) throws Exception {
        log.trace("ShutDown");
        context.removeBundleListener(this.bundleListener);
        context.removeFrameworkListener(this.frameworkListener);
    }

    public void addLogReaderService(LogReaderService service) {
        service.addLogListener(this.logListener);
    }

    public void removeLogReaderService(LogReaderService service) {
        service.removeLogListener(this.logListener);
    }

    public void addExtendedLogReaderService(ExtendedLogReaderService service) {
        service.addLogListener(this.logListener);
    }

    public void removeExtendedLogReaderService(ExtendedLogReaderService service) {
        service.removeLogListener(this.logListener);
    }
}
