package net.beaconcontroller.topology.internal;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.test.BeaconTestCase;
import net.beaconcontroller.topology.LinkTuple;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class TopologyImplTest extends BeaconTestCase {
    public TopologyImpl getTopology() {
        return (TopologyImpl) getApplicationContext().getBean("topology");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        getTopology().links.clear();
        getTopology().portLinks.clear();
        getTopology().switchLinks.clear();
    }

    @Test
    public void testAddOrUpdateLink() throws Exception {
        TopologyImpl topology = getTopology();
        IOFSwitch sw1 = createMockSwitch(1L);
        IOFSwitch sw2 = createMockSwitch(2L);
        replay(sw1, sw2);
        LinkTuple lt = new LinkTuple(sw1, 2, sw2, 1);
        topology.addOrUpdateLink(lt);

        // check invariants hold
        assertNotNull(topology.switchLinks.get(lt.getSrc().getSw()));
        assertTrue(topology.switchLinks.get(lt.getSrc().getSw()).contains(lt));
        assertNotNull(topology.portLinks.get(lt.getSrc()));
        assertTrue(topology.portLinks.get(lt.getSrc()).contains(lt));
        assertNotNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.portLinks.get(lt.getDst()).contains(lt));
        assertTrue(topology.links.containsKey(lt));
    }

    @Test
    public void testDeleteLink() throws Exception {
        TopologyImpl topology = getTopology();
        IOFSwitch sw1 = createMockSwitch(1L);
        IOFSwitch sw2 = createMockSwitch(2L);
        replay(sw1, sw2);
        LinkTuple lt = new LinkTuple(sw1, 2, sw2, 1);
        topology.addOrUpdateLink(lt);
        topology.deleteLinks(Collections.singletonList(lt));

        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc().getSw()));
        assertNull(topology.switchLinks.get(lt.getDst().getSw()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }

    @Test
    public void testAddOrUpdateLinkToSelf() throws Exception {
        TopologyImpl topology = getTopology();
        IOFSwitch sw1 = createMockSwitch(1L);
        IOFSwitch sw2 = createMockSwitch(2L);
        replay(sw1, sw2);
        LinkTuple lt = new LinkTuple(sw1, 2, sw1, 3);
        topology.addOrUpdateLink(lt);

        // check invariants hold
        assertNotNull(topology.switchLinks.get(lt.getSrc().getSw()));
        assertTrue(topology.switchLinks.get(lt.getSrc().getSw()).contains(lt));
        assertNotNull(topology.portLinks.get(lt.getSrc()));
        assertTrue(topology.portLinks.get(lt.getSrc()).contains(lt));
        assertNotNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.portLinks.get(lt.getDst()).contains(lt));
        assertTrue(topology.links.containsKey(lt));
    }

    @Test
    public void testDeleteLinkToSelf() throws Exception {
        TopologyImpl topology = getTopology();
        IOFSwitch sw1 = createMockSwitch(1L);
        replay(sw1);
        LinkTuple lt = new LinkTuple(sw1, 2, sw1, 3);
        topology.addOrUpdateLink(lt);
        topology.deleteLinks(Collections.singletonList(lt));

        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc().getSw()));
        assertNull(topology.switchLinks.get(lt.getDst().getSw()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }

    @Test
    public void testRemovedSwitch() {
        TopologyImpl topology = getTopology();
        IOFSwitch sw1 = createMockSwitch(1L);
        IOFSwitch sw2 = createMockSwitch(2L);
        replay(sw1, sw2);
        LinkTuple lt = new LinkTuple(sw1, 2, sw2, 1);
        topology.addOrUpdateLink(lt);

        // Mock up our expected behavior
        topology.removedSwitch(sw1);

        verify(sw1, sw2);
        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc().getSw()));
        assertNull(topology.switchLinks.get(lt.getDst().getSw()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }

    @Test
    public void testRemovedSwitchSelf() {
        TopologyImpl topology = getTopology();
        IOFSwitch sw1 = createMockSwitch(1L);
        replay(sw1);
        LinkTuple lt = new LinkTuple(sw1, 2, sw1, 3);
        topology.addOrUpdateLink(lt);

        // Mock up our expected behavior
        topology.removedSwitch(sw1);

        verify(sw1);
        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc().getSw()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }
}
