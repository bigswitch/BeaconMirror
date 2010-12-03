package net.beaconcontroller.routing;

import net.beaconcontroller.core.IOFSwitch;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface IRoutingEngine {
    public Route getRoute(IOFSwitch src, IOFSwitch dst);

    public Route getRoute(Long srcDpid, Long dstDpid);

    /**
     * Checks if a route exists between two switches
     * @param srcId
     * @param dstId
     * @return true if at least one route exists between the switches
     */
    public boolean routeExists(Long srcId, Long dstId);

    /**
     * Updates a link status
     * @param srcId
     * @param srcPort
     * @param dstId
     * @param dstPort
     * @param added true if the link is new, false if its being removed
     */
    public void update(Long srcId, Short srcPort, Long dstId,
            Short dstPort, boolean added);

    /**
     * This is merely a convenience method that calls
     * @see #update(Long, Short, Long, Short, boolean) and truncates the extra
     * bits from the ports
     * @param srcId
     * @param srcPort
     * @param dstId
     * @param dstPort
     * @param added
     */
    public void update(Long srcId, Integer srcPort, Long dstId,
            Integer dstPort, boolean added);

    /**
     * Remove all routes and reset all state. USE CAREFULLY!
     */
    public void clear();
}
