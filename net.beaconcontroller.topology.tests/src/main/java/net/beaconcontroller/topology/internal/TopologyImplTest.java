package net.beaconcontroller.topology.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import junit.framework.TestCase;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.io.OFMessageSafeOutStream;
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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getTopology().links.clear();
        getTopology().portLinks.clear();
        getTopology().switchLinks.clear();
    }

    public void testAddOrUpdateLink() throws Exception {
        TopologyImpl topology = getTopology();
        LinkTuple lt = new LinkTuple(1L, 2, 2L, 1);
        topology.addOrUpdateLink(lt);

        // check invariants hold
        TestCase.assertNotNull(topology.switchLinks.get(lt.getSrc().getId()));
        TestCase.assertTrue(topology.switchLinks.get(lt.getSrc().getId()).contains(lt));
        TestCase.assertNotNull(topology.portLinks.get(lt.getSrc()));
        TestCase.assertTrue(topology.portLinks.get(lt.getSrc()).contains(lt));
        TestCase.assertNotNull(topology.portLinks.get(lt.getDst()));
        TestCase.assertTrue(topology.portLinks.get(lt.getDst()).contains(lt));
        TestCase.assertTrue(topology.links.containsKey(lt));
    }

    public void testDeleteLink() throws Exception {
        TopologyImpl topology = getTopology();
        LinkTuple lt = new LinkTuple(1L, 2, 2L, 1);
        topology.addOrUpdateLink(lt);
        topology.deleteLink(lt);

        // check invariants hold
        TestCase.assertNull(topology.switchLinks.get(lt.getSrc().getId()));
        TestCase.assertNull(topology.switchLinks.get(lt.getDst().getId()));
        TestCase.assertNull(topology.portLinks.get(lt.getSrc()));
        TestCase.assertNull(topology.portLinks.get(lt.getDst()));
        TestCase.assertTrue(topology.links.isEmpty());
    }

    public void testAddOrUpdateLinkToSelf() throws Exception {
        TopologyImpl topology = getTopology();
        LinkTuple lt = new LinkTuple(1L, 2, 1L, 3);
        topology.addOrUpdateLink(lt);

        // check invariants hold
        TestCase.assertNotNull(topology.switchLinks.get(lt.getSrc().getId()));
        TestCase.assertTrue(topology.switchLinks.get(lt.getSrc().getId()).contains(lt));
        TestCase.assertNotNull(topology.portLinks.get(lt.getSrc()));
        TestCase.assertTrue(topology.portLinks.get(lt.getSrc()).contains(lt));
        TestCase.assertNotNull(topology.portLinks.get(lt.getDst()));
        TestCase.assertTrue(topology.portLinks.get(lt.getDst()).contains(lt));
        TestCase.assertTrue(topology.links.containsKey(lt));
    }

    public void testDeleteLinkToSelf() throws Exception {
        TopologyImpl topology = getTopology();
        LinkTuple lt = new LinkTuple(1L, 2, 1L, 3);
        topology.addOrUpdateLink(lt);
        topology.deleteLink(lt);

        // check invariants hold
        TestCase.assertNull(topology.switchLinks.get(lt.getSrc().getId()));
        TestCase.assertNull(topology.switchLinks.get(lt.getDst().getId()));
        TestCase.assertNull(topology.portLinks.get(lt.getSrc()));
        TestCase.assertNull(topology.portLinks.get(lt.getDst()));
        TestCase.assertTrue(topology.links.isEmpty());
    }

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
        TestCase.assertNull(topology.switchLinks.get(lt.getSrc().getId()));
        TestCase.assertNull(topology.switchLinks.get(lt.getDst().getId()));
        TestCase.assertNull(topology.portLinks.get(lt.getSrc()));
        TestCase.assertNull(topology.portLinks.get(lt.getDst()));
        TestCase.assertTrue(topology.links.isEmpty());
    }

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
        TestCase.assertNull(topology.switchLinks.get(lt.getSrc().getId()));
        TestCase.assertNull(topology.portLinks.get(lt.getSrc()));
        TestCase.assertNull(topology.portLinks.get(lt.getDst()));
        TestCase.assertTrue(topology.links.isEmpty());
    }
}
