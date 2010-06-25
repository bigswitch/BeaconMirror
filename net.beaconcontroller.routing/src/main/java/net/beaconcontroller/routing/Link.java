package net.beaconcontroller.routing;

/**
 * Represents a link between two datapaths. It is assumed that
 * Links will generally be held in a list, and that the first datapath's
 * id will be held in a different structure.
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class Link {
    /**
     * Outgoing port number of the current datapath the link connects to
     */
    protected Short outPort;

    /**
     * Destination datapath id
     */
    protected Long dst;

    /**
     * Incoming port number on the dst datapath the link connects to
     */
    protected Short inPort;

    public Link(Short outPort, Short inPort, Long dst) {
        super();
        this.outPort = outPort;
        this.inPort = inPort;
        this.dst = dst;
    }

    /**
     * @return the outPort
     */
    public Short getOutPort() {
        return outPort;
    }

    /**
     * @param outPort the outPort to set
     */
    public void setOutPort(Short outPort) {
        this.outPort = outPort;
    }

    /**
     * @return the dst
     */
    public Long getDst() {
        return dst;
    }

    /**
     * @param dst the dst to set
     */
    public void setDst(Long dst) {
        this.dst = dst;
    }

    /**
     * @return the inPort
     */
    public Short getInPort() {
        return inPort;
    }

    /**
     * @param inPort the inPort to set
     */
    public void setInPort(Short inPort) {
        this.inPort = inPort;
    }

    @Override
    public int hashCode() {
        final int prime = 3203;
        int result = 1;
        result = prime * result + ((dst == null) ? 0 : dst.hashCode());
        result = prime * result + ((inPort == null) ? 0 : inPort.hashCode());
        result = prime * result + ((outPort == null) ? 0 : outPort.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Link other = (Link) obj;
        if (dst == null) {
            if (other.dst != null)
                return false;
        } else if (!dst.equals(other.dst))
            return false;
        if (inPort == null) {
            if (other.inPort != null)
                return false;
        } else if (!inPort.equals(other.inPort))
            return false;
        if (outPort == null) {
            if (other.outPort != null)
                return false;
        } else if (!outPort.equals(other.outPort))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Link [outPort=" + outPort + ", inPort=" + inPort + ", dst="
                + dst + "]";
    }
}
