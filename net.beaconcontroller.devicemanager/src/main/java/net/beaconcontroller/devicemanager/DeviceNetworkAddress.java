package net.beaconcontroller.devicemanager;

import java.util.Date;

public class DeviceNetworkAddress {
    private Integer networkAddress;
    private Date lastSeen;
    private Date lastSeenInStorage;
    private Date lastWrittenToStorage;

    // Interval is in milliseconds, defaulting to 1 hour
    // FIXME: Should this be more frequent?
    private static long LAST_SEEN_STORAGE_UPDATE_INTERVAL = 3600000;

    public DeviceNetworkAddress(Integer networkAddress, Date lastSeen) {
        this.networkAddress = networkAddress;
        this.lastSeen = lastSeen;
        this.lastSeenInStorage = null;
        this.lastWrittenToStorage = null;
    }
    
    public Integer getNetworkAddress() {
        return networkAddress;
    }
    
    // FIXME: The methods below dealing with the lastSeen value are duplicated
    // from the DeviceAttachmentPoint code. Should refactor so code is shared.
    
    public Date getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public void lastSeenWrittenToStorage(Date lastWrittenDate) {
        lastSeenInStorage = lastSeen;
        lastWrittenToStorage = lastWrittenDate;
    }
    
    public boolean shouldWriteLastSeenToStorage(Date currentDate) {
        return (lastSeen != lastSeenInStorage) && ((lastWrittenToStorage == null) ||
                (lastSeen.getTime() >= lastWrittenToStorage.getTime() + LAST_SEEN_STORAGE_UPDATE_INTERVAL));
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 2633;
        int result = 1;
        result = prime * result + ((networkAddress == null) ? 0 : networkAddress.hashCode());
        // FIXME: Should we be including last_seen here?
        //result = prime * result + ((lastSeen == null) ? 0 : lastSeen.hashCode());
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
        if (!(obj instanceof DeviceNetworkAddress))
            return false;
        DeviceNetworkAddress other = (DeviceNetworkAddress) obj;
        if (networkAddress == null) {
            if (other.networkAddress != null)
                return false;
        } else if (!networkAddress.equals(other.networkAddress))
            return false;
        // FIXME: Should we be including last_seen here?
        //if (lastSeen == null) {
        //    if (other.lastSeen != null)
        //        return false;
        //} else if (!lastSeen.equals(other.lastSeen))
        //    return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Device Network Address [networkAddress=" + ((networkAddress == null) ? "null" : networkAddress) + "]";
    }
}
