package net.beaconcontroller.test;

import org.junit.Before;
import org.springframework.context.ApplicationContext;

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
