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
    
    // more flow-mod defaults 
    protected static final short IDLE_TIMEOUT_DEFAULT = 5;
    protected static final short HARD_TIMEOUT_DEFAULT = 0;
    protected static final short PRIORITY_DEFAULT     = 100;
    
    // for managing our map sizes
    protected static final int   MAX_MACS_PER_SWITCH  = 1000;

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
        beaconProvider.addOFMessageListener(OFType.PORT_STATUS, this);
        beaconProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
        beaconProvider.addOFMessageListener(OFType.ERROR, this);
        beaconProvider.addOFSwitchListener(this);
    }

    public void shutDown() {
        log.trace("Stopping");
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
        beaconProvider.removeOFMessageListener(OFType.PORT_STATUS, this);
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
        Map<MacVlanPair,Short> macToPortMap = this.getPortMap(sw);
        if (macToPortMap != null) {
            macToPortMap.put(new MacVlanPair(mac, vlan), portVal);
        } else {
            log.error("Whoa - we should have macToPortMap for the switch");
        }
    }
    
    public Short getFromPortMap(IOFSwitch sw, Long mac, Short vlan) {
        return getPortMap(sw).get(new MacVlanPair(mac, vlan));
    }

    private void writeFlowModForMatch(IOFSwitch sw,
                                     int bufferId,
                                     OFMatch matchFields,
                                     short egressPort) {
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
        short flowModLength = (short) OFFlowMod.MINIMUM_LENGTH;
        
        // the ofp_match is set entirely from the packetInMessage, 
        // but we'll override the wildcards here so it is not an exact
        // match - this allows these flow-mods to be overridden with
        // higher priority flow-mods (e.g., from static flow pusher)
        matchFields.setWildcards(0);
        matchFields.setWildcards(OFMatch.OFPFW_NW_TOS);
        flowMod.setMatch(matchFields);
        
        // set rest of header fields as listed above
        long cookie = (long) (LEARNING_SWITCH_APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;
        flowMod.setCookie(cookie);
        flowMod.setCommand(OFFlowMod.OFPFC_ADD);
        flowMod.setIdleTimeout(LearningSwitch.IDLE_TIMEOUT_DEFAULT);
        flowMod.setHardTimeout(LearningSwitch.HARD_TIMEOUT_DEFAULT);
        flowMod.setPriority(LearningSwitch.PRIORITY_DEFAULT);
        flowMod.setBufferId(bufferId);
        flowMod.setOutPort(OFPort.OFPP_NONE.getValue()); // this is not OFPFC_DELETE, so just set none
        flowMod.setFlags((short)(1 << 0)); // LOOK! This is OFPFF_SEND_FLOW_REM - should be part of OFFlowMod.java 

        // set the ofp_action_header/out actions:
        // from the openflow 1.0 spec: need to set these on a struct ofp_action_output:
        // uint16_t type; /* OFPAT_OUTPUT. */
        // uint16_t len; /* Length is 8. */
        // uint16_t port; /* Output port. */
        // uint16_t max_len; /* Max length to send to controller. */
        // type/len are set because it is OFActionOutput,
        // and port, max_len are arguments to this constructor
        List<OFAction> actions = new ArrayList<OFAction>(1);
        actions.add(new OFActionOutput(egressPort, (short) 0)); // 0 used only if port is OFPP_CONTROLLER
        flowMod.setActions(actions);
        flowModLength += OFActionOutput.MINIMUM_LENGTH;
        
        // finally, set the total length
        flowMod.setLength(flowModLength);
        
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
    
    private Command processPacketInMessage(IOFSwitch sw, OFPacketIn packetInMessage) {
        // read in packet data headers by using OFMatch 
        OFMatch matchFields = new OFMatch();
        matchFields.loadFromPacket(packetInMessage.getPacketData(), 
                                   packetInMessage.getInPort());
        Long sourceMac = Ethernet.toLong(matchFields.getDataLayerSource());
        Short vlan = matchFields.getDataLayerVirtualLan();
        if (vlan == (short) 0xffff) {
            // OFMatch.loadFromPacket sets VLAN ID to 0xffff if the packet contains no VLAN tag;
            // for our purposes that is equivalent to the default VLAN ID 0
            vlan = 0;
        }
        if ((sourceMac & 0x010000000000L) == 0) {
            // sourceMac is a unicast address
            this.addToPortMap(sw, sourceMac, vlan, packetInMessage.getInPort());
        }
        
        // now output flow-mod and/or packet
        Short outPort = getFromPortMap(sw, Ethernet.toLong(matchFields.getDataLayerDestination()), vlan);
        if (outPort == null) {
            this.writePacketOutForPacketIn(sw, packetInMessage, OFPort.OFPP_FLOOD.getValue());
        } else {
            this.writeFlowModForMatch(sw, packetInMessage.getBufferId(), matchFields, outPort);
        }
        return Command.CONTINUE;
    }
    
    public void addedSwitch(IOFSwitch sw) {
        // go ahead and initialize structures per switch
        log.info("adding maps for switch " + sw.getId());
        this.getPortMap(sw);
    }
    
    public void removedSwitch(IOFSwitch sw) {
        // delete the switch structures 
        // they will get recreated on first packetin 
        log.info("removing maps for switch " + sw.getId());
        int switchHash = sw.hashCode();
        this.macVlanToPortMaps.remove(switchHash);
    }
    
    private void processPortStatusMessage(IOFSwitch sw, OFPortStatus portStatusMessage) {
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
    }
    
    public Command receive(IOFSwitch sw, OFMessage msg) {
        // Spec:
        // On a per switch, per packet-in basis do the following:
        //    step a. If the source mac is non broadcast, learn the source port + source mac
        //    step b. If we know where the dest mac lives, send a flow mod + packet
        //    step c. If we don't, broadcast a packet out to all ports minus the incoming port
        //
        //    Notes:
        //    note a: The MAC table size should be bounded to avoid overruns
        //    note b: The module should expire entries
        switch (msg.getType()) {
            case PACKET_IN:
                // The main "learning" 
                return this.processPacketInMessage(sw, (OFPacketIn)msg);

            case PORT_STATUS:
                // make sure we don't keep old ports in our table
                log.info("learning switch got a port_status");
                this.processPortStatusMessage(sw, (OFPortStatus)msg);
                break;
            case ERROR:
                log.info("received an error");
                OFError err = (OFError)msg;
                String s = err.toString() + ", code: " + Integer.toString(err.getErrorCode()) + ", type: " + Integer.toString(err.getErrorType());
                log.info(s);
        }
        return Command.CONTINUE;
    }
    
}
