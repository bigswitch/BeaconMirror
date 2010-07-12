package net.beaconcontroller.topology;

public class LinkTuple {
    protected Long srcId;
    protected Short srcPort;
    protected Long dstId;
    protected Short dstPort;

    public LinkTuple(Long srcId, Short srcPort, Long dstId, Short dstPort) {
        super();
        this.srcId = srcId;
        this.srcPort = srcPort;
        this.dstId = dstId;
        this.dstPort = dstPort;
    }

    /**
     * @return the srcId
     */
    public Long getSrcId() {
        return srcId;
    }

    /**
     * @return the srcPort
     */
    public Short getSrcPort() {
        return srcPort;
    }

    /**
     * @return the dstId
     */
    public Long getDstId() {
        return dstId;
    }

    /**
     * @return the dstPort
     */
    public Short getDstPort() {
        return dstPort;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 2221;
        int result = 1;
        result = prime * result + ((dstId == null) ? 0 : dstId.hashCode());
        result = prime * result + ((dstPort == null) ? 0 : dstPort.hashCode());
        result = prime * result + ((srcId == null) ? 0 : srcId.hashCode());
        result = prime * result + ((srcPort == null) ? 0 : srcPort.hashCode());
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
        if (!(obj instanceof LinkTuple))
            return false;
        LinkTuple other = (LinkTuple) obj;
        if (dstId == null) {
            if (other.dstId != null)
                return false;
        } else if (!dstId.equals(other.dstId))
            return false;
        if (dstPort == null) {
            if (other.dstPort != null)
                return false;
        } else if (!dstPort.equals(other.dstPort))
            return false;
        if (srcId == null) {
            if (other.srcId != null)
                return false;
        } else if (!srcId.equals(other.srcId))
            return false;
        if (srcPort == null) {
            if (other.srcPort != null)
                return false;
        } else if (!srcPort.equals(other.srcPort))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "LinkTuple [dstId=" + dstId + ", dstPort=" + dstPort
                + ", srcId=" + srcId + ", srcPort=" + srcPort + "]";
    }
}
