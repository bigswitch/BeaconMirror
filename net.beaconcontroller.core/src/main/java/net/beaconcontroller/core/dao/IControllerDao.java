package net.beaconcontroller.core.dao;

import net.beaconcontroller.core.IOFController;
import net.beaconcontroller.core.IOFSwitch;
import org.openflow.protocol.OFPhysicalPort;

public interface IControllerDao {
    
    public void startedController(IOFController controller);
    
    public void shutDownController(IOFController controller);
    
    public void addedSwitch(IOFSwitch sw);
    
    public void removedSwitch(IOFSwitch sw);
    
    public void addedPort(IOFSwitch sw, OFPhysicalPort port);
    
    public void modifiedPort(IOFSwitch sw, OFPhysicalPort port);
    
    public void deletedPort(IOFSwitch sw, short portNumber);
}
