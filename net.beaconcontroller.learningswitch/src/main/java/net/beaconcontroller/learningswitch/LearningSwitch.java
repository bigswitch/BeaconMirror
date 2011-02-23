/**
 * Beacon
 * A BSD licensed, Java based OpenFlow controller
 *
 * Beacon is a Java based OpenFlow controller originally written by David Erickson at Stanford
 * University. It is available under the BSD license.
 *
 * For documentation, forums, issue tracking and more visit:
 *
 * http://www.openflowhub.org/display/Beacon/Beacon+Home
 **/

package net.beaconcontroller.learningswitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFSwitchListener;
import net.beaconcontroller.packet.Ethernet;

import org.openflow.protocol.OFError;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LearningSwitch implements IOFMessageListener, IOFSwitchListener {
    protected static Logger log = LoggerFactory.getLogger(LearningSwitch.class);
    protected IBeaconProvider beaconProvider;

    // flow-mod - for use in the cookie
    public static final int LEARNING_SWITCH_APP_ID = 1;
    // LOOK! This should probably go in some class that encapsulates
    // the app cookie management
    public static final int APP_ID_BITS = 12;
    public static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
    public static final long LEARNING_SWITCH_COOKIE = (long) (LEARNING_SWITCH_APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;
    
    // more flow-mod defaults 
    protected static final short IDLE_TIMEOUT_DEFAULT = 5;
    protected static final short HARD_TIMEOUT_DEFAULT = 0;
    protected static final short PRIORITY_DEFAULT = 100;
    
    // for managing our map sizes
    protected static final int MAX_MACS_PER_SWITCH  = 1000;

    class MacVlanPair {
        public Long mac;
        public Short vlan;
        public MacVlanPair(Long mac, Short vlan) {
            this.mac = mac;
            this.vlan = vlan;
        }
        public boolean equals(Object o) {
            return (o instanceof MacVlanPair) && (mac.equals(((MacVlanPair) o).mac))
                && (vlan.equals(((MacVlanPair) o).vlan));
        }
        public int hashCode() {
            return mac.hashCode() ^ vlan.hashCode();
        }
    }
    
    // for lookup of the port based on mac
    protected Map<Integer,Map<MacVlanPair,Short>> macVlanToPortMaps;
    
    public LearningSwitch() {
        this.macVlanToPortMaps = new HashMap<Integer,Map<MacVlanPair,Short>>();
    }
    
    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }
    
    public void startUp() {
        log.trace("Starting");
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
        //beaconProvider.addOFMessageListener(OFType.PORT_STATUS, this);
        beaconProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
        beaconProvider.addOFMessageListener(OFType.ERROR, this);
        beaconProvider.addOFSwitchListener(this);
    }

    public void shutDown() {
        log.trace("Stopping");
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
        //beaconProvider.removeOFMessageListener(OFType.PORT_STATUS, this);
        beaconProvider.removeOFMessageListener(OFType.FLOW_REMOVED, this);
        beaconProvider.removeOFMessageListener(OFType.ERROR, this);
        beaconProvider.removeOFSwitchListener(this);
    }

    public String getName() {
        return "switch";
    }

    private Map<MacVlanPair,Short> getPortMap(IOFSwitch sw) {
        int switchHash = sw.hashCode();
        Map<MacVlanPair,Short> macToPortMap = this.macVlanToPortMaps.get(switchHash);
        if (macToPortMap == null) {
            macToPortMap = new org.openflow.util.LRULinkedHashMap<MacVlanPair,Short>(
                    LearningSwitch.MAX_MACS_PER_SWITCH);
            this.macVlanToPortMaps.put(switchHash, macToPortMap);
        }
        return macToPortMap;
    }

    protected void addToPortMap(IOFSwitch sw, Long mac, Short vlan, short portVal) {
        if (vlan == (short) 0xffff) {
            // OFMatch.loadFromPacket sets VLAN ID to 0xffff if the packet contains no VLAN tag;
            // for our purposes that is equivalent to the default VLAN ID 0
            vlan = 0;
        }
        Map<MacVlanPair,Short> macToPortMap = this.getPortMap(sw);
        if (macToPortMap != null) {
            macToPortMap.put(new MacVlanPair(mac, vlan), portVal);
        } else {
            log.error("Whoa - we should have macToPortMap for the switch");
        }
    }
    
    protected void removeFromPortMap(IOFSwitch sw, Long mac, Short vlan) {
        if (vlan == (short) 0xffff) {
            vlan = 0;
        }
        Map<MacVlanPair,Short> macToPortMap = this.getPortMap(sw);
        if (macToPortMap != null) {
            macToPortMap.remove(new MacVlanPair(mac, vlan));
        } else {
            log.error("Whoa - we should have macToPortMap for the switch");
        }
    }

    public Short getFromPortMap(IOFSwitch sw, Long mac, Short vlan) {
        if (vlan == (short) 0xffff) {
            vlan = 0;
        }
        return getPortMap(sw).get(new MacVlanPair(mac, vlan));
    }

    private void writeFlowMod(IOFSwitch sw, short command, int bufferId,
            OFMatch match, short outPort) {
        // from openflow 1.0 spec - need to set these on a struct ofp_flow_mod:
        // struct ofp_flow_mod {
        //    struct ofp_header header;
        //    struct ofp_match match; /* Fields to match */
        //    uint64_t cookie; /* Opaque controller-issued identifier. */
        //
        //    /* Flow actions. */
        //    uint16_t command; /* One of OFPFC_*. */
        //    uint16_t idle_timeout; /* Idle time before discarding (seconds). */
        //    uint16_t hard_timeout; /* Max time before discarding (seconds). */
        //    uint16_t priority; /* Priority level of flow entry. */
        //    uint32_t buffer_id; /* Buffered packet to apply to (or -1).
        //                           Not meaningful for OFPFC_DELETE*. */
        //    uint16_t out_port; /* For OFPFC_DELETE* commands, require
        //                          matching entries to include this as an
        //                          output port. A value of OFPP_NONE
        //                          indicates no restriction. */
        //    uint16_t flags; /* One of OFPFF_*. */
        //    struct ofp_action_header actions[0]; /* The action length is inferred
        //                                            from the length field in the
        //                                            header. */
        //    };
           
        OFFlowMod flowMod = new OFFlowMod();
        flowMod.setMatch(match);
        flowMod.setCookie(LearningSwitch.LEARNING_SWITCH_COOKIE);
        flowMod.setCommand(command);
        flowMod.setIdleTimeout(LearningSwitch.IDLE_TIMEOUT_DEFAULT);
        flowMod.setHardTimeout(LearningSwitch.HARD_TIMEOUT_DEFAULT);
        flowMod.setPriority(LearningSwitch.PRIORITY_DEFAULT);
        flowMod.setBufferId(bufferId);
        flowMod.setOutPort((command == OFFlowMod.OFPFC_DELETE) ? outPort : OFPort.OFPP_NONE.getValue());
        flowMod.setFlags((command == OFFlowMod.OFPFC_DELETE) ? 0 : (short) (1 << 0)); // OFPFF_SEND_FLOW_REM

        // set the ofp_action_header/out actions:
        // from the openflow 1.0 spec: need to set these on a struct ofp_action_output:
        // uint16_t type; /* OFPAT_OUTPUT. */
        // uint16_t len; /* Length is 8. */
        // uint16_t port; /* Output port. */
        // uint16_t max_len; /* Max length to send to controller. */
        // type/len are set because it is OFActionOutput,
        // and port, max_len are arguments to this constructor
        flowMod.setActions(Arrays.asList((OFAction) new OFActionOutput(outPort, (short) 0xffff)));
        flowMod.setLength((short) (OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));

        log.debug("{} {} flow mod {}", new Object[]{ sw, (command == OFFlowMod.OFPFC_DELETE) ? "deleting" : "adding", flowMod });

        // and write it out
        try {
            sw.getOutputStream().write(flowMod);
        } catch (IOException e) {
            log.error("could not write flow mod to switch");
        }
    }
    
    private void writePacketOutForPacketIn(IOFSwitch sw, 
                                          OFPacketIn packetInMessage, 
                                          short egressPort) {
        // from openflow 1.0 spec - need to set these on a struct ofp_packet_out:
        // uint32_t buffer_id; /* ID assigned by datapath (-1 if none). */
        // uint16_t in_port; /* Packet's input port (OFPP_NONE if none). */
        // uint16_t actions_len; /* Size of action array in bytes. */
        // struct ofp_action_header actions[0]; /* Actions. */
        /* uint8_t data[0]; */ /* Packet data. The length is inferred
                                  from the length field in the header.
                                  (Only meaningful if buffer_id == -1.) */
        
        OFPacketOut packetOutMessage = new OFPacketOut();
        short packetOutLength = (short)OFPacketOut.MINIMUM_LENGTH; // starting length

        // Set buffer_id, in_port, actions_len
        packetOutMessage.setBufferId(packetInMessage.getBufferId());
        packetOutMessage.setInPort(packetInMessage.getInPort());
        packetOutMessage.setActionsLength((short)OFActionOutput.MINIMUM_LENGTH);
        packetOutLength += OFActionOutput.MINIMUM_LENGTH;
        
        // set actions
        List<OFAction> actions = new ArrayList<OFAction>(1);      
        actions.add(new OFActionOutput(egressPort, (short) 0));
        packetOutMessage.setActions(actions);

        // set data - only if buffer_id == -1
        if (packetInMessage.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
            byte[] packetData = packetInMessage.getPacketData();
            packetOutMessage.setPacketData(packetData); 
            packetOutLength += (short)packetData.length;
        }
        
        // finally, set the total length
        packetOutMessage.setLength(packetOutLength);              
            
        // and write it out
        try {
            sw.getOutputStream().write(packetOutMessage);
        } catch (IOException e) {
            log.error("could not write packet out to switch");
        }
    }
    
    private Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi) {
        if (!sw.portEnabled(pi.getInPort())) {
            log.debug("ignoring packet received from disabled port: switch {} port {}", sw, pi.getInPort());
            return Command.STOP;
        }

        // Read in packet data headers by using OFMatch
        OFMatch match = new OFMatch();
        match.loadFromPacket(pi.getPacketData(), pi.getInPort());
        Long sourceMac = Ethernet.toLong(match.getDataLayerSource());
        Long destMac = Ethernet.toLong(match.getDataLayerDestination());
        Short vlan = match.getDataLayerVirtualLan();
        if ((destMac & 0xffffffffff00L) == 0x0180c2000000L) {
            log.debug("ignoring packet sent to 802.1D/Q reserved addr: switch {} vlan {} dest MAC {}",
                    new Object[]{ sw, vlan, HexString.toHexString(destMac) });
            return Command.STOP;
        }
        if ((sourceMac & 0x010000000000L) == 0) {
            // If source MAC is a unicast address, learn the port for this MAC/VLAN
            this.addToPortMap(sw, sourceMac, vlan, pi.getInPort());
        }
        
        // Now output flow-mod and/or packet
        Short outPort = getFromPortMap(sw, destMac, vlan);
        if (outPort == null) {
            // If we haven't learned the port for the dest MAC/VLAN, flood it
            this.writePacketOutForPacketIn(sw, pi, OFPort.OFPP_FLOOD.getValue());
        } else {
            // Add flow table entry matching source MAC, dest MAC, VLAN and input port
            // that sends to the port we previously learned for the dest MAC/VLAN.  Also
            // add a flow table entry with source and destination MACs reversed, and
            // input and output ports reversed.  When either entry expires due to idle
            // timeout, remove the other one.  This ensures that if a device moves to
            // a different port, a constant stream of packets headed to the device at
            // its former location does not keep the stale entry alive forever.
            // FIXME: current HP switches ignore DL_SRC and DL_DST fields, so we have to match on
            // NW_SRC and NW_DST as well
            match.setWildcards(sw.getFastWildcards() & ~OFMatch.OFPFW_IN_PORT
                    & ~OFMatch.OFPFW_DL_VLAN & ~OFMatch.OFPFW_DL_SRC & ~OFMatch.OFPFW_DL_DST
                    & ~OFMatch.OFPFW_NW_SRC_MASK & ~OFMatch.OFPFW_NW_DST_MASK);
            this.writeFlowMod(sw, OFFlowMod.OFPFC_ADD, pi.getBufferId(), match, outPort);
            this.writeFlowMod(sw, OFFlowMod.OFPFC_ADD, -1, match.clone()
                    .setDataLayerSource(match.getDataLayerDestination())
                    .setDataLayerDestination(match.getDataLayerSource())
                    .setNetworkSource(match.getNetworkDestination())
                    .setNetworkDestination(match.getNetworkSource())
                    .setTransportSource(match.getTransportDestination())
                    .setTransportDestination(match.getTransportSource())
                    .setInputPort(outPort),
                    match.getInputPort());
        }
        return Command.CONTINUE;
    }
    
    public void addedSwitch(IOFSwitch sw) {
        // go ahead and initialize structures per switch
        log.info("adding macVlanToPortMap for switch {}", sw);
        this.getPortMap(sw);
    }
    
    public void removedSwitch(IOFSwitch sw) {
        // delete the switch structures 
        // they will get recreated on first packetin 
        log.info("removing macVlanToPortMap for switch {}", sw);
        this.macVlanToPortMaps.remove(sw.hashCode());
    }
    
    private Command processPortStatusMessage(IOFSwitch sw, OFPortStatus portStatusMessage) {
        // FIXME This is really just an optimization, speeding up removal of flow
        // entries for a disabled port; think about whether it's really needed
        log.info("learning switch got a port_status");
        OFPhysicalPort port = portStatusMessage.getDesc();
        log.info("received port status: " + portStatusMessage.getReason() + " for port " + port.getPortNumber());
        // LOOK! should be using the reason enums - but how?
        if (portStatusMessage.getReason() == 1 || // DELETED
            (portStatusMessage.getReason() == 2 &&  // MODIFIED and is now down
             ((port.getConfig() & OFPhysicalPort.OFPortConfig.OFPPC_PORT_DOWN.getValue()) > 1 ||
              (port.getState() & OFPhysicalPort.OFPortState.OFPPS_LINK_DOWN.getValue()) > 1))) {
            // then we should reset the switch data structures
            // LOOK! we could be doing something more intelligent like
            // extract out the macs just assigned to a port, but this is ok for now
            this.removedSwitch(sw);
        }
        return Command.CONTINUE;
    }

    private Command processFlowRemovedMessage(IOFSwitch sw, OFFlowRemoved flowRemovedMessage) {
        if (flowRemovedMessage.getCookie() != LearningSwitch.LEARNING_SWITCH_COOKIE) {
            return Command.CONTINUE;
        }
        log.debug("{} flow entry removed {}", sw, flowRemovedMessage);
        OFMatch match = flowRemovedMessage.getMatch();
        // When a flow entry expires, it means the device with the matching source
        // MAC address and VLAN either stopped sending packets or moved to a different
        // port.  If the device moved, we can't know where it went until it sends
        // another packet, allowing us to re-learn its port.  Meanwhile we remove
        // it from the macVlanToPortMap to revert to flooding packets to this device.
        this.removeFromPortMap(sw, Ethernet.toLong(match.getDataLayerSource()),
                match.getDataLayerVirtualLan());
        // Also, if packets keep coming from another device (e.g. from ping), the
        // corresponding reverse flow entry will never expire on its own and will
        // send the packets to the wrong port (the matching input port of the
        // expired flow entry), so we must delete the reverse entry explicitly.
        this.writeFlowMod(sw, OFFlowMod.OFPFC_DELETE, -1, match.clone()
                .setWildcards(sw.getFastWildcards()
                        & ~OFMatch.OFPFW_DL_VLAN & ~OFMatch.OFPFW_DL_SRC & ~OFMatch.OFPFW_DL_DST
                        & ~OFMatch.OFPFW_NW_SRC_MASK & ~OFMatch.OFPFW_NW_DST_MASK)
                .setDataLayerSource(match.getDataLayerDestination())
                .setDataLayerDestination(match.getDataLayerSource())
                .setNetworkSource(match.getNetworkDestination())
                .setNetworkDestination(match.getNetworkSource())
                .setTransportSource(match.getTransportDestination())
                .setTransportDestination(match.getTransportSource()),
                match.getInputPort());
        return Command.CONTINUE;
    }
    
    public Command receive(IOFSwitch sw, OFMessage msg) {
        switch (msg.getType()) {
            case PACKET_IN:
                return this.processPacketInMessage(sw, (OFPacketIn) msg);
            case PORT_STATUS:
                return this.processPortStatusMessage(sw, (OFPortStatus) msg);
            case FLOW_REMOVED:
                return this.processFlowRemovedMessage(sw, (OFFlowRemoved) msg);
            case ERROR:
                log.info("received an error {} from switch {}", (OFError) msg, sw);
                return Command.CONTINUE;
        }
        log.error("received an unexpected message {} from switch {}", msg, sw);
        return Command.CONTINUE;
    }
    
}
