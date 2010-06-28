package net.beaconcontroller.routing;

import net.beaconcontroller.core.IOFSwitch;

public interface IRouting {
    public Route getRoute(IOFSwitch src, IOFSwitch dst);

    public Route getRoute(Long srcDpid, Long dstDpid);

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
}
