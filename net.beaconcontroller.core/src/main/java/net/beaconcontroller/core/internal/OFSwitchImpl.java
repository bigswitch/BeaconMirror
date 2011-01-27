package net.beaconcontroller.core.internal;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.io.OFMessageSafeOutStream;

import org.openflow.io.OFMessageInStream;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFPhysicalPort;
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
    protected ArrayList<OFPhysicalPort> portList;
    
    public OFSwitchImpl() {
        this.attributes = new ConcurrentHashMap<Object, Object>();
        this.connectedSince = new Date();
        this.transactionIdSource = new AtomicInteger();
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

    /**
     *
     */
    public OFFeaturesReply getFeaturesReply() {
        return this.featuresReply;
    }

    /**
     * @param featuresReply the featuresReply to set
     */
    public void setFeaturesReply(OFFeaturesReply featuresReply) {
        this.featuresReply = featuresReply;
        // Initialize the port list from the list in the OFFeaturesReply
        synchronized (this) {
            List<OFPhysicalPort> ports = featuresReply.getPorts();
            this.portList = (ports != null) ?
                    new ArrayList<OFPhysicalPort>(ports) :
                    new ArrayList<OFPhysicalPort>();
        }
    }

    public List<OFPhysicalPort> getPorts() {
        ArrayList<OFPhysicalPort> portListCopy;
        synchronized (this) {
            portListCopy = new ArrayList<OFPhysicalPort>(portList);
        }
        return Collections.unmodifiableList(portListCopy);
    }
    
    private void addOrModifyPort(OFPhysicalPort port) {
        // This method works for both the add and modify cases -- it succeeds
        // whether or not there's already a port object with the same port number
        // as the new port. Typically there shouldn't be an existing port object
        // with the same port number if we're doing an add and there should be
        // an existing port object if we're doing a modify, but it's possible
        // that there was a previous port status message from the switch that
        // was missed/dropped, so the controller port list state may be out of
        // sync with the switch. In that case we don't want to fail on subsequent
        // add/modify port operations, so we handle both cases. We could possibly
        // add a log warning message here, though, if the add or modify call
        // indicates that our port list is out of sync.
        synchronized (this) {
            ListIterator<OFPhysicalPort> iter = portList.listIterator();
            while (iter.hasNext()) {
                OFPhysicalPort nextPort = iter.next();
                if (nextPort.getPortNumber() == port.getPortNumber()) {
                    iter.set(port);
                    return;
                } else if (nextPort.getPortNumber() == port.getPortNumber()) {
                    portList.add(iter.previousIndex(), port);
                    return;
                }
            }
            portList.add(port);
        }
    }
    
    public void addPort(OFPhysicalPort port) {
        addOrModifyPort(port);
    }
    
    public void modifyPort(OFPhysicalPort port) {
        addOrModifyPort(port);
    }
    
    public void deletePort(short portNumber) {
        synchronized (this) {
            ListIterator<OFPhysicalPort> iter = portList.listIterator();
            while (iter.hasNext()) {
                OFPhysicalPort nextPort = iter.next();
                if (nextPort.getPortNumber() == portNumber) {
                    iter.remove();
                    return;
                }
            }
        }
        // Normally we shouldn't reach here, but it's possible that a port
        // status message to add the port was dropped/missed, so we may not
        // have the port in our port list and we don't want to treat that
        // as an error. Should possibly/probably log a warning here, though.
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

    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }
}
