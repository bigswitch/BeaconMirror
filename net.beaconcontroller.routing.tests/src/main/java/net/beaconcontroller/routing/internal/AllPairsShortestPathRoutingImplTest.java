package net.beaconcontroller.routing.internal;

import junit.framework.TestCase;
import net.beaconcontroller.routing.IRouting;
import net.beaconcontroller.routing.Route;
import net.beaconcontroller.test.BeaconTestCase;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class AllPairsShortestPathRoutingImplTest extends BeaconTestCase {
    protected IRouting getRouting() {
        return (IRouting) getApplicationContext().getBean("routing");
    }

    public void testGetRoute() throws Exception {
        IRouting routing = getRouting();
        TestCase.assertNull(routing.getRoute(1L, 2L));

        routing.update(1L, 2, 2L, 1, true);
        // [1]2-1[2]
        TestCase.assertEquals(new Route(1L, 2, 1, 2L
                ), routing.getRoute(1L, 2L));

        routing.update(2L, 2, 3L, 1, true);
        // [1]2-1[2] [2]2-1[3]
        TestCase.assertEquals(new Route(1L, 
                2, 1, 2L,
                2, 1, 3L
                ), routing.getRoute(1L, 3L));

        routing.update(1L, 3, 3L, 4, true);
        // [1]2-1[2] [2]2-1[3]
        // [1]3     -     4[3]
        TestCase.assertEquals(new Route(1L, 
                3, 4, 3L
                ), routing.getRoute(1L, 3L));

        routing.update(1L, 3, 3L, 4, false);
        // [1]2-1[2] [2]2-1[3]
        TestCase.assertEquals(new Route(1L, 
                2, 1, 2L,
                2, 1, 3L
                ), routing.getRoute(1L, 3L));

        routing.update(2L, 2, 3L, 1, false);
        // [1]2-1[2]
        TestCase.assertEquals(new Route(1L,
                2, 1, 2L
                ), routing.getRoute(1L, 2L));
    }
}
