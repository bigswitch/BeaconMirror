package net.beaconcontroller.core;


public interface IOFSwitchListener {

    /**
     * Fired when a switch is connected to the controller, and has sent
     * a features reply.
     * @param sw
     */
    public void addedSwitch(IOFSwitch sw);

    /**
     * Fired when a switch is disconnected from the controller.
     * @param sw
     */
    public void removedSwitch(IOFSwitch sw);
    
    /**
     * The name assigned to this listener
     * @return
     */
    public String getName();
}
