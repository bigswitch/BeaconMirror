package net.beaconcontroller.devicemanager;

import java.util.List;

/**
 * Used to interact with DeviceManager implementations
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface IDeviceManager {
    /**
     * Returns a device for the given data layer address
     * @param address
     * @return
     */
    public Device getDeviceByDataLayerAddress(byte[] address);

    /**
     * Returns a list of all known devices in the system
     * @return
     */
    public List<Device> getDevices();
}
