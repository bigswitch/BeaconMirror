package net.beaconcontroller.topology;

import net.beaconcontroller.core.IOFSwitch;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class LinkTuple {
    protected SwitchPortTuple src;
    protected Integer srcPortState;
    protected SwitchPortTuple dst;
    protected Integer dstPortState;

    /**
     * @param src
     * @param srcPortState
     * @param dst
     * @param dstPortState
     */
    public LinkTuple(SwitchPortTuple src, Integer srcPortState, SwitchPortTuple dst, Integer dstPortState) {
        this.src = src;
        this.srcPortState = srcPortState;
        this.dst = dst;
        this.dstPortState = dstPortState;
    }

    public LinkTuple(IOFSwitch src, Short srcPortNumber, Integer srcPortState,
            IOFSwitch dst, Short dstPortNumber, Integer dstPortState) {
        this.src = new SwitchPortTuple(src, srcPortNumber);
        this.srcPortState = srcPortState;
        this.dst = new SwitchPortTuple(dst, dstPortNumber);
        this.dstPortState = dstPortState;
    }

    /**
     * Convenience constructor, ports are cast to shorts
     * @param srcId
     * @param srcPortNumber
     * @param srcPortState
     * @param dstId
     * @param dstPortNumber
     * @param dstPortState
     */
    public LinkTuple(IOFSwitch src, Integer srcPortNumber, Integer srcPortState,
            IOFSwitch dst, Integer dstPortNumber, Integer dstPortState) {
        this.src = new SwitchPortTuple(src, srcPortNumber);
        this.srcPortState = srcPortState;
        this.dst = new SwitchPortTuple(dst, dstPortNumber);
        this.dstPortState = dstPortState;
    }

    /**
     * @return the src
     */
    public SwitchPortTuple getSrc() {
        return src;
    }

    /**
     * @param src the src to set
     */
    public void setSrc(SwitchPortTuple src) {
        this.src = src;
    }

    public Integer getSrcPortState() {
        return srcPortState;
    }
    
    public void setSrcPortState(Integer srcPortState) {
        this.srcPortState = srcPortState;
    }
    
    /**
     * @return the dst
     */
    public SwitchPortTuple getDst() {
        return dst;
    }

    /**
     * @param dst the dst to set
     */
    public void setDst(SwitchPortTuple dst) {
        this.dst = dst;
    }

    public Integer getDstPortState() {
        return dstPortState;
    }
    
    public void setDstPortState(Integer dstPortState) {
        this.dstPortState = dstPortState;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 2221;
        int result = 1;
        result = prime * result + ((dst == null) ? 0 : dst.hashCode());
        result = prime * result + ((dstPortState == null) ? 0 : dstPortState.hashCode());
        result = prime * result + ((src == null) ? 0 : src.hashCode());
        result = prime * result + ((srcPortState == null) ? 0 : srcPortState.hashCode());
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
        if (dst == null) {
            if (other.dst != null)
                return false;
        } else if (!dst.equals(other.dst))
            return false;
        if (dstPortState == null) {
            if (other.dstPortState != null)
                return false;
        } else if (!dstPortState.equals(other.dstPortState))
            return false;
        if (src == null) {
            if (other.src != null)
                return false;
        } else if (!src.equals(other.src))
            return false;
        if (srcPortState == null) {
            if (other.srcPortState != null)
                return false;
        } else if (!srcPortState.equals(other.srcPortState))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "LinkTuple [src=" + src + ", srcPortState=" + srcPortState +
            ",dst=" + dst + ", dstPortState=" + dstPortState + "]";
    }
}
