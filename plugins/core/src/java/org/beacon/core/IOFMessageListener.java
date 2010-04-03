package org.beacon.core;

import org.openflow.protocol.OFMessage;

public interface IOFMessageListener<E extends OFMessage> {

  /**
   * 
   * @param sw
   * @param msg
   */
  public void receive(IOFSwitch sw, E msg);

  /**
   * 
   * @return
   */
  public String getName();
}
