package net.beaconcontroller.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OsgiApplicationContextHolder implements ApplicationContextAware {
    protected static Logger log = LoggerFactory.getLogger(OsgiApplicationContextHolder.class);
    protected static ApplicationContext applicationContext;
    protected static Object applicationContextLock = new Object();

    public OsgiApplicationContextHolder() {
    }

    public void setApplicationContext(ApplicationContext context)
            throws BeansException {
        synchronized (OsgiApplicationContextHolder.applicationContextLock) {
            OsgiApplicationContextHolder.applicationContext = context;
            OsgiApplicationContextHolder.applicationContextLock.notifyAll();
        }
    }

    public static ApplicationContext getApplicationContext(boolean block) {
        if (block) {
            synchronized (OsgiApplicationContextHolder.applicationContextLock) {
                if (OsgiApplicationContextHolder.applicationContext == null) {
                    try {
                        OsgiApplicationContextHolder.applicationContextLock.wait();
                    } catch (InterruptedException e) {
                        log.error("Interupted while waiting for ApplicationContext to be set", e);
                    }
                }
            }
        }
        return OsgiApplicationContextHolder.applicationContext;
    }
}
