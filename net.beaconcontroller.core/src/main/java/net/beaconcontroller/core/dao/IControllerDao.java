package net.beaconcontroller.core.dao;

import net.beaconcontroller.core.IOFController;
import net.beaconcontroller.core.IOFSwitch;

public interface IControllerDao {
    
    public void startedController(IOFController controller);
    
    public void shutDownController(IOFController controller);
    
    public void addedSwitch(IOFSwitch sw);
    
    public void removedSwitch(IOFSwitch sw);
}
