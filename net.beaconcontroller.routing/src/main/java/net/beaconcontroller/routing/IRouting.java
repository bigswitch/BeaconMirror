package net.beaconcontroller.routing;

import net.beaconcontroller.core.IOFSwitch;

public interface IRouting {
    public Route getRoute(IOFSwitch src, IOFSwitch dst);

    public Route getRoute(Long srcDpid, Long dstDpid);

    public void update(Long srcId, short srcPort, Long dstId,
            short dstPort, boolean added);

}
