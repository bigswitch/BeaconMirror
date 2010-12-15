package net.beaconcontroller.topology;

import net.beaconcontroller.core.IOFSwitch;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface TopologyAware {
    /**
     * 
     * @param src the source switch
     * @param srcPort the source port from the source switch
     * @param dst
     * @param dstPort
     * @param added
     */
    public void linkUpdate(IOFSwitch src, short srcPort,
            IOFSwitch dst, short dstPort, boolean added);
}
