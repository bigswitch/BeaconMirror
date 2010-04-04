package org.beacon.core;

import java.nio.channels.SocketChannel;

import org.openflow.io.OFMessageAsyncStream;

public interface IOFSwitch {
    /**
     * 
     * @return
     */
    public OFMessageAsyncStream getStream();

    /**
     * 
     * @param stream
     */
    public void setStream(OFMessageAsyncStream stream);

    /**
     * 
     * @return
     */
    public SocketChannel getSocketChannel();

    /**
     * 
     * @param channel
     */
    public void setSocketChannel(SocketChannel channel);
}
