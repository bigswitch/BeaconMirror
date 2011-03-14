package net.beaconcontroller.routing.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.io.OFMessageSafeOutStream;
import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.devicemanager.IDeviceManagerAware;
import net.beaconcontroller.routing.IRoutingEngine;
import net.beaconcontroller.routing.Link;
import net.beaconcontroller.routing.Route;
import net.beaconcontroller.topology.SwitchPortTuple;

import org.openflow.io.OFMessageInStream;
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
import org.openflow.util.HexString;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class Routing implements IOFMessageListener, IDeviceManagerAware {
    protected static Logger log = LoggerFactory.getLogger(Routing.class);

    protected IBeaconProvider beaconProvider;
    protected IDeviceManager deviceManager;
    protected IRoutingEngine routingEngine;
    
    // flow-mod - for use in the cookie
    public static final int ROUTING_APP_ID = 2;
    // LOOK! This should probably go in some class that encapsulates
    // the app cookie management
    public static final int APP_ID_BITS = 12;
    public static final int APP_ID_SHIFT = (64 - APP_ID_BITS);

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
            Route route = null;
            SwitchPortTuple dstSwPort = null;
            for (SwitchPortTuple p : dstDevice.getSwPorts()) {
                route = routingEngine.getRoute(sw.getId(), p.getSw().getId());
                if (route != null) {
                    dstSwPort = p;
                    break;
                }
            }
            if (route != null) {
                // set the route
                if (log.isTraceEnabled())
                    log.trace("Pushing route match={} route={} destination={}:{}", new Object[] {match, route, dstSwPort.getSw(), dstSwPort.getPortNumber()});
                OFMessageInStream in = sw.getInputStream();
                pushRoute(in.getMessageFactory(), match, route, dstSwPort, pi.getBufferId());

                // send the packet if its not buffered
                if (pi.getBufferId() == 0xffffffff) {
                    pushPacket(in.getMessageFactory(), sw, match, pi);
                }
                return Command.STOP;
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("No route found from {}:{} to device {}",
                            new Object[] { HexString.toHexString(sw.getId()),
                                    pi.getInPort(), dstDevice });
                }
            }
        } else {
            if (log.isTraceEnabled()) {
                // filter multicast destinations
                if ((match.getDataLayerDestination()[0] & 0x1) == 0) {
                    log.trace("Unable to locate device with address {}",
                            HexString.toHexString(match
                                    .getDataLayerDestination()));
                }
            }
        }

        return Command.CONTINUE;
    }

    /**
     * Push routes from back to front
     * @param factory
     * @param match
     * @param route
     * @param dstDevice
     */
    public void pushRoute(OFMessageFactory factory, OFMatch match, Route route, SwitchPortTuple dstSwPort, int bufferId) {
        OFFlowMod fm = (OFFlowMod) factory.getMessage(OFType.FLOW_MOD);
        OFActionOutput action = new OFActionOutput();
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(action);
        match.setWildcards(OFMatch.OFPFW_NW_TOS);
        fm.setIdleTimeout((short)5)
            .setBufferId(0xffffffff)
            .setCookie((ROUTING_APP_ID & ((1L << APP_ID_BITS) - 1)) << APP_ID_SHIFT)
            .setMatch(match.clone())
            .setActions(actions)
            .setLengthU(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH);
        IOFSwitch sw = dstSwPort.getSw();
        OFMessageSafeOutStream out = sw.getOutputStream(); // to prevent NoClassDefFoundError
        ((OFActionOutput)fm.getActions().get(0)).setPort(dstSwPort.getPortNumber());

        for (int i = route.getPath().size() - 1; i >= 0; --i) {
            Link link = route.getPath().get(i);
            fm.getMatch().setInputPort(link.getInPort());
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

            // setup for the next loop iteration
            ((OFActionOutput)fm.getActions().get(0)).setPort(link.getOutPort());
            if (i > 0) {
                sw = beaconProvider.getSwitches().get(route.getPath().get(i-1).getDst());
            } else {
                sw = beaconProvider.getSwitches().get(route.getId().getSrc());
            }
            if (sw == null) {
                if (log.isWarnEnabled()) {
                    log.warn(
                            "Unable to push route, switch at DPID {} not available",
                            (i > 0) ? HexString.toHexString(route.getPath()
                                    .get(i - 1).getDst()) : HexString
                                    .toHexString(route.getId().getSrc()));
                }
                return;
            }
            out = sw.getOutputStream();
        }
        // set the original match for the first switch, and buffer id
        fm.setMatch(match)
            .setBufferId(bufferId);

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

        byte[] packetData = pi.getPacketData();
        po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
                + po.getActionsLength() + packetData.length));
        po.setPacketData(packetData);

        try {
            sw.getOutputStream().write(po);
        } catch (IOException e) {
            log.error("Failure writing packet out", e);
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

    @Override
    public void deviceAdded(Device device) {
        // NOOP
    }

    @Override
    public void deviceRemoved(Device device) {
        // NOOP
    }

    @Override
    public void deviceMoved(Device device, IOFSwitch oldSw, Short oldPort,
            IOFSwitch sw, Short port) {
        // Build flow mod to delete based on destination mac == device mac
        OFMatch match = new OFMatch();
        match.setDataLayerDestination(device.getDataLayerAddress());
        match.setWildcards(OFMatch.OFPFW_ALL ^ OFMatch.OFPFW_DL_DST);
        OFMessage fm = ((OFFlowMod) sw.getInputStream().getMessageFactory()
            .getMessage(OFType.FLOW_MOD))
            .setCommand(OFFlowMod.OFPFC_DELETE)
            .setOutPort((short) OFPort.OFPP_NONE.getValue())
            .setMatch(match)
            .setLength(U16.t(OFFlowMod.MINIMUM_LENGTH));

        // Flush to all switches
        for (IOFSwitch outSw : beaconProvider.getSwitches().values()) {
            try {
                outSw.getOutputStream().write(fm);
            } catch (IOException e) {
                log.error("Failure sending flow mod delete for moved device", e);
            }
        }
    }
}
