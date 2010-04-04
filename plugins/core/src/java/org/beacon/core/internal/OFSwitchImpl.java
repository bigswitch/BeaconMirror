package org.beacon.core.internal;

import java.nio.channels.SocketChannel;

import org.beacon.core.IOFSwitch;
import org.openflow.io.OFMessageAsyncStream;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class OFSwitchImpl implements IOFSwitch {
    protected SocketChannel socketChannel;
    protected OFMessageAsyncStream stream;

    @Override
    public OFMessageAsyncStream getStream() {
        return this.stream;
    }

    @Override
    public void setStream(OFMessageAsyncStream stream) {
        this.stream = stream;
    }

    @Override
    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

    @Override
    public void setSocketChannel(SocketChannel channel) {
        this.socketChannel = channel;
    }
}
