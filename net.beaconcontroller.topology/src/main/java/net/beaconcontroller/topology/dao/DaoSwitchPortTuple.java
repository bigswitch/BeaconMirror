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
    protected Short port;

    public DaoSwitchPortTuple(Long id, Short port) {
        super();
        this.id = id;
        this.port = port;
    }

    /**
     * Convenience constructor, port is immediately cast to a short
     * @param id
     * @param port
     */
    public DaoSwitchPortTuple(Long id, Integer port) {
        super();
        this.id = id;
        this.port = port.shortValue();
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
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        if (!(obj instanceof DaoSwitchPortTuple))
            return false;
        DaoSwitchPortTuple other = (DaoSwitchPortTuple) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
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
        return "DaoSwitchPortTuple [id="
                + ((id == null) ? "null" : HexString.toHexString(id))
                + ", port=" + ((port == null) ? "null" : (0xff & port)) + "]";
    }

    public byte[] toBytes() {
        byte[] ret = new byte[10];
        ByteBuffer bb = ByteBuffer.wrap(ret);
        bb.putLong(id);
        bb.putShort(port);
        return ret;
    }

    public static DaoSwitchPortTuple fromBytes(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        long id = bb.getLong();
        short port = bb.getShort();
        return new DaoSwitchPortTuple(id, port);
    }
}
