package net.beaconcontroller.packet;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class Data extends BasePacket {
    protected byte[] data;

    /**
     * 
     */
    public Data() {
    }

    /**
     * @param data
     */
    public Data(byte[] data) {
        this.data = data;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public Data setData(byte[] data) {
        this.data = data;
        return this;
    }

    public byte[] serialize() {
        return this.data;
    }
}
