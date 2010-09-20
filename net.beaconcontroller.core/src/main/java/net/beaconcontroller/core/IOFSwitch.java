package net.beaconcontroller.core;

import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;

import net.beaconcontroller.core.io.OFMessageSafeOutStream;

import org.openflow.io.OFMessageInStream;
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
    public OFMessageSafeOutStream getOutputStream();

    /**
     * 
     * @param stream
     */
    public void setOutputStream(OFMessageSafeOutStream stream);

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

    /**
     * Get the datapathId of the switch
     * @return
     */
    public long getId();

    /**
     * Retrieves attributes of this switch
     * @return
     */
    public ConcurrentMap<Object, Object> getAttributes();

    /**
     * Retrieves the date the switch connected to this controller
     * @return the date
     */
    public Date getConnectedSince();
}
