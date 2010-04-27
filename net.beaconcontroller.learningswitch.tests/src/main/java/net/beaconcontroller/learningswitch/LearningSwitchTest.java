package net.beaconcontroller.learningswitch;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.test.BeaconTestCase;
import net.beaconcontroller.test.MockBeaconProvider;

import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFType;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class LearningSwitchTest extends BeaconTestCase {
    public void testFlood() throws Exception {
        // Retrieve the services from Spring
        LearningSwitch learningSwitch = (LearningSwitch) getApplicationContext()
                .getBean("learningSwitch");
        MockBeaconProvider mockBeaconProvider = (MockBeaconProvider) getApplicationContext()
                .getBean("mockBeaconProvider");

        // Mock up our expected behavior
        IOFSwitch mockSwitch = createMock(IOFSwitch.class);
        OFMessageAsyncStream mockStream = createMock(OFMessageAsyncStream.class);
        expect(mockSwitch.getStream()).andReturn(mockStream);
        OFPacketOut packetOut = new OFPacketOut();
        mockStream.write(packetOut);

        // Start recording the replay on the mocks
        replay(mockSwitch, mockStream);
        // Get the listener and trigger the packet in
        IOFMessageListener listener = mockBeaconProvider.getListeners().get(
                OFType.PACKET_IN).get(0);
        listener.receive(mockSwitch, new OFPacketIn());

        // Verify the replay matched our expectations
        verify(mockSwitch, mockStream);
        // Verify the mack table inside the switch
        assertEquals(5, learningSwitch.getMacTables().get(mockSwitch).get(1)
                .shortValue());
    }
}
