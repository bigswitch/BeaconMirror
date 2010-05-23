package net.beaconcontroller.core;

import org.openflow.protocol.OFMessage;

public interface IOFMessageListener {
    public enum Command {
        CONTINUE, STOP
    }

  /**
   * This is the method Beacon uses to call listeners with OpenFlow messages
   * @param sw the OpenFlow switch that sent this message
   * @param msg the message
   * @return the command to continue or stop the execution
   */
  public Command receive(IOFSwitch sw, OFMessage msg);

  /**
   * The name assigned to this listener
   * @return
   */
  public String getName();
}
