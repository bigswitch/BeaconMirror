package net.beaconcontroller.topology.dao;

import java.nio.ByteBuffer;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class DaoLinkTuple {
    protected DaoSwitchPortTuple src;
    protected Integer srcPortState;
    protected DaoSwitchPortTuple dst;
    protected Integer dstPortState;

    /**
     * @param src
     * @param dst
     */
    public DaoLinkTuple(DaoSwitchPortTuple src, Integer srcPortState,
            DaoSwitchPortTuple dst, Integer dstPortState) {
        this.src = src;
        this.srcPortState = srcPortState;
        this.dst = dst;
        this.dstPortState = dstPortState;
    }

    public DaoLinkTuple(Long srcId, Short srcPort, Integer srcPortState,
            Long dstId, Short dstPort, Integer dstPortState) {
        this.src = new DaoSwitchPortTuple(srcId, srcPort);
        this.srcPortState = srcPortState;
        this.dst = new DaoSwitchPortTuple(dstId, dstPort);
        this.dstPortState = dstPortState;
    }

    /**
     * Convenience constructor, ports are cast to shorts
     * @param srcId
     * @param srcPort
     * @param srcPortState
     * @param dstId
     * @param dstPort
     * @param dstPortState
     */
    public DaoLinkTuple(Long srcId, Integer srcPort, Integer srcPortState,
            Long dstId, Integer dstPort, Integer dstPortState) {
        this.src = new DaoSwitchPortTuple(srcId, srcPort);
        this.srcPortState = srcPortState;
        this.dst = new DaoSwitchPortTuple(dstId, dstPort);
        this.dstPortState = dstPortState;
    }

    /**
     * @return the src
     */
    public DaoSwitchPortTuple getSrc() {
        return src;
    }

    /**
     * @param src the src to set
     */
    public void setSrc(DaoSwitchPortTuple src) {
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
    public DaoSwitchPortTuple getDst() {
        return dst;
    }

    /**
     * @param dst the dst to set
     */
    public void setDst(DaoSwitchPortTuple dst) {
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
        if (!(obj instanceof DaoLinkTuple))
            return false;
        DaoLinkTuple other = (DaoLinkTuple) obj;
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
        return "DaoLinkTuple [src=" + src + ", srcPortState=" + srcPortState +
            ", dst=" + dst + ", dstPortState=" + dstPortState + "]";
    }

    public byte[] toBytes() {
        byte[] ret = new byte[20];
        ByteBuffer bb = ByteBuffer.wrap(ret);
        bb.put(src.toBytes());
        bb.putInt(srcPortState);
        bb.put(dst.toBytes());
        bb.putInt(dstPortState);
        return ret;
    }

    public static DaoLinkTuple fromBytes(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        long srcId = bb.getLong();
        short srcPortNumber = bb.getShort();
        int srcPortState = bb.getInt();
        long dstId = bb.getLong();
        short dstPortNumber = bb.getShort();
        int dstPortState = bb.getInt();
        return new DaoLinkTuple(srcId, srcPortNumber, srcPortState,
                dstId, dstPortNumber, dstPortState);
    }
}
