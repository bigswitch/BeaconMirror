package net.beaconcontroller.core;

import java.nio.channels.SocketChannel;

import org.openflow.io.OFMessageInStream;
import org.openflow.io.OFMessageOutStream;
import org.openflow.protocol.OFFeaturesReply;

public interface IOFSwitch {
    /**
     *
     * @return
     */
    public OFMessageInStream getInputStream();

    /**
     *
     * @param stream
     */
    public void setInputStream(OFMessageInStream stream);

    /**
     *
     * @return
     */
    public OFMessageOutStream getOutputStream();

    /**
     * 
     * @param stream
     */
    public void setOutputStream(OFMessageOutStream stream);

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

    /**
     * Returns the cached OFFeaturesReply message returned by the switch during
     * the initial handshake.
     * @return
     */
    public OFFeaturesReply getFeaturesReply();

    /**
     * Set the OFFeaturesReply message returned by the switch during initial
     * handshake.
     * @param featuresReply
     */
    public void setFeaturesReply(OFFeaturesReply featuresReply);
}
