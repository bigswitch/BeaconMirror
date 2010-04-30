package net.beaconcontroller.packet;

/**
*
* @author David Erickson (derickso@stanford.edu)
*/
public interface IPacket {
    /**
     * 
     * @return
     */
    public IPacket getPayload();

    /**
     * 
     * @param packet
     * @return
     */
    public IPacket setPayload(IPacket packet);

    /**
     * 
     * @return
     */
    public IPacket getParent();

    /**
     * 
     * @param packet
     * @return
     */
    public IPacket setParent(IPacket packet);

    /**
     * Sets all payloads parent packet if applicable, then serializes this 
     * packet and all payloads
     * @return a byte[] containing this packet and payloads
     */
    public byte[] serialize();
}
