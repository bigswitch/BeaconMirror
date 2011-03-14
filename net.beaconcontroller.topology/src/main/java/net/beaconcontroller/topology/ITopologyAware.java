package net.beaconcontroller.topology;

import net.beaconcontroller.core.IOFSwitch;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface ITopologyAware {
    /**
     * 
     * @param src the source switch
     * @param srcPort the source port from the source switch
     * @param dst
     * @param dstPort
     * @param added
     */
    public void linkUpdate(IOFSwitch src, short srcPortNumber, int srcPortState,
            IOFSwitch dst, short dstPortNumber, int dstPortState, boolean added);
}
