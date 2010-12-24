package net.beaconcontroller.routing.internal;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.io.OFMessageSafeOutStream;
import net.beaconcontroller.core.test.MockBeaconProvider;
import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.packet.Data;
import net.beaconcontroller.packet.Ethernet;
import net.beaconcontroller.packet.IPacket;
import net.beaconcontroller.packet.IPv4;
import net.beaconcontroller.packet.UDP;
import net.beaconcontroller.routing.IRoutingEngine;
import net.beaconcontroller.routing.Link;
import net.beaconcontroller.routing.Route;
import net.beaconcontroller.routing.internal.Routing;
import net.beaconcontroller.test.BeaconTestCase;

import org.junit.Before;
import org.junit.Test;
import org.openflow.io.OFMessageInStream;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFPacketIn.OFPacketInReason;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.factory.BasicFactory;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class RoutingTest extends BeaconTestCase {
    protected OFPacketIn packetIn;
    protected IPacket testPacket;
    protected byte[] testPacketSerialized;

    @Before
    public void setUp() throws Exception {
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

    protected Routing getRouting() {
        return (Routing) getApplicationContext().getBean("routing");
    }

    @Test
    public void testRouting() throws Exception {
        MockBeaconProvider mockBeaconProvider = getMockBeaconProvider();
        Routing routing = getRouting();
        byte[] dataLayerSource = ((Ethernet)this.testPacket).getSourceMACAddress();

        // Create mock switches
        IOFSwitch sw1 = createMock(IOFSwitch.class);
        OFMessageInStream mockIn = createMock(OFMessageInStream.class);
        OFMessageSafeOutStream out1 = createMock(OFMessageSafeOutStream.class);
        IOFSwitch sw2 = createMock(IOFSwitch.class);
        expect(sw2.getId()).andReturn(2L).anyTimes();
        OFMessageSafeOutStream out2 = createMock(OFMessageSafeOutStream.class);

        // build our expected Device
        Device dstDevice = new Device();
        dstDevice.setDataLayerAddress(dataLayerSource);
        dstDevice.setSw(sw2);
        dstDevice.setSwPort((short)3);

        // Mock deviceManager
        IDeviceManager deviceManager = createMock(IDeviceManager.class);
        expect(deviceManager.getDeviceByDataLayerAddress(aryEq(Ethernet.toMACAddress("00:11:22:33:44:55")))).andReturn(dstDevice).atLeastOnce();

        // Mock route
        Route route = new Route(1L, 2L);
        route.setPath(new ArrayList<Link>());
        route.getPath().add(new Link((short)2, (short)1, 2L));
        IRoutingEngine routingEngine = createMock(IRoutingEngine.class);
        expect(routingEngine.getRoute(1L, 2L)).andReturn(route).atLeastOnce();

        // Flow mods
        OFMatch match = new OFMatch();
        match.loadFromPacket(this.testPacketSerialized, (short) 1);

        OFFlowMod fm1 = new OFFlowMod();
        OFActionOutput action = new OFActionOutput((short)2, (short)0);
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(action);
        fm1.setIdleTimeout((short)5)
            .setMatch(match)
            .setActions(actions)
            .setBufferId(-1)
            .setLengthU(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH);

        OFFlowMod fm2 = fm1.clone();
        ((OFActionOutput)fm2.getActions().get(0)).setPort((short) 3);

        // Packet out
        OFPacketOut po = new OFPacketOut();
        po.setBufferId(this.packetIn.getBufferId())
            .setInPort(this.packetIn.getInPort());
        actions = new ArrayList<OFAction>();
        actions.add(new OFActionOutput(OFPort.OFPP_TABLE.getValue(), (short) 0));
        po.setActions(actions)
            .setActionsLength((short) OFActionOutput.MINIMUM_LENGTH)
            .setPacketData(this.testPacketSerialized)
            .setLengthU(OFPacketOut.MINIMUM_LENGTH+po.getActionsLength()+this.testPacketSerialized.length);

        // Load the switch map
        Map<Long, IOFSwitch> switches = new HashMap<Long, IOFSwitch>();
        switches.put(1L, sw1);
        switches.put(2L, sw2);
        mockBeaconProvider.setSwitches(switches);

        // Expected behavior
        expect(sw1.getId()).andReturn(1L).atLeastOnce();
        expect(sw1.getInputStream()).andReturn(mockIn).atLeastOnce();
        expect(mockIn.getMessageFactory()).andReturn(new BasicFactory()).atLeastOnce();
        expect(sw1.getOutputStream()).andReturn(out1).atLeastOnce();
        expect(sw2.getOutputStream()).andReturn(out2).atLeastOnce();
        out1.write(fm1);
        out1.write(po);
        out2.write(fm2);

        // Load routing with our mock objects and begin the replay
        routing.setDeviceManager(deviceManager);
        routing.setRoutingEngine(routingEngine);
        replay(sw1, sw2, mockIn, deviceManager, routingEngine, out1, out2);

        // trigger the packet in
        routing.receive(sw1, this.packetIn);

        // Verify the replay matched our expectations
        verify(sw1, sw2, mockIn, deviceManager, routingEngine, out1, out2);
    }
}
