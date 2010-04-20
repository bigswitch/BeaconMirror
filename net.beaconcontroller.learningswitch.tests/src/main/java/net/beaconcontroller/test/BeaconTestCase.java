package net.beaconcontroller.test;

import org.springframework.context.ApplicationContext;

import junit.framework.TestCase;

public class BeaconTestCase extends TestCase {
    protected ApplicationContext applicationContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.applicationContext =
            OsgiApplicationContextHolder.getApplicationContext(true);
    }

    /**
     * @return the applicationContext
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
