package net.beaconcontroller.hub;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;

import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.packet.Data;
import net.beaconcontroller.packet.Ethernet;
import net.beaconcontroller.packet.IPacket;
import net.beaconcontroller.packet.IPv4;
import net.beaconcontroller.packet.UDP;
import net.beaconcontroller.test.BeaconTestCase;
import net.beaconcontroller.test.MockBeaconProvider;

import org.openflow.io.OFMessageInStream;
import org.openflow.io.OFMessageOutStream;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFPacketIn.OFPacketInReason;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.factory.BasicFactory;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class HubTest extends BeaconTestCase {
    protected OFPacketIn packetIn;
    protected IPacket testPacket;
    protected byte[] testPacketSerialized;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Build our test packet
        this.testPacket = new Ethernet()
            .setDestinationMACAddress("00:11:22:33:44:55")
            .setSourceMACAddress("00:44:33:22:11:00")
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
        this.testPacketSerialized = testPacket.serialize();

        // Build the PacketIn
        this.packetIn = new OFPacketIn()
            .setBufferId(-1)
            .setInPort((short) 1)
            .setPacketData(this.testPacketSerialized)
            .setReason(OFPacketInReason.NO_MATCH)
            .setTotalLength((short) this.testPacketSerialized.length);
    }

    protected MockBeaconProvider getMockBeaconProvider() {
        return (MockBeaconProvider) getApplicationContext().getBean("mockBeaconProvider");
    }

    public void testFloodNoBufferId() throws Exception {
        MockBeaconProvider mockBeaconProvider = getMockBeaconProvider();

        // build our expected flooded packetOut
        OFPacketOut po = new OFPacketOut()
            .setActions(Arrays.asList(new OFAction[] {new OFActionOutput().setPort(OFPort.OFPP_FLOOD.getValue())}))
            .setActionsLength((short) OFActionOutput.MINIMUM_LENGTH)
            .setBufferId(-1)
            .setInPort((short) 1)
            .setPacketData(this.testPacketSerialized);
        po.setLengthU(OFPacketOut.MINIMUM_LENGTH + po.getActionsLengthU()
                + this.testPacketSerialized.length);

        // Mock up our expected behavior
        IOFSwitch mockSwitch = createMock(IOFSwitch.class);
        OFMessageInStream mockInStream = createMock(OFMessageInStream.class);
        OFMessageOutStream mockStream = createMock(OFMessageOutStream.class);
        expect(mockSwitch.getInputStream()).andReturn(mockInStream);
        expect(mockInStream.getMessageFactory()).andReturn(new BasicFactory());
        expect(mockSwitch.getOutputStream()).andReturn(mockStream);
        mockStream.write(po);

        // Start recording the replay on the mocks
        replay(mockSwitch, mockStream, mockInStream);
        // Get the listener and trigger the packet in
        IOFMessageListener listener = mockBeaconProvider.getListeners().get(
                OFType.PACKET_IN).get(0);
        listener.receive(mockSwitch, this.packetIn);

        // Verify the replay matched our expectations
        verify(mockSwitch, mockStream, mockInStream);
    }

    public void testFloodBufferId() throws Exception {
        MockBeaconProvider mockBeaconProvider = getMockBeaconProvider();
        this.packetIn.setBufferId(10);

        // build our expected flooded packetOut
        OFPacketOut po = new OFPacketOut()
            .setActions(Arrays.asList(new OFAction[] {new OFActionOutput().setPort(OFPort.OFPP_FLOOD.getValue())}))
            .setActionsLength((short) OFActionOutput.MINIMUM_LENGTH)
            .setBufferId(10)
            .setInPort((short) 1);
        po.setLengthU(OFPacketOut.MINIMUM_LENGTH + po.getActionsLengthU());

        // Mock up our expected behavior
        IOFSwitch mockSwitch = createMock(IOFSwitch.class);
        OFMessageInStream mockInStream = createMock(OFMessageInStream.class);
        OFMessageOutStream mockStream = createMock(OFMessageOutStream.class);
        expect(mockSwitch.getInputStream()).andReturn(mockInStream);
        expect(mockInStream.getMessageFactory()).andReturn(new BasicFactory());
        expect(mockSwitch.getOutputStream()).andReturn(mockStream);
        mockStream.write(po);

        // Start recording the replay on the mocks
        replay(mockSwitch, mockStream, mockInStream);
        // Get the listener and trigger the packet in
        IOFMessageListener listener = mockBeaconProvider.getListeners().get(
                OFType.PACKET_IN).get(0);
        listener.receive(mockSwitch, this.packetIn);

        // Verify the replay matched our expectations
        verify(mockSwitch, mockStream, mockInStream);
    }
}
