/**
 *
 */
package net.beaconcontroller.topology;

import net.beaconcontroller.core.IOFSwitch;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.util.HexString;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class SwitchPortTuple {
    protected IOFSwitch sw;
    protected Short portNumber;
    protected Integer portState;

    public SwitchPortTuple(IOFSwitch sw, Short portNumber) {
        super();
        this.sw = sw;
        this.portNumber = portNumber;
        OFPhysicalPort port = sw.getPort(portNumber);
        if (port != null)
            this.portState = port.getState();
    }

    /**
     * Convenience constructor, port is immediately cast to a short
     * @param id
     * @param port
     */
    public SwitchPortTuple(IOFSwitch sw, Integer port) {
        this(sw, port.shortValue());
    }

    public SwitchPortTuple(IOFSwitch sw, Short portNumber, Integer portState) {
        super();
        this.sw = sw;
        this.portNumber = portNumber;
        this.portState = portState;
    }

    public SwitchPortTuple(IOFSwitch sw, Integer portNumber, Integer portState) {
        this(sw, portNumber.shortValue(), portState);
    }

    /**
     * @return the sw
     */
    public IOFSwitch getSw() {
        return sw;
    }

    /**
     * @return the port number
     */
    public Short getPortNumber() {
        return portNumber;
    }

    /**
     * @return the port state
     */
    public Integer getPortState() {
        return portState;
    }
    
    /**
     * Set the port state
     * @param portState
     */
    public void setPortState(Integer portState) {
        this.portState = portState;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 5557;
        int result = 1;
        result = prime * result + ((sw == null) ? 0 : sw.hashCode());
        result = prime * result + ((portNumber == null) ? 0 : portNumber.hashCode());
        result = prime * result + ((portState == null) ? 0 : portState.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SwitchPortTuple))
            return false;
        SwitchPortTuple other = (SwitchPortTuple) obj;
        if (sw == null) {
            if (other.sw != null)
                return false;
        } else if (!sw.equals(other.sw))
            return false;
        
        if (portNumber == null) {
            if (other.portNumber != null)
                return false;
        } else if (!portNumber.equals(other.portNumber))
            return false;
        
        if (portState == null) {
            if (other.portState != null)
                return false;
        } else if (!portState.equals(other.portState))
            return false;

        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SwitchPortTuple [id="
                + ((sw == null) ? "null" : HexString.toHexString(sw.getId()))
                + ", portNumber=" + ((portNumber == null) ? "null" : (0xff & portNumber))
                + ", portState=" + ((portState == null) ? "null" : portState) + "]";
    }
}
