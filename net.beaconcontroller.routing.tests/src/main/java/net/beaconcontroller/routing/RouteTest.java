package net.beaconcontroller.routing;

import junit.framework.TestCase;
import net.beaconcontroller.test.BeaconTestCase;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class RouteTest extends BeaconTestCase {
    public void testCloneable() throws Exception {
        Route r1 = new Route(1L, 2L);
        Route r2 = (Route) r1.clone();
        r2.getId().setDst(3L);
        TestCase.assertNotSame(r1, r2);
        TestCase.assertNotSame(r1.getId(), r2.getId());

        r1.getId().setDst(3L);
        r1.getPath().add(new Link((short)1, (short)1, 2L));
        r1.getPath().add(new Link((short)2, (short)1, 3L));
        r2 = (Route) r1.clone();
        TestCase.assertEquals(r1, r2);

        Link temp = r2.getPath().remove(0);
        TestCase.assertNotSame(r1, r2);

        r2.getPath().add(0, temp);
        TestCase.assertEquals(r1, r2);

        r2.getPath().get(0).setInPort((short) 5);
        TestCase.assertNotSame(r1, r2);
    }
}
