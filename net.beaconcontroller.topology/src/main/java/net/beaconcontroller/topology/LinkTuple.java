package net.beaconcontroller.topology;

import java.nio.ByteBuffer;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class LinkTuple {
    protected IdPortTuple src;
    protected IdPortTuple dst;

    /**
     * @param src
     * @param dst
     */
    public LinkTuple(IdPortTuple src, IdPortTuple dst) {
        this.src = src;
        this.dst = dst;
    }

    public LinkTuple(Long srcId, Short srcPort, Long dstId, Short dstPort) {
        this.src = new IdPortTuple(srcId, srcPort);
        this.dst = new IdPortTuple(dstId, dstPort);
    }

    /**
     * Convenience constructor, ports are cast to shorts
     * @param srcId
     * @param srcPort
     * @param dstId
     * @param dstPort
     */
    public LinkTuple(Long srcId, Integer srcPort, Long dstId, Integer dstPort) {
        this.src = new IdPortTuple(srcId, srcPort);
        this.dst = new IdPortTuple(dstId, dstPort);
    }

    /**
     * @return the src
     */
    public IdPortTuple getSrc() {
        return src;
    }

    /**
     * @param src the src to set
     */
    public void setSrc(IdPortTuple src) {
        this.src = src;
    }

    /**
     * @return the dst
     */
    public IdPortTuple getDst() {
        return dst;
    }

    /**
     * @param dst the dst to set
     */
    public void setDst(IdPortTuple dst) {
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

    public byte[] toBytes() {
        byte[] ret = new byte[20];
        ByteBuffer bb = ByteBuffer.wrap(ret);
        bb.put(src.toBytes());
        bb.put(dst.toBytes());
        return ret;
    }

    public static LinkTuple fromBytes(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        long srcId = bb.getLong();
        short srcPort = bb.getShort();
        long dstId = bb.getLong();
        short dstPort = bb.getShort();
        return new LinkTuple(srcId, srcPort, dstId, dstPort);
    }
}
