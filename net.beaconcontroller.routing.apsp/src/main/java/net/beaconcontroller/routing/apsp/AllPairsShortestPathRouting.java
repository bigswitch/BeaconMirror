package net.beaconcontroller.routing.apsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.routing.IRoutingEngine;
import net.beaconcontroller.routing.Link;
import net.beaconcontroller.routing.Route;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.factory.OFMessageFactory;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class AllPairsShortestPathRouting implements IOFMessageListener {
    protected static Logger log = LoggerFactory.getLogger(AllPairsShortestPathRouting.class);

    protected IBeaconProvider beaconProvider;
    protected IDeviceManager deviceManager;
    protected IRoutingEngine routingEngine;

    public void startUp() {
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }

    public void shutDown() {
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
    }

    @Override
    public String getName() {
        return "routing";
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg) {
        OFPacketIn pi = (OFPacketIn) msg;
        OFMatch match = new OFMatch();
        match.loadFromPacket(pi.getPacketData(), pi.getInPort());

        // Check if we have the location of the destination
        Device dstDevice = deviceManager.getDeviceByDataLayerAddress(match.getDataLayerDestination());

        if (dstDevice != null) {
            // does a route exist?
            Route route = routingEngine.getRoute(sw.getId(), dstDevice.getSwId());
            if (route != null) {
                // set the route
                log.debug("Pushing route {}", route);
                OFMessageInStream in = sw.getInputStream();
                pushRoute(in.getMessageFactory(), match, route, dstDevice);
                // send the packet down the route
                pushPacket(in.getMessageFactory(), sw, match, pi);
                return Command.STOP;
            }
        }

        return Command.CONTINUE;
    }

    public void pushRoute(OFMessageFactory factory, OFMatch match, Route route, Device dstDevice) {
        OFFlowMod fm = (OFFlowMod) factory.getMessage(OFType.FLOW_MOD);
        OFActionOutput action = new OFActionOutput();
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(action);
        fm.setIdleTimeout((short)5)
            .setMatch(match)
            .setActions(actions)
            .setLengthU(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH);
        IOFSwitch sw = beaconProvider.getSwitches().get(route.getId().getSrc());
        OFMessageSafeOutStream out = sw.getOutputStream(); // to prevent NoClassDefFoundError

        for (Iterator<Link> it = route.getPath().iterator(); it.hasNext();) {
            Link link = it.next();
            ((OFActionOutput)fm.getActions().get(0)).setPort(link.getOutPort());
            try {
                out.write(fm);
            } catch (IOException e) {
                log.error("Failure writing flow mod", e);
            }
            try {
                fm = fm.clone();
            } catch (CloneNotSupportedException e) {
                log.error("Failure cloning flow mod", e);
            }
            fm.getMatch().setInputPort(link.getInPort());
            sw = beaconProvider.getSwitches().get(link.getDst());
            out = sw.getOutputStream();
        }

        // write the flow mod to get the packet out to the device
        ((OFActionOutput)fm.getActions().get(0)).setPort(dstDevice.getSwPort());
        try {
            out.write(fm);
        } catch (IOException e) {
            log.error("Failure writing flow mod", e);
        }
    }

    public void pushPacket(OFMessageFactory factory, IOFSwitch sw, OFMatch match, OFPacketIn pi) {
        OFPacketOut po = (OFPacketOut) factory.getMessage(OFType.PACKET_OUT);
        po.setBufferId(pi.getBufferId());
        po.setInPort(pi.getInPort());

        // set actions
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(new OFActionOutput(OFPort.OFPP_TABLE.getValue(), (short) 0));
        po.setActions(actions)
            .setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

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
    public void setRoutingEngine(IRoutingEngine routingEngine) {
        this.routingEngine = routingEngine;
    }

    /**
     * @param deviceManager the deviceManager to set
     */
    public void setDeviceManager(IDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }
}
