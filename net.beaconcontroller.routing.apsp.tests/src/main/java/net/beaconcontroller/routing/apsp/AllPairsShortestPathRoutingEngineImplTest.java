package net.beaconcontroller.routing.apsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import net.beaconcontroller.routing.IRoutingEngine;
import net.beaconcontroller.routing.Route;
import net.beaconcontroller.test.BeaconTestCase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openflow.util.HexString;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class AllPairsShortestPathRoutingEngineImplTest extends BeaconTestCase {
    protected IRoutingEngine getRouting() {
        return (IRoutingEngine) getApplicationContext().getBean("routingEngine");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        getRouting().clear();
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

    @Test
    public void testGetRouteFive() throws Exception {
        IRoutingEngine routingEngine = getRouting();
        assertNull(routingEngine.getRoute(1L, 2L));

        routingEngine.update(HexString.toLong("00:00:00:00:00:00:02:01"), 52,
                HexString.toLong("00:00:00:00:00:00:01:01"), 49, true);
        routingEngine.update(HexString.toLong("00:00:00:00:00:00:01:01"), 49,
                HexString.toLong("00:00:00:00:00:00:02:01"), 52, true);
        routingEngine.update(HexString.toLong("00:00:00:00:00:00:02:01"), 50,
                HexString.toLong("00:00:00:00:00:00:00:01"), 49, true);
        routingEngine.update(HexString.toLong("00:00:00:00:00:00:00:01"), 49,
                HexString.toLong("00:00:00:00:00:00:02:01"), 50, true);
        routingEngine.update(HexString.toLong("00:00:00:00:00:00:00:01"), 3,
                HexString.toLong("00:00:d6:92:09:d3:f4:a4"), 1, true);
        routingEngine.update(HexString.toLong("00:00:d6:92:09:d3:f4:a4"), 1,
                HexString.toLong("00:00:00:00:00:00:00:01"), 3, true);

        List<Long> ids = new ArrayList<Long>();
        ids.add(HexString.toLong("00:00:00:00:00:00:00:01"));
        ids.add(HexString.toLong("00:00:00:00:00:00:01:01"));
        ids.add(HexString.toLong("00:00:00:00:00:00:02:01"));
        ids.add(HexString.toLong("00:00:d6:92:09:d3:f4:a4"));

        // ensure full matrix connectivity
        for (Long id : ids) {
            for (Long id2 : ids) {
                Route route = routingEngine.getRoute(id, id2);
                if (route ==  null)
                    Assert.fail("Missing route from "
                            + HexString.toHexString(id) + " to "
                            + HexString.toHexString(id2));
            }
        }
    }
}
