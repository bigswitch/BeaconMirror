package net.beaconcontroller.topology;

import net.beaconcontroller.core.IOFSwitch;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface TopologyAware {
    public void linkAdded(IOFSwitch srcSwitch, short srcPort,
            IOFSwitch dstSwitch, short dstPort);

    public void linkRemoved(IOFSwitch srcSwitch, short srcPort,
            IOFSwitch dstSwitch, short dstPort);
}
