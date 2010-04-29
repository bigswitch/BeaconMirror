package net.beaconcontroller.packet;

/**
*
* @author David Erickson (derickso@stanford.edu)
*/
public abstract class BasePacket implements IPacket {
    protected IPacket parent;
    protected IPacket payload;

    /**
     * @return the parent
     */
    public IPacket getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public IPacket setParent(IPacket parent) {
        this.parent = parent;
        return this;
    }

    /**
     * @return the payload
     */
    public IPacket getPayload() {
        return payload;
    }

    /**
     * @param payload the payload to set
     */
    public IPacket setPayload(IPacket payload) {
        this.payload = payload;
        return this;
    }
}
