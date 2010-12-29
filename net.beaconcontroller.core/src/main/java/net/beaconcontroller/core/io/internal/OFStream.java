/**
 * 
 */
package net.beaconcontroller.core.io.internal;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

import net.beaconcontroller.core.io.OFMessageSafeOutStream;

import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.factory.OFMessageFactory;

/**
 * This class is a thread-safe implementation of OFMessageAsyncStream, but only
 * for the OutStream portion. The write functions now trigger the select stream
 * to send the messages after being queued.
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class OFStream extends OFMessageAsyncStream implements OFMessageSafeOutStream {
    protected SelectionKey key;

    /**
     * @param sock
     * @param messageFactory
     * @throws IOException
     */
    public OFStream(SocketChannel sock, OFMessageFactory messageFactory, SelectionKey key)
            throws IOException {
        super(sock, messageFactory);
        this.key = key;
    }

    /**
     * Buffers a single outgoing openflow message
     */
    @Override
    public void write(OFMessage m) throws IOException {
        synchronized (outBuf) {
            appendMessageToOutBuf(m);
        }
        key.interestOps(SelectionKey.OP_WRITE);
      }

    /**
     * Buffers a list of OpenFlow messages
     */
    @Override
    public void write(List<OFMessage> l) throws IOException {
        synchronized (outBuf) {
            for (OFMessage m : l) {
                appendMessageToOutBuf(m);
            }
        }
        key.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * Flush buffered outgoing data. Keep flushing until needsFlush() returns
     * false. Each flush() corresponds to a SocketChannel.write(), so this is
     * designed for one flush() per select() event
     */
    public void flush() throws IOException {
        synchronized (outBuf) {
            outBuf.flip(); // swap pointers; lim = pos; pos = 0;
            sock.write(outBuf); // write data starting at pos up to lim
            outBuf.compact();
        }
    }

    /**
     * Is there outgoing buffered data that needs to be flush()'d?
     */
    public boolean needsFlush() {
        synchronized (outBuf) {
            return outBuf.position() > 0;
        }
    }
}
