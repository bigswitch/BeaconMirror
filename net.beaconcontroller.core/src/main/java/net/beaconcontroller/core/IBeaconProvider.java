package net.beaconcontroller.core;

import java.util.Map;

import org.openflow.protocol.OFType;

public interface IBeaconProvider {
  /**
   * 
   * @param type
   * @param listener
   */
  public void addOFMessageListener(OFType type, IOFMessageListener listener);

  /**
   * 
   * @param type
   * @param listener
   */
  public void removeOFMessageListener(OFType type, IOFMessageListener listener);

  /**
   * Returns a list of all actively connected OpenFlow switches
   * @return the set of connected switches
   */
  public Map<Long, IOFSwitch> getSwitches();

  /**
   * Add a switch listener
   * @param listener
   */
  public void addOFSwitchListener(IOFSwitchListener listener);

  /**
   * Remove a switch listener
   * @param listener
   */
  public void removeOFSwitchListener(IOFSwitchListener listener);
}
