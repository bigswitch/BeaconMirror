package net.beaconcontroller.devicemanager;

/**
 * Used to interact with DeviceManager implementations
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */

import java.util.List;

/**
 * Used to interact with DeviceManager implementations
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface IDeviceManager {
    /**
     * Returns a device for the given data layer address hash code, created by
     * Arrays.hashCode(byte[]), if one exists.
     * @param hashCode
     * @return
     */
    public Device getDeviceByDataLayerAddress(Integer hashCode);

    /**
     * Returns a device for the given data layer address
     * @param address
     * @return
     */
    public Device getDeviceByDataLayerAddress(byte[] address);

    /**
     * Returns a device for the given network layer address
     * @param address
     * @return
     */
    public Device getDeviceByNetworkLayerAddress(Integer address);

    /**
     * Returns a list of all known devices in the system
     * @return
     */
    public List<Device> getDevices();
}
