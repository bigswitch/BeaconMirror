package net.beaconcontroller.topology.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.test.BeaconTestCase;
import net.beaconcontroller.topology.LinkTuple;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
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
        LinkTuple lt = new LinkTuple(1L, 2, 2L, 1);
        topology.addOrUpdateLink(lt);

        // check invariants hold
        assertNotNull(topology.switchLinks.get(lt.getSrc().getId()));
        assertTrue(topology.switchLinks.get(lt.getSrc().getId()).contains(lt));
        assertNotNull(topology.portLinks.get(lt.getSrc()));
        assertTrue(topology.portLinks.get(lt.getSrc()).contains(lt));
        assertNotNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.portLinks.get(lt.getDst()).contains(lt));
        assertTrue(topology.links.containsKey(lt));
    }

    @Test
    public void testDeleteLink() throws Exception {
        TopologyImpl topology = getTopology();
        LinkTuple lt = new LinkTuple(1L, 2, 2L, 1);
        topology.addOrUpdateLink(lt);
        topology.deleteLink(lt);

        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc().getId()));
        assertNull(topology.switchLinks.get(lt.getDst().getId()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }

    @Test
    public void testAddOrUpdateLinkToSelf() throws Exception {
        TopologyImpl topology = getTopology();
        LinkTuple lt = new LinkTuple(1L, 2, 1L, 3);
        topology.addOrUpdateLink(lt);

        // check invariants hold
        assertNotNull(topology.switchLinks.get(lt.getSrc().getId()));
        assertTrue(topology.switchLinks.get(lt.getSrc().getId()).contains(lt));
        assertNotNull(topology.portLinks.get(lt.getSrc()));
        assertTrue(topology.portLinks.get(lt.getSrc()).contains(lt));
        assertNotNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.portLinks.get(lt.getDst()).contains(lt));
        assertTrue(topology.links.containsKey(lt));
    }

    @Test
    public void testDeleteLinkToSelf() throws Exception {
        TopologyImpl topology = getTopology();
        LinkTuple lt = new LinkTuple(1L, 2, 1L, 3);
        topology.addOrUpdateLink(lt);
        topology.deleteLink(lt);

        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc().getId()));
        assertNull(topology.switchLinks.get(lt.getDst().getId()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }

    @Test
    public void testRemovedSwitch() {
        TopologyImpl topology = getTopology();
        LinkTuple lt = new LinkTuple(1L, 2, 2L, 1);
        topology.addOrUpdateLink(lt);

        // Mock up our expected behavior
        IOFSwitch mockSwitch = createMock(IOFSwitch.class);
        expect(mockSwitch.getId()).andReturn(1L).anyTimes();
        replay(mockSwitch);
        topology.removedSwitch(mockSwitch);

        verify(mockSwitch);
        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc().getId()));
        assertNull(topology.switchLinks.get(lt.getDst().getId()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }

    @Test
    public void testRemovedSwitchSelf() {
        TopologyImpl topology = getTopology();
        LinkTuple lt = new LinkTuple(1L, 2, 1L, 3);
        topology.addOrUpdateLink(lt);

        // Mock up our expected behavior
        IOFSwitch mockSwitch = createMock(IOFSwitch.class);
        expect(mockSwitch.getId()).andReturn(1L).anyTimes();
        replay(mockSwitch);
        topology.removedSwitch(mockSwitch);

        verify(mockSwitch);
        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc().getId()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }
}
