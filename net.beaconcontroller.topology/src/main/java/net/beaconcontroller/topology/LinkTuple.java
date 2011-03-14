package net.beaconcontroller.topology;

import net.beaconcontroller.core.IOFSwitch;
import org.openflow.protocol.OFPhysicalPort.OFPortState;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class LinkTuple {
    protected SwitchPortTuple src;
    protected SwitchPortTuple dst;

    /**
     * @param src
     * @param dst
     */
    public LinkTuple(SwitchPortTuple src, SwitchPortTuple dst) {
        this.src = src;
        this.dst = dst;
    }

    public LinkTuple(IOFSwitch src, Short srcPortNumber, Integer srcPortState,
            IOFSwitch dst, Short dstPortNumber, Integer dstPortState) {
        this.src = new SwitchPortTuple(src, srcPortNumber, srcPortState);
        this.dst = new SwitchPortTuple(dst, dstPortNumber, dstPortState);
    }

    /**
     * Convenience constructor, ports are cast to shorts
     * @param srcId
     * @param srcPortNumber
     * @param dstId
     * @param dstPortNumber
     */
    public LinkTuple(IOFSwitch src, Integer srcPortNumber, Integer srcPortState,
            IOFSwitch dst, Integer dstPortNumber, Integer dstPortState) {
        this.src = new SwitchPortTuple(src, srcPortNumber, srcPortState);
        this.dst = new SwitchPortTuple(dst, dstPortNumber, dstPortState);
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

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 2221;
        int result = 1;
        result = prime * result + ((dst == null) ? 0 : dst.hashCode());
        result = prime * result + ((src == null) ? 0 : src.hashCode());
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
        if (src == null) {
            if (other.src != null)
                return false;
        } else if (!src.equals(other.src))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "LinkTuple [src=" + src + ", dst=" + dst + "]";
    }
}
