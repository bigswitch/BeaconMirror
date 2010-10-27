package net.beaconcontroller.core;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import net.beaconcontroller.core.io.OFMessageSafeOutStream;

import org.openflow.io.OFMessageInStream;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface IOFSwitch {
    /**
     *
     * @return
     */
    public OFMessageInStream getInputStream();

    /**
     *
     * @return
     */
    public OFMessageSafeOutStream getOutputStream();

    /**
     *
     * @return
     */
    public SocketChannel getSocketChannel();

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

    /**
     * Returns the next available transaction id
     * @return
     */
    public int getNextTransactionId();

    /**
     * Returns a Future object that can be used to retrieve the asynchronous
     * OFStatisticsReply when it is available.
     *
     * @param request statistics request
     * @return Future object wrapping OFStatisticsReply
     * @throws IOException 
     */
    public Future<List<OFStatistics>> getStatistics(OFStatisticsRequest request)
            throws IOException;
}
