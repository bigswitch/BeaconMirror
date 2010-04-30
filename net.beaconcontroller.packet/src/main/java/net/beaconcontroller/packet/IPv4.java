/**
 * 
 */
package net.beaconcontroller.packet;

import java.nio.ByteBuffer;

/**
 * @author derickso
 *
 */
public class IPv4 extends BasePacket {
    public static byte PROTOCOL_UDP = 0x11;

    protected byte version;
    protected byte headerLength;
    protected byte diffServ;
    protected short totalLength;
    protected short identification;
    protected byte flags;
    protected short fragmentOffset;
    protected byte ttl;
    protected byte protocol;
    protected short checksum;
    protected int sourceAddress;
    protected int destinationAddress;
    protected byte[] options;

    /**
     * Default constructor that sets the version to 4.
     */
    public IPv4() {
        super();
        this.version = 4;
    }

    /**
     * @return the version
     */
    public byte getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public IPv4 setVersion(byte version) {
        this.version = version;
        return this;
    }

    /**
     * @return the headerLength
     */
    public byte getHeaderLength() {
        return headerLength;
    }

    /**
     * @param headerLength the headerLength to set
     */
    public IPv4 setHeaderLength(byte headerLength) {
        this.headerLength = headerLength;
        return this;
    }

    /**
     * @return the diffServ
     */
    public byte getDiffServ() {
        return diffServ;
    }

    /**
     * @param diffServ the diffServ to set
     */
    public IPv4 setDiffServ(byte diffServ) {
        this.diffServ = diffServ;
        return this;
    }

    /**
     * @return the totalLength
     */
    public short getTotalLength() {
        return totalLength;
    }

    /**
     * @param totalLength the totalLength to set
     */
    public IPv4 setTotalLength(short totalLength) {
        this.totalLength = totalLength;
        return this;
    }

    /**
     * @return the identification
     */
    public short getIdentification() {
        return identification;
    }

    /**
     * @param identification the identification to set
     */
    public IPv4 setIdentification(short identification) {
        this.identification = identification;
        return this;
    }

    /**
     * @return the flags
     */
    public byte getFlags() {
        return flags;
    }

    /**
     * @param flags the flags to set
     */
    public IPv4 setFlags(byte flags) {
        this.flags = flags;
        return this;
    }

    /**
     * @return the fragmentOffset
     */
    public short getFragmentOffset() {
        return fragmentOffset;
    }

    /**
     * @param fragmentOffset the fragmentOffset to set
     */
    public IPv4 setFragmentOffset(short fragmentOffset) {
        this.fragmentOffset = fragmentOffset;
        return this;
    }

    /**
     * @return the ttl
     */
    public byte getTtl() {
        return ttl;
    }

    /**
     * @param ttl the ttl to set
     */
    public IPv4 setTtl(byte ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     * @return the protocol
     */
    public byte getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public IPv4 setProtocol(byte protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * @return the checksum
     */
    public short getChecksum() {
        return checksum;
    }

    /**
     * @param checksum the checksum to set
     */
    public IPv4 setChecksum(short checksum) {
        this.checksum = checksum;
        return this;
    }

    /**
     * @return the sourceAddress
     */
    public int getSourceAddress() {
        return sourceAddress;
    }

    /**
     * @param sourceAddress the sourceAddress to set
     */
    public IPv4 setSourceAddress(int sourceAddress) {
        this.sourceAddress = sourceAddress;
        return this;
    }

    /**
     * @param sourceAddress the sourceAddress to set
     */
    public IPv4 setSourceAddress(String sourceAddress) {
        this.sourceAddress = IPv4.toIPv4Address(sourceAddress);
        return this;
    }

    /**
     * @return the destinationAddress
     */
    public int getDestinationAddress() {
        return destinationAddress;
    }

    /**
     * @param destinationAddress the destinationAddress to set
     */
    public IPv4 setDestinationAddress(int destinationAddress) {
        this.destinationAddress = destinationAddress;
        return this;
    }

    /**
     * @param destinationAddress the destinationAddress to set
     */
    public IPv4 setDestinationAddress(String destinationAddress) {
        this.destinationAddress = IPv4.toIPv4Address(destinationAddress);
        return this;
    }

    /**
     * @return the options
     */
    public byte[] getOptions() {
        return options;
    }

    /**
     * @param options the options to set
     */
    public IPv4 setOptions(byte[] options) {
        if (options != null && (options.length % 4) > 0)
            throw new IllegalArgumentException(
                    "Options length must be a multiple of 4");
        this.options = options;
        return this;
    }

    /**
     * Serializes the packet. Will compute and set the following fields if they
     * are set to specific values at the time serialize is called:
     *      -checksum : 0
     *      -headerLength : 0
     *      -totalLength : 0
     */
    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (payload != null) {
            payload.setParent(this);
            payloadData = payload.serialize();
        }

        if (this.headerLength == 0) {
            int optionsLength = 0;
            if (this.options != null)
                optionsLength = this.options.length / 4;
            this.headerLength = (byte) (5 + optionsLength);
        }

        if (this.totalLength == 0) {
            this.totalLength = (short) (this.headerLength * 4 + ((payloadData == null) ? 0
                    : payloadData.length));
        }

        byte[] data = new byte[this.totalLength];
        ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put((byte) (((this.version & 0xf) << 4) | (this.headerLength & 0xf)));
        bb.put(this.diffServ);
        bb.putShort(this.totalLength);
        bb.putShort(this.identification);
        bb.putShort((short) (((this.flags & 0x7) << 29) | (this.fragmentOffset & 0x1fff)));
        bb.put(this.ttl);
        bb.put(this.protocol);
        bb.putShort(this.checksum);
        bb.putInt(this.sourceAddress);
        bb.putInt(this.destinationAddress);
        if (this.options != null)
            bb.put(this.options);
        if (payloadData != null)
            bb.put(payloadData);

        // compute checksum if needed
        if (this.checksum == 0) {
            bb.rewind();
            int accumulation = 0;
            for (int i = 0; i < this.headerLength * 2; ++i) {
                accumulation += 0xffff & bb.getShort();
            }
            accumulation = ((accumulation >> 16) & 0xffff)
                    + (accumulation & 0xffff);
            this.checksum = (short) (~accumulation & 0xffff);
            bb.putShort(10, this.checksum);
        }
        return data;
    }

    /**
     * Accepts an IPv4 address of the form xxx.xxx.xxx.xxx, ie 192.168.0.1 and
     * returns the corresponding 32 bit integer.
     * @param ipAddress
     * @return
     */
    public static int toIPv4Address(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4) 
            throw new IllegalArgumentException("Specified IPv4 address must" +
                "contain 4 sets of numerical digits separated by periods");

        int result = 0;
        for (int i = 0; i < 4; ++i) {
            result |= Integer.valueOf(octets[i]) << ((3-i)*8);
        }
        return result;
    }
}
