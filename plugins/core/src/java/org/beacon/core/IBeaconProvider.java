package org.beacon.core;

import org.openflow.protocol.OFType;

public interface IBeaconProvider {
  /**
   * 
   * @param type
   * @param listener
   */
  public void addListener(OFType type, IOFMessageListener<?> listener);

  /**
   * 
   * @param type
   * @param listener
   */
  public void removeListener(OFType type, IOFMessageListener<?> listener);
}
