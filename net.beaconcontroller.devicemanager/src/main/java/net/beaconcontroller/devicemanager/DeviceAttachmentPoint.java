package net.beaconcontroller.devicemanager;

import java.util.Date;

import net.beaconcontroller.topology.SwitchPortTuple;

public class DeviceAttachmentPoint {
    
    private SwitchPortTuple switchPort;
    private Date lastSeen;
    private Date lastSeenInStorage;
    private Date lastWrittenToStorage;
    
    // Interval is in milliseconds, defaulting to 1 hour
    // FIXME: Should this be more frequent?
    private static long LAST_SEEN_STORAGE_UPDATE_INTERVAL = 3600000;
    
    public DeviceAttachmentPoint(SwitchPortTuple switchPort, Date lastSeen) {
        this.switchPort = switchPort;
        this.lastSeen = lastSeen;
        this.lastSeenInStorage = null;
        this.lastWrittenToStorage = null;
    }
    
    public SwitchPortTuple getSwitchPort() {
        return switchPort;
    }
    
    //void setSwitchPort(SwitchPortTuple switchPort) {
    //    // Intentionally not public. Only intended to be called by DeviceManagerImpl
    //    this.switchPort = switchPort;
    //}
    
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
        result = prime * result + ((switchPort == null) ? 0 : switchPort.hashCode());
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
        if (!(obj instanceof DeviceAttachmentPoint))
            return false;
        DeviceAttachmentPoint other = (DeviceAttachmentPoint) obj;
        if (switchPort == null) {
            if (other.switchPort != null)
                return false;
        } else if (!switchPort.equals(other.switchPort))
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
        return "Device Attachment Point [switchPort=" + switchPort + "]";
    }
}
