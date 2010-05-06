package net.beaconcontroller.core.internal;

import java.nio.channels.SocketChannel;

import net.beaconcontroller.core.IOFSwitch;

import org.openflow.io.OFMessageInStream;
import org.openflow.io.OFMessageOutStream;
import org.openflow.protocol.OFFeaturesReply;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class OFSwitchImpl implements IOFSwitch {
    protected OFFeaturesReply featuresReply;
    protected OFMessageInStream inStream;
    protected OFMessageOutStream outStream;
    protected SocketChannel socketChannel;

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

    public void setSocketChannel(SocketChannel channel) {
        this.socketChannel = channel;
    }

    public OFMessageInStream getInputStream() {
        return inStream;
    }

    public OFMessageOutStream getOutputStream() {
        return outStream;
    }

    public void setInputStream(OFMessageInStream stream) {
        this.inStream = stream;
    }

    public void setOutputStream(OFMessageOutStream stream) {
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
}
