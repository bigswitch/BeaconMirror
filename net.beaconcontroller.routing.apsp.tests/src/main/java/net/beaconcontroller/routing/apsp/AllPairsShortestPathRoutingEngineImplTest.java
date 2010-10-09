package net.beaconcontroller.routing.apsp;

import static org.junit.Assert.*;

import org.junit.Test;

import net.beaconcontroller.routing.IRoutingEngine;
import net.beaconcontroller.routing.Route;
import net.beaconcontroller.test.BeaconTestCase;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class AllPairsShortestPathRoutingEngineImplTest extends BeaconTestCase {
    protected IRoutingEngine getRouting() {
        return (IRoutingEngine) getApplicationContext().getBean("routingEngine");
    }

    @Test
    public void testGetRoute() throws Exception {
        IRoutingEngine routingEngine = getRouting();
        assertNull(routingEngine.getRoute(1L, 2L));

        routingEngine.update(1L, 2, 2L, 1, true);
        // [1]2-1[2]
        assertEquals(new Route(1L, 2, 1, 2L
                ), routingEngine.getRoute(1L, 2L));

        routingEngine.update(2L, 2, 3L, 1, true);
        // [1]2-1[2] [2]2-1[3]
        assertEquals(new Route(1L,
                2, 1, 2L,
                2, 1, 3L
                ), routingEngine.getRoute(1L, 3L));

        routingEngine.update(1L, 3, 3L, 4, true);
        // [1]2-1[2] [2]2-1[3]
        // [1]3     -     4[3]
        assertEquals(new Route(1L,
                3, 4, 3L
                ), routingEngine.getRoute(1L, 3L));

        routingEngine.update(1L, 3, 3L, 4, false);
        // [1]2-1[2] [2]2-1[3]
        assertEquals(new Route(1L,
                2, 1, 2L,
                2, 1, 3L
                ), routingEngine.getRoute(1L, 3L));

        routingEngine.update(2L, 2, 3L, 1, false);
        // [1]2-1[2]
        assertEquals(new Route(1L,
                2, 1, 2L
                ), routingEngine.getRoute(1L, 2L));
    }
}
