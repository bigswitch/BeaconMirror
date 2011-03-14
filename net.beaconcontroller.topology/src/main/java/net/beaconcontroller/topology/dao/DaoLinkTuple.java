package net.beaconcontroller.topology.dao;

import java.nio.ByteBuffer;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class DaoLinkTuple {
    protected DaoSwitchPortTuple src;
    protected DaoSwitchPortTuple dst;

    /**
     * @param src
     * @param dst
     */
    public DaoLinkTuple(DaoSwitchPortTuple src, DaoSwitchPortTuple dst) {
        this.src = src;
        this.dst = dst;
    }

    public DaoLinkTuple(Long srcId, Short srcPortNumber, Integer srcPortState,
            Long dstId, Short dstPortNumber, Integer dstPortState) {
        this.src = new DaoSwitchPortTuple(srcId, srcPortNumber, srcPortState);
        this.dst = new DaoSwitchPortTuple(dstId, dstPortNumber, dstPortState);
    }

    /**
     * Convenience constructor, ports are cast to shorts
     * @param srcId
     * @param srcPort
     * @param dstId
     * @param dstPort
     */
    public DaoLinkTuple(Long srcId, Integer srcPortNumber, Integer srcPortState,
            Long dstId, Integer dstPortNumber, Integer dstPortState) {
        this.src = new DaoSwitchPortTuple(srcId, srcPortNumber, srcPortState);
        this.dst = new DaoSwitchPortTuple(dstId, dstPortNumber, dstPortState);
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
        if (!(obj instanceof DaoLinkTuple))
            return false;
        DaoLinkTuple other = (DaoLinkTuple) obj;
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
        return "DaoLinkTuple [src=" + src + ", dst=" + dst + "]";
    }

    public byte[] toBytes() {
        byte[] ret = new byte[20];
        ByteBuffer bb = ByteBuffer.wrap(ret);
        bb.put(src.toBytes());
        bb.put(dst.toBytes());
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
