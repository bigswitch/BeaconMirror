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

        routing.update(1L, (short)1, 2L, (short)1, true);
        TestCase.assertEquals(new Route(1L, new Integer(1).shortValue(), new Integer(1).shortValue(), 2L), routing.getRoute(1L, 2L));
    }
}
