package net.beaconcontroller.routing.apsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.factory.OFMessageFactory;
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
        Device dstDevice = deviceManager.getDeviceByDataLayerAddress(Arrays
                .hashCode(match.getDataLayerDestination()));

        if (dstDevice != null) {
            // does a route exist?
            Route route = routingEngine.getRoute(sw.getId(), dstDevice.getSwId());
            if (route != null) {
                //route.getPath().get(0).get
            }
        }

        return Command.CONTINUE;
    }

    public void pushRoute(OFMessageFactory factory, OFMatch match, Route route) {
        OFFlowMod fm = (OFFlowMod) factory.getMessage(OFType.FLOW_MOD);
        OFActionOutput action = new OFActionOutput();
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(action);
        fm.setIdleTimeout((short)5)
            .setMatch(match)
            .setActions(actions)
            .setLengthU(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH);
        IOFSwitch sw = beaconProvider.getSwitches().get(route.getId().getSrc());

        for (Iterator<Link> it = route.getPath().iterator(); it.hasNext();) {
            Link link = it.next();
            fm.getMatch().setInputPort(link.getOutPort());
            ((OFActionOutput)fm.getActions().get(0)).setPort(link.getInPort());
            try {
                sw.getOutputStream().write(fm);
            } catch (IOException e) {
                log.error("Failure writing flow mod", e);
            }
            if (it.hasNext()) {
                try {
                    fm = fm.clone();
                } catch (CloneNotSupportedException e) {
                    log.error("Failure cloning flow mod", e);
                }
                sw = beaconProvider.getSwitches().get(link.getDst());
            }
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
}
