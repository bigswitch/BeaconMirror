package net.beaconcontroller.learningswitch;

import net.beaconcontroller.test.BeaconTestCase;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class LearningSwitchTest extends BeaconTestCase {
    public void testOsgiPlatformStarts() throws Exception {
        System.out.println("Running!!!!");
        LearningSwitch learningSwitch = (LearningSwitch) getApplicationContext().getBean("learningSwitch");
        System.out.println(learningSwitch.getClass().getCanonicalName());
    }
}
