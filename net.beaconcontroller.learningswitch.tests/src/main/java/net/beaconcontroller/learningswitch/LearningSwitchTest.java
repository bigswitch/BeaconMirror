package net.beaconcontroller.learningswitch;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.packet.Data;
import net.beaconcontroller.packet.Ethernet;
import net.beaconcontroller.packet.IPacket;
import net.beaconcontroller.packet.IPv4;
import net.beaconcontroller.packet.UDP;
import net.beaconcontroller.test.BeaconTestCase;
import net.beaconcontroller.test.MockBeaconProvider;

import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFPacketIn.OFPacketInReason;

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

        // Build our test packet
        IPacket packet = new Ethernet()
            .setDestinationMACAddress("00:11:22:33:44:55")
            .setSourceMACAddress("55:44:33:22:11:00")
            .setEtherType(Ethernet.TYPE_IPv4)
            .setPayload(
                new IPv4()
                .setTtl((byte) 128)
                .setSourceAddress("192.168.1.1")
                .setDestinationAddress("192.168.1.2")
                .setPayload(new UDP()
                            .setSourcePort((short) 5000)
                            .setDestinationPort((short) 5001)
                            .setPayload(new Data(new byte[] {0x01}))));
        byte[] serializedPacket = packet.serialize();

        // Mock up our expected behavior
        IOFSwitch mockSwitch = createMock(IOFSwitch.class);
        OFMessageAsyncStream mockStream = createMock(OFMessageAsyncStream.class);
        expect(mockSwitch.getOutputStream()).andReturn(mockStream);
        OFPacketOut packetOut = new OFPacketOut();
        mockStream.write(packetOut);

        // Start recording the replay on the mocks
        replay(mockSwitch, mockStream);
        // Get the listener and trigger the packet in
        IOFMessageListener listener = mockBeaconProvider.getListeners().get(
                OFType.PACKET_IN).get(0);
        listener.receive(mockSwitch, new OFPacketIn()
            .setBufferId(-1)
            .setInPort((short) 1)
            .setPacketData(serializedPacket)
            .setReason(OFPacketInReason.NO_MATCH)
            .setTotalLength((short) serializedPacket.length));

        // Verify the replay matched our expectations
        verify(mockSwitch, mockStream);
        // Verify the mack table inside the switch
        assertEquals(5, learningSwitch.getMacTables().get(mockSwitch).get(1)
                .shortValue());
    }
}
