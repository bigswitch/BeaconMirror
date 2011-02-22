package net.beaconcontroller.core.internal;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.io.OFMessageSafeOutStream;

import org.openflow.io.OFMessageInStream;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPhysicalPort.OFPortConfig;
import org.openflow.protocol.OFPhysicalPort.OFPortState;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.util.HexString;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFSwitchImpl implements IOFSwitch {
    protected ConcurrentMap<Object, Object> attributes;
    protected IBeaconProvider beaconProvider;
    protected Date connectedSince;
    protected OFFeaturesReply featuresReply;
    protected OFMessageInStream inStream;
    protected OFMessageSafeOutStream outStream;
    protected SocketChannel socketChannel;
    protected AtomicInteger transactionIdSource;
    protected HashMap<Short, OFPhysicalPort> ports;
    
    public OFSwitchImpl() {
        this.attributes = new ConcurrentHashMap<Object, Object>();
        this.connectedSince = new Date();
        this.transactionIdSource = new AtomicInteger();
        this.ports = new HashMap<Short, OFPhysicalPort>();
    }

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

    public void setSocketChannel(SocketChannel channel) {
        this.socketChannel = channel;
    }

    public OFMessageInStream getInputStream() {
        return inStream;
    }

    public OFMessageSafeOutStream getOutputStream() {
        return outStream;
    }

    public void setInputStream(OFMessageInStream stream) {
        this.inStream = stream;
    }

    public void setOutputStream(OFMessageSafeOutStream stream) {
        this.outStream = stream;
    }

    public OFFeaturesReply getFeaturesReply() {
        return this.featuresReply;
    }

    public synchronized void setFeaturesReply(OFFeaturesReply featuresReply) {
        this.featuresReply = featuresReply;
        for (OFPhysicalPort port : featuresReply.getPorts()) {
            ports.put(port.getPortNumber(), port);
        }
    }

    public synchronized List<OFPhysicalPort> getEnabledPorts() {
        List<OFPhysicalPort> result = new ArrayList<OFPhysicalPort>();
        for (OFPhysicalPort port : ports.values()) {
            if (portEnabled(port)) {
                result.add(port);
            }
        }
        return result;
    }
    
    public synchronized void setPort(OFPhysicalPort port) {
        ports.put(port.getPortNumber(), port);
    }
    
    public synchronized void deletePort(short portNumber) {
        ports.remove(portNumber);
    }

    public synchronized boolean portEnabled(short portNumber) {
        return portEnabled(ports.get(portNumber));
    }
    
    public boolean portEnabled(OFPhysicalPort port) {
        if (port == null)
            return false;
        if ((port.getConfig() & OFPortConfig.OFPPC_PORT_DOWN.getValue()) > 0)
            return false;
        if ((port.getState() & OFPortState.OFPPS_LINK_DOWN.getValue()) > 0)
            return false;
        if ((port.getState() & OFPortState.OFPPS_STP_MASK.getValue()) == OFPortState.OFPPS_STP_BLOCK.getValue())
            return false;
        return true;
    }
    
    @Override
    public long getId() {
        if (this.featuresReply == null)
            throw new RuntimeException("Features reply has not yet been set");
        return this.featuresReply.getDatapathId();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFSwitchImpl [" + socketChannel.socket() + " DPID[" + ((featuresReply != null) ? HexString.toHexString(featuresReply.getDatapathId()) : "?") + "]]";
    }

    @Override
    public ConcurrentMap<Object, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Date getConnectedSince() {
        return connectedSince;
    }

    @Override
    public int getNextTransactionId() {
        return this.transactionIdSource.incrementAndGet();
    }

    @Override
    public Future<List<OFStatistics>> getStatistics(OFStatisticsRequest request) throws IOException {
        request.setXid(getNextTransactionId());
        OFStatisticsFuture future = new OFStatisticsFuture(beaconProvider, this, request.getXid());
        this.beaconProvider.addOFMessageListener(OFType.STATS_REPLY, future);
        this.beaconProvider.addOFSwitchListener(future);
        this.getOutputStream().write(request);
        return future;
    }

    @Override
    public int getFastWildcards() {
        // FIXME: query the switch; this is the set supported by current HP switches
        return OFMatch.OFPFW_IN_PORT | OFMatch.OFPFW_NW_PROTO | OFMatch.OFPFW_TP_SRC
            | OFMatch.OFPFW_TP_DST | OFMatch.OFPFW_NW_SRC_ALL | OFMatch.OFPFW_NW_DST_ALL
            | OFMatch.OFPFW_NW_TOS;
    }

    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }
}
