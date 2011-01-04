package net.beaconcontroller.devicemanager.dao;

import net.beaconcontroller.devicemanager.Device;

/**
 * The DAO for Device Manager, are functions are required to be threadsafe.
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public interface IDeviceManagerDao {

    /**
     * Adds a new device
     * @param device
     */
    public void addDevice(Device device);

    /**
     * Get a device based on its source mac address
     * @param dlAddress
     * @return
     */
    public Device getDevice(byte[] dlAddress);

    /**
     * Removes a device
     * @param device
     */
    public void removeDevice(Device device);

    /**
     * Update a device
     * @param device
     */
    public void updateDevice(Device device);
}
