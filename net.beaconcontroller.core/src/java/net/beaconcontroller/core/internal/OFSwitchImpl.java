package net.beaconcontroller.core.internal;

import java.nio.channels.SocketChannel;

import net.beaconcontroller.core.IOFSwitch;

import org.openflow.io.OFMessageAsyncStream;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class OFSwitchImpl implements IOFSwitch {
    protected SocketChannel socketChannel;
    protected OFMessageAsyncStream stream;

    public OFMessageAsyncStream getStream() {
        return this.stream;
    }

    public void setStream(OFMessageAsyncStream stream) {
        this.stream = stream;
    }

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

    public void setSocketChannel(SocketChannel channel) {
        this.socketChannel = channel;
    }
}
