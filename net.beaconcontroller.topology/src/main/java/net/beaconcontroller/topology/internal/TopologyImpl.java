package net.beaconcontroller.topology.internal;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFType;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFSwitchListener;
import net.beaconcontroller.packet.Ethernet;
import net.beaconcontroller.packet.LLDP;
import net.beaconcontroller.packet.LLDPTLV;
import net.beaconcontroller.topology.ITopology;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class TopologyImpl implements IOFMessageListener, IOFSwitchListener, ITopology {
    protected IBeaconProvider beaconProvider;
    protected Timer timer;

    protected void startUp() {
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
        beaconProvider.addOFSwitchListener(this);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendLLDPs();
            }}, 1000, 60*1000);
    }

    protected void shutDown() {
        timer.cancel();
        beaconProvider.removeOFSwitchListener(this);
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
    }

    protected void sendLLDPs() {
        Ethernet ethernet = new Ethernet();
        ethernet.setSourceMACAddress(new byte[6]);
        ethernet.setDestinationMACAddress("01:80:c2:00:00:0e");
        ethernet.setEtherType(Ethernet.TYPE_LLDP);

        LLDP lldp = new LLDP();
        lldp.setChassisId(new LLDPTLV().setType((byte) 1).setLength((short) 7));
        byte[] chassisId = new byte[7];
        chassisId[0] = 4;
        // set MAC here
        lldp.getChassisId().setValue(chassisId);

        lldp.setPortId(new LLDPTLV().setType((byte) 2).setLength((short) 3));
        byte[] portId = new byte[3];
        portId[0] = 2;
        // set port here
        lldp.getPortId().setValue(portId);

        lldp.setTtl(new LLDPTLV().setType((byte) 3).setLength((short) 2).setValue(new byte[] {0, 0x78}));

        LLDPTLV dpidTLV = new LLDPTLV().setType((byte) 0x127).setLength((short) 12);
        byte[] dpidTLVValue = new byte[12];
        // OpenFlow OUI - 00-26-E1
        dpidTLVValue[0] = 0x0;
        dpidTLVValue[1] = 0x26;
        dpidTLVValue[2] = (byte) 0xe1;
        dpidTLVValue[3] = 0x0;
        dpidTLV.setValue(dpidTLVValue);
        lldp.setOptionalTLVList(new ArrayList<LLDPTLV>());
        lldp.getOptionalTLVList().add(dpidTLV);
        
        Map<Long, IOFSwitch> switches = beaconProvider.getSwitches();
        byte[] dpidArray = new byte[8];
        ByteBuffer dpidBB = ByteBuffer.wrap(dpidArray);
        ByteBuffer portBB = ByteBuffer.wrap(portId, 1, 2);
        for (Entry<Long, IOFSwitch> entry : switches.entrySet()) {
            Long dpid = entry.getKey();
            IOFSwitch sw = entry.getValue();
            dpidBB.putLong(sw.getDatapathId());

            // set the ethernet source mac to last 6 bytes of dpid
            System.arraycopy(dpidArray, 2, ethernet.getSourceMACAddress(), 0, 6);
            // set the chassis id's value to last 6 bytes of dpid
            System.arraycopy(dpidArray, 2, chassisId, 1, 6);
            // set the optional tlv to the full dpid
            System.arraycopy(dpidArray, 0, dpidTLVValue, 4, 8);
            for (OFPhysicalPort port : sw.getFeaturesReply().getPorts()) {
                // set the portId to the outgoing port
                portBB.putShort(port.getPortNumber());

                // serialize and wrap in a packet out here

                // rewind for next pass
                portBB.position(1);
            }

            // rewind for next pass
            dpidBB.position(0);
        }
    }

    @Override
    public String getName() {
        return "topology";
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg) {
        OFPacketIn pi = (OFPacketIn) msg;
        return Command.STOP;
    }

    @Override
    public void addedSwitch(IOFSwitch sw) {
    }

    @Override
    public void removedSwitch(IOFSwitch sw) {
    }

    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }
}
