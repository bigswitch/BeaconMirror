package net.beaconcontroller.test;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.*;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class OsgiApplicationContextHolder implements ApplicationContextAware {
    protected static ApplicationContext applicationContext;

    public OsgiApplicationContextHolder() {
    }

    public void setApplicationContext(ApplicationContext context)
            throws BeansException {
        OsgiApplicationContextHolder.applicationContext = context;
    }

    public static ApplicationContext getApplicationContext() {
        return OsgiApplicationContextHolder.applicationContext;
    }
}
