package net.beaconcontroller.topology.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFSwitchListener;
import net.beaconcontroller.packet.Ethernet;
import net.beaconcontroller.packet.LLDP;
import net.beaconcontroller.packet.LLDPTLV;
import net.beaconcontroller.routing.IRoutingEngine;
import net.beaconcontroller.topology.ITopology;
import net.beaconcontroller.topology.IdPortTuple;
import net.beaconcontroller.topology.LinkTuple;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFPhysicalPort.OFPortConfig;
import org.openflow.protocol.OFPhysicalPort.OFPortState;
import org.openflow.protocol.OFPortStatus.OFPortReason;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invariants:
 *  -portLinks and switchLinks will not contain empty Sets outside of critical sections
 *  -portLinks contains LinkTuples where one of the src or dst IdPortTuple matches the map key
 *  -switchLinks contains LinkTuples where one of the src or dst IdPortTuple's id matches the switch id
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class TopologyImpl implements IOFMessageListener, IOFSwitchListener, ITopology {
    protected static Logger logger = LoggerFactory.getLogger(TopologyImpl.class);

    protected IBeaconProvider beaconProvider;

    /**
     * Map from link to the most recent time it was verified functioning
     */
    protected Map<LinkTuple, Long> links;
    protected Timer lldpSendTimer;
    protected Long lldpFrequency = 15L * 1000; // sending frequency
    protected Long lldpTimeout = 35L * 1000; // timeout

    /**
     * Map from a id:port to the set of links containing it as an endpoint
     */
    protected Map<IdPortTuple, Set<LinkTuple>> portLinks;
    protected IRoutingEngine routingEngine;

    /**
     * Map from switch id to a set of all links with it as an endpoint
     */
    protected Map<Long, Set<LinkTuple>> switchLinks;
    protected Timer timeoutLinksTimer;

    protected void startUp() {
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
        beaconProvider.addOFMessageListener(OFType.PORT_STATUS, this);
        beaconProvider.addOFSwitchListener(this);
        links = new HashMap<LinkTuple, Long>();
        portLinks = new HashMap<IdPortTuple, Set<LinkTuple>>();
        switchLinks = new HashMap<Long, Set<LinkTuple>>();

        lldpSendTimer = new Timer();
        lldpSendTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendLLDPs();
            }}, 1000, lldpFrequency);

        timeoutLinksTimer = new Timer();
        timeoutLinksTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeoutLinks();
            }}, 1000, lldpTimeout);
    }

    protected void shutDown() {
        lldpSendTimer.cancel();
        beaconProvider.removeOFSwitchListener(this);
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
        beaconProvider.removeOFMessageListener(OFType.PORT_STATUS, this);
    }

    protected void sendLLDPs() {
        Ethernet ethernet = new Ethernet()
            .setSourceMACAddress(new byte[6])
            .setDestinationMACAddress("01:80:c2:00:00:0e")
            .setEtherType(Ethernet.TYPE_LLDP);

        LLDP lldp = new LLDP();
        ethernet.setPayload(lldp);
        byte[] chassisId = new byte[] {4, 0, 0, 0, 0, 0, 0}; // filled in later
        byte[] portId = new byte[] {2, 0, 0}; // filled in later
        lldp.setChassisId(new LLDPTLV().setType((byte) 1).setLength((short) 7).setValue(chassisId));
        lldp.setPortId(new LLDPTLV().setType((byte) 2).setLength((short) 3).setValue(portId));
        lldp.setTtl(new LLDPTLV().setType((byte) 3).setLength((short) 2).setValue(new byte[] {0, 0x78}));

        // OpenFlow OUI - 00-26-E1
        byte[] dpidTLVValue = new byte[] {0x0, 0x26, (byte) 0xe1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        LLDPTLV dpidTLV = new LLDPTLV().setType((byte) 0x127).setLength((short) 12).setValue(dpidTLVValue);
        lldp.setOptionalTLVList(new ArrayList<LLDPTLV>());
        lldp.getOptionalTLVList().add(dpidTLV);

        Map<Long, IOFSwitch> switches = beaconProvider.getSwitches();
        byte[] dpidArray = new byte[8];
        ByteBuffer dpidBB = ByteBuffer.wrap(dpidArray);
        ByteBuffer portBB = ByteBuffer.wrap(portId, 1, 2);
        for (Entry<Long, IOFSwitch> entry : switches.entrySet()) {
            Long dpid = entry.getKey();
            IOFSwitch sw = entry.getValue();
            dpidBB.putLong(dpid);

            // set the ethernet source mac to last 6 bytes of dpid
            System.arraycopy(dpidArray, 2, ethernet.getSourceMACAddress(), 0, 6);
            // set the chassis id's value to last 6 bytes of dpid
            System.arraycopy(dpidArray, 2, chassisId, 1, 6);
            // set the optional tlv to the full dpid
            System.arraycopy(dpidArray, 0, dpidTLVValue, 4, 8);
            for (OFPhysicalPort port : sw.getFeaturesReply().getPorts()) {
                if (port.getPortNumber() == OFPort.OFPP_LOCAL.getValue())
                    continue;

                // set the portId to the outgoing port
                portBB.putShort(port.getPortNumber());

                // serialize and wrap in a packet out
                byte[] data = ethernet.serialize();
                OFPacketOut po = new OFPacketOut();
                po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
                po.setInPort(OFPort.OFPP_NONE);

                // set actions
                List<OFAction> actions = new ArrayList<OFAction>();
                actions.add(new OFActionOutput(port.getPortNumber(), (short) 0));
                po.setActions(actions);
                po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

                // set data
                po.setLengthU(OFPacketOut.MINIMUM_LENGTH + po.getActionsLength() + data.length);
                po.setPacketData(data);

                // send
                try {
                    sw.getOutputStream().write(po);
                } catch (IOException e) {
                    e.printStackTrace();
                }

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
        if (msg instanceof OFPacketIn)
            return handlePacketIn(sw, (OFPacketIn) msg);
        else
            return handlePortStatus(sw, (OFPortStatus)msg);
    }

    protected Command handlePacketIn(IOFSwitch sw, OFPacketIn pi) {
        Ethernet eth = new Ethernet();
        eth.deserialize(pi.getPacketData(), 0, pi.getPacketData().length);

        if (!(eth.getPayload() instanceof LLDP))
            return Command.CONTINUE;

        LLDP lldp = (LLDP) eth.getPayload();
        ByteBuffer portBB = ByteBuffer.wrap(lldp.getPortId().getValue());
        portBB.position(1);
        Short remotePort = portBB.getShort();
        Long remoteDpid = 0L;
        boolean remoteDpidSet = false;

        // Verify this LLDP packet matches what we're looking for
        for (LLDPTLV lldptlv : lldp.getOptionalTLVList()) {
            if (lldptlv.getType() == 0x127 && lldptlv.getLength() == 12 &&
                    lldptlv.getValue()[0] == 0x0 && lldptlv.getValue()[1] == 0x26 &&
                    lldptlv.getValue()[2] == 0xe1 && lldptlv.getValue()[3] == 0x0) {
                ByteBuffer dpidBB = ByteBuffer.wrap(lldptlv.getValue());
                remoteDpid = dpidBB.getLong(4);
                remoteDpidSet = true;
                break;
            }
        }

        if (!remoteDpidSet) {
            logger.error("Failed to determine remote switch DPID from received LLDP");
            return Command.STOP;
        }

        IOFSwitch remoteSwitch = beaconProvider.getSwitches().get(remoteDpid);

        if (remoteSwitch == null) {
            logger.error("Failed to locate remote switch with DPID: {}", remoteDpid);
            return Command.STOP;
        }

        // Store the time of update to this link, and push it out to routingEngine
        // TODO Locking!
        LinkTuple lt = new LinkTuple(new IdPortTuple(sw.getId(), pi.getInPort()),
                new IdPortTuple(remoteDpid, remotePort));
        addOrUpdateLink(lt);

        // Consume this message
        return Command.STOP;
    }

    protected void addOrUpdateLink(LinkTuple lt) {
        if (links.put(lt, System.currentTimeMillis()) == null) {
            // index it by switch source
            if (!switchLinks.containsKey(lt.getSrc().getId()))
                switchLinks.put(lt.getSrc().getId(), new HashSet<LinkTuple>());
            switchLinks.get(lt.getSrc().getId()).add(lt);

            // index it by switch dest
            if (!switchLinks.containsKey(lt.getDst().getId()))
                switchLinks.put(lt.getDst().getId(), new HashSet<LinkTuple>());
            switchLinks.get(lt.getDst().getId()).add(lt);

            // index both ends by switch:port
            if (!portLinks.containsKey(lt.getSrc()))
                portLinks.put(lt.getSrc(), new HashSet<LinkTuple>());
            portLinks.get(lt.getSrc()).add(lt);

            if (!portLinks.containsKey(lt.getDst()))
                portLinks.put(lt.getDst(), new HashSet<LinkTuple>());
            portLinks.get(lt.getDst()).add(lt);

            if (routingEngine != null) {
                routingEngine.update(lt.getSrc().getId(), lt.getSrc().getPort(),
                        lt.getDst().getId(), lt.getDst().getPort(), true);
            }
        }
    }

    protected void deleteLink(LinkTuple lt) {
        this.switchLinks.get(lt.getSrc().getId()).remove(lt);
        this.switchLinks.get(lt.getDst().getId()).remove(lt);
        if (this.switchLinks.containsKey(lt.getSrc().getId()) &&
                this.switchLinks.get(lt.getSrc().getId()).isEmpty())
            this.switchLinks.remove(lt.getSrc().getId());
        if (this.switchLinks.containsKey(lt.getDst().getId()) &&
                this.switchLinks.get(lt.getDst().getId()).isEmpty())
            this.switchLinks.remove(lt.getDst().getId());

        this.portLinks.get(lt.getSrc()).remove(lt);
        this.portLinks.get(lt.getDst()).remove(lt);
        if (this.portLinks.get(lt.getSrc()).isEmpty())
            this.portLinks.remove(lt.getSrc());
        if (this.portLinks.get(lt.getDst()).isEmpty())
            this.portLinks.remove(lt.getDst());

        this.links.remove(lt);

        if (routingEngine != null) {
            routingEngine.update(lt.getSrc().getId(), lt.getSrc().getPort(),
                    lt.getDst().getId(), lt.getDst().getPort(), false);
        }
    }

    protected Command handlePortStatus(IOFSwitch sw, OFPortStatus ps) {
        // if ps is a delete, or a modify where the port is down or configured down
        if ((byte)OFPortReason.OFPPR_DELETE.ordinal() == ps.getReason() ||
            ((byte)OFPortReason.OFPPR_MODIFY.ordinal() == ps.getReason() &&
                        (((OFPortConfig.OFPPC_PORT_DOWN.getValue() & ps.getDesc().getConfig()) > 0) ||
                                ((OFPortState.OFPPS_LINK_DOWN.getValue() & ps.getDesc().getState()) > 0)))) {
            IdPortTuple tuple = new IdPortTuple(sw.getId(), ps.getDesc().getPortNumber());

            // TODO lock here
            List<LinkTuple> eraseList = new ArrayList<LinkTuple>();
            if (this.portLinks.containsKey(tuple)) {
                eraseList.addAll(this.portLinks.get(tuple));
                for (LinkTuple lt : this.portLinks.get(tuple)) {
                    // cleanup id:port->links map
                    // check src
                    if (!lt.getSrc().equals(tuple))
                        if (portLinks.get(lt.getSrc()) != null) {
                            portLinks.get(lt.getSrc()).remove(lt);
                            if (portLinks.get(lt.getSrc()).isEmpty())
                                portLinks.remove(lt.getSrc());
                        }
                    else
                        if (portLinks.get(lt.getDst()) != null) {
                            portLinks.get(lt.getDst()).remove(lt);
                            if (portLinks.get(lt.getDst()).isEmpty())
                                portLinks.remove(lt.getDst());
                        }

                    // cleanup swid->links map
                    this.switchLinks.get(sw.getId()).remove(lt);
                    if (this.switchLinks.get(sw.getId()).isEmpty())
                        this.switchLinks.remove(sw.getId());

                    // cleanup link->timeout map
                    this.links.remove(lt);
                    
                }
            }
            for (LinkTuple lt : eraseList)
                if (routingEngine != null)
                    routingEngine.update(lt.getSrc().getId(), lt.getSrc().getPort(), lt
                            .getDst().getId(), lt.getDst().getPort(), false);
            eraseList.clear();
        }
        return Command.CONTINUE;
    }

    @Override
    public void addedSwitch(IOFSwitch sw) {
        // no-op
    }

    @Override
    public void removedSwitch(IOFSwitch sw) {
        // TODO locking
        List<LinkTuple> eraseList = new ArrayList<LinkTuple>();
        if (switchLinks.containsKey(sw.getId())) {
            // add all tuples with an endpoint on this switch to erase list
            eraseList.addAll(switchLinks.get(sw.getId()));

            for (LinkTuple lt : switchLinks.get(sw.getId())) {
                // cleanup id:port->links map
                // check src
                if (lt.getSrc().getId().equals(sw.getId())) {
                    portLinks.remove(lt.getSrc());
                } else {
                    if (portLinks.get(lt.getSrc()) != null) {
                        portLinks.get(lt.getSrc()).remove(lt);
                        if (portLinks.get(lt.getSrc()).isEmpty())
                            portLinks.remove(lt.getSrc());
                    }

                    this.switchLinks.get(lt.getSrc().getId()).remove(lt);
                    if (this.switchLinks.get(lt.getSrc().getId()).isEmpty())
                        this.switchLinks.remove(lt.getSrc().getId());
                }

                // check dst
                if (lt.getDst().getId().equals(sw.getId())) {
                    portLinks.remove(lt.getDst());
                } else {
                    if (portLinks.get(lt.getDst()) != null) {
                        portLinks.get(lt.getDst()).remove(lt);
                        if (portLinks.get(lt.getDst()).isEmpty())
                            portLinks.remove(lt.getDst());
                    }

                    this.switchLinks.get(lt.getDst().getId()).remove(lt);
                    if (this.switchLinks.get(lt.getDst().getId()).isEmpty())
                        this.switchLinks.remove(lt.getDst().getId());
                }

                // cleanup link->timeout map
                this.links.remove(lt);
            }
            switchLinks.remove(sw.getId());
        }
        for (LinkTuple lt : eraseList)
            if (routingEngine != null)
                routingEngine.update(lt.getSrc().getId(), lt.getSrc().getPort(), lt
                        .getDst().getId(), lt.getDst().getPort(), false);
        eraseList.clear();
    }

    protected void timeoutLinks() {
        // TODO locking
        List<LinkTuple> eraseList = new ArrayList<LinkTuple>();
        Iterator<Entry<LinkTuple, Long>> it = this.links.entrySet().iterator();
        Long curTime = System.currentTimeMillis();

        while (it.hasNext()) {
            Entry<LinkTuple, Long> entry = it.next();
            if (entry.getValue() + this.lldpTimeout < curTime) {
                eraseList.add(entry.getKey());
            }
        }

        for (LinkTuple lt : eraseList) {
            deleteLink(lt);
        }
    }

    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

    /**
     * @param routingEngine the routingEngine to set
     */
    public void setRouting(IRoutingEngine routingEngine) {
        this.routingEngine = routingEngine;
    }
}
