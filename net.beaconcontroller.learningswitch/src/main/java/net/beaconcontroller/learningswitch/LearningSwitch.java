package net.beaconcontroller.learningswitch;

import java.io.IOException;
import java.util.Collections;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.packet.Ethernet;
import net.beaconcontroller.learningswitch.dao.ILearningSwitchDao;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - 04/04/10
 */
public class LearningSwitch implements IOFMessageListener {
    protected static Logger log = LoggerFactory.getLogger(LearningSwitch.class);
    protected IBeaconProvider beaconProvider;
    protected ILearningSwitchDao learningSwitchDao;

    // flow-mod - for use in the cookie                                                                                                                             
    public static final int LEARNING_SWITCH_APP_ID = 1;
    // LOOK! This should probably go in some class that encapsulates                                                                                                
    // the app cookie management                                                                                                                                    
    public static final int APP_ID_BITS = 12;
    public static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
    
    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

    public void startUp() {
        log.trace("Starting");
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }

    public void shutDown() {
        log.trace("Stopping");
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
    }

    public String getName() {
        return "switch";
    }

    public void clearTables() {
        learningSwitchDao.clearTables();
    }

    public Command receive(IOFSwitch sw, OFMessage msg) {
        OFPacketIn pi = (OFPacketIn) msg;

        // Build the Match
        OFMatch match = new OFMatch();
        match.loadFromPacket(pi.getPacketData(), pi.getInPort());
        byte[] dlDst = match.getDataLayerDestination();
        byte[] dlSrc = match.getDataLayerSource();
        int bufferId = pi.getBufferId();

        // if the src is not multicast, learn it
        if ((dlSrc[0] & 0x1) == 0) {
            Short srcMapping = learningSwitchDao.getMapping(sw, dlSrc);
            if (srcMapping == null ||
                    !srcMapping.equals(pi.getInPort())) {
                learningSwitchDao.setMapping(sw, dlSrc, pi.getInPort());
            }
        }

        Short outPort = null;
        // if the destination is not multicast, look it up
        if ((dlDst[0] & 0x1) == 0) {
            outPort = learningSwitchDao.getMapping(sw, dlDst);
        }

        // push a flow mod if we know where the destination lives
        if (outPort != null) {
            if (outPort == pi.getInPort()) {
                // don't send out the port it came in
                return Command.CONTINUE;
            }
            match.setInputPort(pi.getInPort());
            match.setWildcards(OFMatch.OFPFW_NW_TOS);

            // build action
            OFActionOutput action = new OFActionOutput()
                .setPort(outPort);

            // build flow mod
            OFFlowMod fm = (OFFlowMod) sw.getInputStream().getMessageFactory()
                    .getMessage(OFType.FLOW_MOD);
            fm.setBufferId(bufferId)
                .setCookie((LEARNING_SWITCH_APP_ID & ((1L << APP_ID_BITS) - 1)) << APP_ID_SHIFT)
                .setIdleTimeout((short) 5)
                .setOutPort((short) OFPort.OFPP_NONE.getValue())
                .setMatch(match)
                .setActions(Collections.singletonList((OFAction)action))
                .setLength(U16.t(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH));
            try {
                sw.getOutputStream().write(fm);
            } catch (IOException e) {
                log.error("Failure writing FlowMod", e);
            }
        }

        // Send a packet out
        if (outPort == null || pi.getBufferId() == 0xffffffff) {
            // build action
            OFActionOutput action = new OFActionOutput()
                .setPort((short) ((outPort == null) ? OFPort.OFPP_FLOOD
                    .getValue() : outPort));

            // build packet out
            OFPacketOut po = new OFPacketOut()
                .setBufferId(bufferId)
                .setInPort(pi.getInPort())
                .setActions(Collections.singletonList((OFAction)action))
                .setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

            // set data if it is included in the packetin
            if (bufferId == 0xffffffff) {
                byte[] packetData = pi.getPacketData();
                po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
                        + po.getActionsLength() + packetData.length));
                po.setPacketData(packetData);
            } else {
                po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
                        + po.getActionsLength()));
            }

            try {
                sw.getOutputStream().write(po);
            } catch (IOException e) {
                log.error("Failure writing PacketOut", e);
            }
        }
        return Command.CONTINUE;
    }

    /**
     * @return the learningSwitchDAO
     */
    public ILearningSwitchDao getLearningSwitchDao() {
        return learningSwitchDao;
    }

    /**
     * @param learningSwitchDAO the learningSwitchDAO to set
     */
    public void setLearningSwitchDao(ILearningSwitchDao learningSwitchDao) {
        this.learningSwitchDao = learningSwitchDao;
    }
}
