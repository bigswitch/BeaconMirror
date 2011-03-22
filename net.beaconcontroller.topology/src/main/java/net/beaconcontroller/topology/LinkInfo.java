package net.beaconcontroller.topology;

public class LinkInfo {
    protected Long validTime;

    /** The port states stored here are topology's last knowledge of
     * the state of the port. This mostly mirrors the state
     * maintained in the port list in IOFSwitch (i.e. the one returned
     * from getPort), except that during a port status message the
     * IOFSwitch port state will already have been updated with the
     * new port state, so topology needs to keep its own copy so that
     * it can determine if the port state has changed and therefore
     * requires the new state to be written to storage and the clusters
     * recomputed.
     */
    protected Integer srcPortState;
    protected Integer dstPortState;
    
    public LinkInfo(Long validTime, Integer srcPortState, Integer dstPortState) {
        this.validTime = validTime;
        this.srcPortState = srcPortState;
        this.dstPortState = dstPortState;
    }

    public Long getValidTime() {
        return validTime;
    }
    
    public void setValidTime(Long validTime) {
        this.validTime = validTime;
    }
    
    public Integer getSrcPortState() {
        return srcPortState;
    }
    
    public void setSrcPortState(Integer srcPortState) {
        this.srcPortState = srcPortState;
    }
    
    public Integer getDstPortState() {
        return dstPortState;
    }
    
    public void setDstPortState(int dstPortState) {
        this.dstPortState = dstPortState;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 5557;
        int result = 1;
        result = prime * result + ((validTime == null) ? 0 : validTime.hashCode());
        result = prime * result + ((srcPortState == null) ? 0 : validTime.hashCode());
        result = prime * result + ((dstPortState == null) ? 0 : dstPortState.hashCode());
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
        if (!(obj instanceof LinkInfo))
            return false;
        LinkInfo other = (LinkInfo) obj;
        if (validTime == null) {
            if (other.validTime != null)
                return false;
        } else if (!validTime.equals(other.validTime))
            return false;
        
        if (srcPortState == null) {
            if (other.srcPortState != null)
                return false;
        } else if (!srcPortState.equals(other.srcPortState))
            return false;

        if (dstPortState == null) {
            if (other.dstPortState != null)
                return false;
        } else if (!dstPortState.equals(other.dstPortState))
            return false;

        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "LinkInfo [validTime=" + ((validTime == null) ? "null" : validTime)
                + ", srcPortState=" + ((srcPortState == null) ? "null" : srcPortState)
                + ", dstPortState=" + ((dstPortState == null) ? "null" : srcPortState) + "]";
    }
}
