package net.beaconcontroller.hub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;

/**
 *
 * @author David Erickson (derickso@stanford.edu) - 04/04/10
 */
public class Hub implements IOFMessageListener {
    protected IBeaconProvider beaconProvider;

    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

    public void startUp() {
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }

    public void shutDown() {
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
    }

    public String getName() {
        return Hub.class.getPackage().getName();
    }

    public Command receive(IOFSwitch sw, OFMessage msg) {
        OFPacketIn pi = (OFPacketIn) msg;
        OFPacketOut po = (OFPacketOut) sw.getInputStream().getMessageFactory()
                .getMessage(OFType.PACKET_OUT);
        po.setBufferId(pi.getBufferId());
        po.setInPort(pi.getInPort());

        // set actions
        OFActionOutput action = new OFActionOutput();
        action.setMaxLength((short) 0);
        action.setPort((short) OFPort.OFPP_FLOOD.getValue());
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(action);
        po.setActions(actions);
        po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

        // set data if needed
        if (pi.getBufferId() == 0xffffffff) {
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
            e.printStackTrace();
        }

        return Command.CONTINUE;
    }
}
