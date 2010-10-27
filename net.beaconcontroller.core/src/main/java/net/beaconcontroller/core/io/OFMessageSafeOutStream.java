/**
 * 
 */
package net.beaconcontroller.core.io;

import org.openflow.io.OFMessageOutStream;

/**
 * This is a thread-safe implementation of the OFMessageOutStream
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public interface OFMessageSafeOutStream extends OFMessageOutStream {
}
