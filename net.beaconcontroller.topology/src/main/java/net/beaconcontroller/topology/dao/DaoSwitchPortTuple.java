/**
 *
 */
package net.beaconcontroller.topology.dao;

import java.nio.ByteBuffer;

import org.openflow.util.HexString;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class DaoSwitchPortTuple {
    protected Long id;
    protected Short portNumber;
    protected Integer portState;
    
    public DaoSwitchPortTuple(Long id, Short portNumber, Integer portState) {
        super();
        this.id = id;
        this.portNumber = portNumber;
        this.portState = portState;
    }

    /**
     * Convenience constructor, port is immediately cast to a short
     * @param id
     * @param port
     */
    public DaoSwitchPortTuple(Long id, Integer portNumber, Integer portState) {
        this(id, portNumber.shortValue(), portState);
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the port
     */
    public Short getPortNumber() {
        return portNumber;
    }

    public Integer getPortState() {
        return portState;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 5557;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        if (!(obj instanceof DaoSwitchPortTuple))
            return false;
        DaoSwitchPortTuple other = (DaoSwitchPortTuple) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
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
        return "DaoSwitchPortTuple [id="
                + ((id == null) ? "null" : HexString.toHexString(id))
                + ", portNumber=" + ((portNumber == null) ? "null" : (0xff & portNumber))
                + ", portState=" + ((portState == null) ? "null" : portState) + "]";
    }

    public byte[] toBytes() {
        byte[] ret = new byte[10];
        ByteBuffer bb = ByteBuffer.wrap(ret);
        bb.putLong(id);
        bb.putShort(portNumber);
        bb.putInt(portState);
        return ret;
    }

    public static DaoSwitchPortTuple fromBytes(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        long id = bb.getLong();
        short portNumber = bb.getShort();
        int portState = bb.getInt();
        return new DaoSwitchPortTuple(id, portNumber, portState);
    }
}
