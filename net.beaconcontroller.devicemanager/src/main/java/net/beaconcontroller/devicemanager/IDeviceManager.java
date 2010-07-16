package net.beaconcontroller.devicemanager;

public interface IDeviceManager {
    /**
     * Returns a device for the given data layer address hash code, created by
     * Arrays.hashCode(byte[]), if one exists.
     * @param hashCode
     * @return
     */
    public Device getDeviceByDataLayerAddress(Integer hashCode);
}
