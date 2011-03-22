package net.beaconcontroller.topology.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFPhysicalPort;

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

    public IOFSwitch createMockSwitch(Long id) {
        IOFSwitch mockSwitch = createMock(IOFSwitch.class);
        expect(mockSwitch.getId()).andReturn(id).anyTimes();
        return mockSwitch;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        TopologyImpl topology = getTopology();
        topology.links.clear();
        topology.portLinks.clear();
        topology.switchLinks.clear();
        if (topology.switchClusterMap != null)
            topology.switchClusterMap.clear();
    }

    @Test
    public void testAddOrUpdateLink() throws Exception {
        TopologyImpl topology = getTopology();
        IOFSwitch sw1 = createMockSwitch(1L);
        IOFSwitch sw2 = createMockSwitch(2L);
        replay(sw1, sw2);
        LinkTuple lt = new LinkTuple(sw1, 2, sw2, 1);
        topology.addOrUpdateLink(lt, 0, 0);

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
        topology.addOrUpdateLink(lt, 0, 0);
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
        topology.addOrUpdateLink(lt, 0, 0);

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
        topology.addOrUpdateLink(lt, 0, 0);
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
        topology.addOrUpdateLink(lt, 0, 0);

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
        topology.addOrUpdateLink(lt, 0, 0);

        // Mock up our expected behavior
        topology.removedSwitch(sw1);

        verify(sw1);
        // check invariants hold
        assertNull(topology.switchLinks.get(lt.getSrc().getSw()));
        assertNull(topology.portLinks.get(lt.getSrc()));
        assertNull(topology.portLinks.get(lt.getDst()));
        assertTrue(topology.links.isEmpty());
    }
    
    private void createLinks(TopologyImpl topology, IOFSwitch[] switches, int[][] linkInfoArray) {
        for (int i = 0; i < linkInfoArray.length; i++) {
            int[] linkInfo = linkInfoArray[i];
            LinkTuple lt = new LinkTuple(switches[linkInfo[0]-1], linkInfo[1], switches[linkInfo[3]-1], linkInfo[4]);
            topology.addOrUpdateLink(lt, linkInfo[2], linkInfo[5]);
        }
    }
    
    private void verifyClusters(TopologyImpl topology, IOFSwitch[] switches, int[][] clusters) {
        // Keep track of which switches we've already checked for cluster membership
        List<IOFSwitch> verifiedSwitches = new ArrayList<IOFSwitch>();
        
        // Make sure the expected cluster arrays are sorted so we can
        // use binarySearch to test for membership
        for (int i = 0; i < clusters.length; i++)
            Arrays.sort(clusters[i]);
        
        for (int i = 0; i < switches.length; i++) {
            IOFSwitch sw = switches[i];
            if (!verifiedSwitches.contains(sw)) {
                long id = sw.getId();
                int[] expectedCluster = null;
                
                for (int j = 0; j < clusters.length; j++) {
                    if (Arrays.binarySearch(clusters[j], (int)id) >= 0) {
                        expectedCluster = clusters[j];
                        break;
                    }
                }
                if (expectedCluster != null) {
                    Set<IOFSwitch> cluster = topology.getSwitchesInCluster(sw);
                    assertEquals(expectedCluster.length, cluster.size());
                    for (IOFSwitch sw2: cluster) {
                        long id2 = sw2.getId();
                        assertTrue(Arrays.binarySearch(expectedCluster, (int)id2) >= 0);
                        verifiedSwitches.add(sw2);
                    }
                }
            }
        }
    }
    
    @Test
    public void testCluster() {
        
        //      +-------+             +-------+
        //      |       |             |       |
        //      |   1  1|-------------|1  2   |
        //      |   2   |             |2  3  4|
        //      +-------+       +-----+-------+
        //          |           |         |   |
        //          |           |         |   |
        //      +-------+-------+         |   |
        //      |   1  2|                 |   |
        //      |   3   |                 |   |
        //      |   3   |                 |   |
        //      +-------+                 |   |
        //          |                     |   |
        //          |                     |   |
        //      +-------+-----------------+   |             
        //      |   1  2|                     |
        //      |   4   |                     |
        //      |   3   |      +--------------+
        //      +-------+      |
        //          |          |         
        //          |          |         
        //      +-------+------+      +-------+
        //      |   1  2|             |       |
        //      |   5  3|-------------|1  6   |
        //      |       |             |       |
        //      +-------+             +-------+
        
        TopologyImpl topology = getTopology();
        
        // Create several switches
        IOFSwitch[] switches = new IOFSwitch[6];
        for (int i = 0; i < 6; i++) {
            switches[i] = createMockSwitch((long)i+1);
            replay(switches[i]);
        }

        // Create links among the switches
        int linkInfoArray1[][] = {
                // SrcSw#, SrcPort#, SrcPortState, DstSw#, DstPort#, DstPortState
                { 1, 1, 0, 2, 1, 0},
                { 2, 1, 0, 1, 1, 0},
                { 1, 2, 0, 3, 1, 0},
                { 3, 1, 0, 1, 2, 0},
                { 2, 2, 0, 3, 2, 0},
                { 3, 2, 0, 2, 2, 0},
                { 2, 3, 0, 4, 2, 0},
                { 4, 2, 0, 2, 3, 0},
                { 3, 3, 0, 4, 1, 0},
                { 4, 1, 0, 3, 3, 0},
                { 5, 3, 0, 6, 1, 0},
                { 6, 1, 0, 5, 3, 0},
        };
        createLinks(topology, switches, linkInfoArray1);
        
        int expectedClusters1[][] = {
                {1,2,3,4},
                {5,6}
        };
        verifyClusters(topology, switches, expectedClusters1);
        
        int linkInfoArray2[][] = {
            { 4, 3, 0, 5, 1, 0},
            { 5, 1, 0, 4, 3, 0},
            { 2, 4, 0, 5, 2, 0},
            { 5, 2, 0, 2, 4, 0},
        };
        createLinks(topology, switches, linkInfoArray2);
        int expectedClusters2[][] = {
                {1,2,3,4,5,6},
        };
        verifyClusters(topology, switches, expectedClusters2);
        
        OFPortStatus portStatus = new OFPortStatus();
        portStatus.setReason((byte)OFPortStatus.OFPortReason.OFPPR_MODIFY.ordinal());
        OFPhysicalPort physicalPort = new OFPhysicalPort();
        physicalPort.setPortNumber((short)3);
        physicalPort.setConfig(0);
        physicalPort.setState(OFPhysicalPort.OFPortState.OFPPS_STP_BLOCK.getValue());
        portStatus.setDesc(physicalPort);
        topology.handlePortStatus(switches[4], portStatus);
        
        int expectedClusters3[][] = {
                {1,2,3,4,5},
                {6}
        };
        verifyClusters(topology, switches, expectedClusters3);
        
        physicalPort.setState(OFPhysicalPort.OFPortState.OFPPS_STP_FORWARD.getValue());
        topology.handlePortStatus(switches[4], portStatus);
        verifyClusters(topology, switches, expectedClusters2);
        
        topology.removedSwitch(switches[3]);
        int expectedClusters4[][] = {
                {1,2,3,5,6}
        };
        verifyClusters(topology, switches, expectedClusters4);
        
        portStatus.setReason((byte)OFPortStatus.OFPortReason.OFPPR_DELETE.ordinal());
        physicalPort.setPortNumber((short)4);
        topology.handlePortStatus(switches[1], portStatus);
        physicalPort.setPortNumber((short)2);
        topology.handlePortStatus(switches[4], portStatus);

        int expectedClusters5[][] = {
                {1,2,3},
                {5,6}
        };
        verifyClusters(topology, switches, expectedClusters5);
    }
}
