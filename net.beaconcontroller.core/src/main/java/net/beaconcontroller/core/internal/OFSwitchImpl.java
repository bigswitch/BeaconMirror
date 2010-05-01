package net.beaconcontroller.core.internal;

import java.nio.channels.SocketChannel;

import net.beaconcontroller.core.IOFSwitch;

import org.openflow.io.OFMessageInStream;
import org.openflow.io.OFMessageOutStream;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class OFSwitchImpl implements IOFSwitch {
    protected SocketChannel socketChannel;
    protected OFMessageInStream inStream;
    protected OFMessageOutStream outStream;

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

    public void setSocketChannel(SocketChannel channel) {
        this.socketChannel = channel;
    }

    @Override
    public OFMessageInStream getInputStream() {
        return inStream;
    }

    @Override
    public OFMessageOutStream getOutputStream() {
        return outStream;
    }

    @Override
    public void setInputStream(OFMessageInStream stream) {
        this.inStream = stream;
    }

    @Override
    public void setOutputStream(OFMessageOutStream stream) {
        this.outStream = stream;
    }
}
