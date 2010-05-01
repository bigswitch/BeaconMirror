package net.beaconcontroller.test;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class OsgiApplicationContextHolder implements ApplicationContextAware {
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
                        e.printStackTrace();
                    }
                }
            }
        }
        return OsgiApplicationContextHolder.applicationContext;
    }
}
