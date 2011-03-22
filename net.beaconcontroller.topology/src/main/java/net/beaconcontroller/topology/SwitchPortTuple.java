/**
 *
 */
package net.beaconcontroller.topology;

import net.beaconcontroller.core.IOFSwitch;
import org.openflow.util.HexString;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class SwitchPortTuple {
    protected IOFSwitch sw;
    protected Short port;

    public SwitchPortTuple(IOFSwitch sw, Short port) {
        super();
        this.sw = sw;
        this.port = port;
    }

    /**
     * Convenience constructor, port is immediately cast to a short
     * @param id
     * @param port
     */
    public SwitchPortTuple(IOFSwitch sw, Integer port) {
        this(sw, port.shortValue());
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
    public Short getPort() {
        return port;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 5557;
        int result = 1;
        result = prime * result + ((sw == null) ? 0 : sw.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
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
        
        if (port == null) {
            if (other.port != null)
                return false;
        } else if (!port.equals(other.port))
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
                + ", port=" + ((port == null) ? "null" : (0xff & port)) + "]";
    }
}
