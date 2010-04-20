package net.beaconcontroller.core;

import org.openflow.protocol.OFMessage;

public interface IOFMessageListener {

  /**
   * 
   * @param sw
   * @param msg
   */
  public void receive(IOFSwitch sw, OFMessage msg);

  /**
   * 
   * @return
   */
  public String getName();
}
