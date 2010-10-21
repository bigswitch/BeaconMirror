package net.beaconcontroller.core;

/**
 * Used in conjunction with {@link IOFMessageListener} to allow a listener to
 * filter an incoming message based on the {@link IOFSwitch} it originated from.
 * Implementations wanting to use this interface should implement both
 * IOFMessageListener and IOFSwitchFilter.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface IOFSwitchFilter {

    /**
     * The result of this method call determines whether the
     * IOFMessageListener's receive method is called or not.
     *
     * @param sw switch to filter on
     * @return true to receive the message, false to ignore
     */
    public boolean isInterested(IOFSwitch sw);
}
