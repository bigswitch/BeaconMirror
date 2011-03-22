package net.beaconcontroller.topology;

import net.beaconcontroller.core.IOFSwitch;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface ITopologyAware {
    /**
     * @param srcSw the source switch
     * @param srcPort the source port from the source switch
     * @param srcPortState the state of the port (i.e. STP state)
     * @param dstSw
     * @param dstPort
     */
    public void addedLink(IOFSwitch srcSw, short srcPort, int srcPortState,
            IOFSwitch dstSw, short dstPort, int dstPortState);
    
    /**
     * @param srcSw the source switch
     * @param srcPort the source port from the source switch
     * @param srcPortState the state of the src port (i.e. STP state)
     * @param dstSw
     * @param dstPort
     * @param dstPortState
     */
    public void updatedLink(IOFSwitch srcSw, short srcPort, int srcPortState,
            IOFSwitch dstSw, short dstPort, int dstPortState);

    /**
     * @param srcSw
     * @param srcPort
     * @param dstSw
     * @param dstPort
     */
    public void removedLink(IOFSwitch srcSw, short srcPort,
            IOFSwitch dstSw, short dstPort);
}
