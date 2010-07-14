/**
 *
 */
package net.beaconcontroller.topology;

/**
 * @author derickso
 *
 */
public class IdPortTuple {
    protected Long id;
    protected Short port;

    public IdPortTuple(Long id, Short port) {
        super();
        this.id = id;
        this.port = port;
    }

    /**
     * Convenience constructor, port is immediately cast to a short
     * @param id
     * @param port
     */
    public IdPortTuple(Long id, Integer port) {
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
        if (!(obj instanceof IdPortTuple))
            return false;
        IdPortTuple other = (IdPortTuple) obj;
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
        return "IdPortTuple [id=" + id + ", port=" + port + "]";
    }
}
