package net.beaconcontroller.core.internal;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.io.OFMessageSafeOutStream;

import org.openflow.io.OFMessageInStream;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.util.HexString;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class OFSwitchImpl implements IOFSwitch {
    protected ConcurrentMap<Object, Object> attributes;
    protected OFFeaturesReply featuresReply;
    protected OFMessageInStream inStream;
    protected OFMessageSafeOutStream outStream;
    protected SocketChannel socketChannel;

    public OFSwitchImpl() {
        this.attributes = new ConcurrentHashMap<Object, Object>();
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
}
