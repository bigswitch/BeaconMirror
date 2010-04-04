package org.beacon.plugins.learningswitch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.beacon.core.IBeaconProvider;
import org.beacon.core.IOFMessageListener;
import org.beacon.core.IOFSwitch;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.LRULinkedHashMap;
import org.openflow.util.U16;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class LearningSwitch implements IOFMessageListener {
    protected IBeaconProvider beaconProvider;
    protected Map<IOFSwitch, Map<Integer, Short>> macTables =
        new HashMap<IOFSwitch, Map<Integer, Short>>();

    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
        System.out.println("setbeaconprovider");
    }

    public void startUp() {
        beaconProvider.addListener(OFType.PACKET_IN, this);
    }

    public void shutDown() {
        beaconProvider.removeListener(OFType.PACKET_IN, this);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void receive(IOFSwitch sw, OFMessage msg) {
        OFPacketIn pi = (OFPacketIn) msg;
        Map<Integer, Short> macTable = macTables.get(sw);
        if (macTable == null) {
            macTable = new LRULinkedHashMap<Integer, Short>(64001, 64000);
            macTables.put(sw, macTable);
        }

        // Build the Match
        OFMatch match = new OFMatch();
        match.loadFromPacket(pi.getPacketData());
        byte[] dlDst = match.getDataLayerDestination();
        Integer dlDstKey = Arrays.hashCode(dlDst);
        byte[] dlSrc = match.getDataLayerSource();
        Integer dlSrcKey = Arrays.hashCode(dlSrc);
        int bufferId = pi.getBufferId();

        // if the src is not multicast, learn it
        if ((dlSrc[0] & 0x1) == 0) {
            if (!macTable.containsKey(dlSrcKey) ||
                    !macTable.get(dlSrcKey).equals(pi.getInPort())) {
                macTable.put(dlSrcKey, pi.getInPort());
            }
        }

        Short outPort = null;
        // if the destination is not multicast, look it up
        if ((dlDst[0] & 0x1) == 0) {
            outPort = macTable.get(dlDstKey);
        }

        // push a flow mod if we know where the packet should be going
        if (outPort != null) {
            OFFlowMod fm = (OFFlowMod) sw.getStream().getMessageFactory()
                    .getMessage(OFType.FLOW_MOD);
            fm.setBufferId(bufferId);
            fm.setCommand((short) 0);
            fm.setCookie(0);
            fm.setFlags((short) 0);
            fm.setHardTimeout((short) 0);
            fm.setIdleTimeout((short) 5);
            match.setInputPort(pi.getInPort());
            match.setWildcards(0);
            fm.setMatch(match);
            fm.setOutPort((short) OFPort.OFPP_NONE.getValue());
            fm.setPriority((short) 0);
            OFActionOutput action = new OFActionOutput();
            action.setMaxLength((short) 0);
            action.setPort(outPort);
            List<OFAction> actions = new ArrayList<OFAction>();
            actions.add(action);
            fm.setActions(actions);
            fm.setLength(U16.t(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH));
            sw.getStream().write(fm);
        }

        // Send a packet out
        if (outPort == null || pi.getBufferId() == 0xffffffff) {
           OFPacketOut po = new OFPacketOut();
           po.setBufferId(bufferId);
           po.setInPort(pi.getInPort());

           // set actions
           OFActionOutput action = new OFActionOutput();
           action.setMaxLength((short) 0);
           action.setPort((short) ((outPort == null) ? OFPort.OFPP_FLOOD.getValue() :
               outPort));
           List<OFAction> actions = new ArrayList<OFAction>();
           actions.add(action);
           po.setActions(actions);
           po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

           // set data if needed
           if (bufferId == 0xffffffff) {
               byte[] packetData = pi.getPacketData();
               po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH +
                       po.getActionsLength()+packetData.length));
               po.setPacketData(packetData);
           } else {
               po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH +
                       po.getActionsLength()));
           }
           sw.getStream().write(po);
        }
    }
}
