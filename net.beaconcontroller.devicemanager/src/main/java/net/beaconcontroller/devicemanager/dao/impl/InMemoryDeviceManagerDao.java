package net.beaconcontroller.devicemanager.dao.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.dao.IDeviceManagerDao;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class InMemoryDeviceManagerDao implements IDeviceManagerDao {
    protected Map<Integer, Device> dataLayerAddressDeviceMap;

    public InMemoryDeviceManagerDao() {
        this.dataLayerAddressDeviceMap = new ConcurrentHashMap<Integer, Device>();
    }

    /* (non-Javadoc)
     * @see net.beaconcontroller.devicemanager.dao.IDeviceManagerDao#addDevice(net.beaconcontroller.devicemanager.Device)
     */
    @Override
    public void addDevice(Device device) {
        dataLayerAddressDeviceMap.put(Arrays.hashCode(device.getDataLayerAddress()), device);
    }

    /* (non-Javadoc)
     * @see net.beaconcontroller.devicemanager.dao.IDeviceManagerDao#getDevice(byte[])
     */
    @Override
    public Device getDevice(byte[] dlAddress) {
        return dataLayerAddressDeviceMap.get(Arrays.hashCode(dlAddress));
    }

    /* (non-Javadoc)
     * @see net.beaconcontroller.devicemanager.dao.IDeviceManagerDao#removeDevice(net.beaconcontroller.devicemanager.Device)
     */
    @Override
    public void removeDevice(Device device) {
        dataLayerAddressDeviceMap.remove(Arrays.hashCode(device.getDataLayerAddress()));
    }

    /* (non-Javadoc)
     * @see net.beaconcontroller.devicemanager.dao.IDeviceManagerDao#updateDevice(net.beaconcontroller.devicemanager.Device)
     */
    @Override
    public void updateDevice(Device device) {
        dataLayerAddressDeviceMap.put(Arrays.hashCode(device.getDataLayerAddress()), device);
    }
}
