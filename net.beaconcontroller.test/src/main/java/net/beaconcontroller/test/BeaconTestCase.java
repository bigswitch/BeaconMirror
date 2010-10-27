package net.beaconcontroller.test;

import org.junit.Before;
import org.springframework.context.ApplicationContext;

/**
 * This class gets a handle on the application context which is used to
 * retrieve Spring beans from during tests
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class BeaconTestCase {
    protected ApplicationContext applicationContext;

    @Before
    public void setUp() throws Exception {
        this.applicationContext =
            OsgiApplicationContextHolder.getApplicationContext(true);
    }

    /**
     * @return the applicationContext
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void testSanity() {
    }
}
