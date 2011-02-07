package net.beaconcontroller.devicemanager;

import net.beaconcontroller.core.IOFSwitch;

/**
 * Implementors of this interface can receive updates from DeviceManager about
 * the state of devices under its control.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface IDeviceManagerAware {
    /**
     * Called when a new Device is found
     * @param device
     */
    public void deviceAdded(Device device);

    /**
     * Called when a Device is removed, this typically occurs when the port the
     * Device is attached to goes down, or the switch it is attached to is
     * removed.
     * @param device
     */
    public void deviceRemoved(Device device);

    /**
     * Called when a Device has moved to a new location on the network. Note
     * that either the switch or the port or both has changed.
     *
     * @param device the device
     * @param oldSw the old switch
     * @param oldPort the port on the old switch
     * @param sw the current switch
     * @param port the current port on the current switch
     */
    public void deviceMoved(Device device, IOFSwitch oldSw, Short oldPort,
            IOFSwitch sw, Short port);
}
