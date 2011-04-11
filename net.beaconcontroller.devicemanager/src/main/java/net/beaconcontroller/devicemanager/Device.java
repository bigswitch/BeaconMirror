package net.beaconcontroller.devicemanager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.beaconcontroller.packet.IPv4;
import net.beaconcontroller.topology.SwitchPortTuple;

import org.openflow.util.HexString;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class Device {
    protected byte[] dataLayerAddress;
    Map<Integer, DeviceNetworkAddress> networkAddresses;
    Map<SwitchPortTuple, DeviceAttachmentPoint> attachmentPoints;

    public Device() {
        this.networkAddresses = new ConcurrentHashMap<Integer, DeviceNetworkAddress>();
        this.attachmentPoints = new ConcurrentHashMap<SwitchPortTuple, DeviceAttachmentPoint>();
    }

    /**
     * @return the dataLayerAddress
     */
    public byte[] getDataLayerAddress() {
        return dataLayerAddress;
    }

    /**
     * @param dataLayerAddress the dataLayerAddress to set
     */
    public void setDataLayerAddress(byte[] dataLayerAddress) {
        this.dataLayerAddress = dataLayerAddress;
    }

    /**
     * @return the swPorts
     */
    public Collection<DeviceAttachmentPoint> getAttachmentPoints() {
        return attachmentPoints.values();
    }

    public DeviceAttachmentPoint getAttachmentPoint(SwitchPortTuple switchPort) {
        return attachmentPoints.get(switchPort);
    }
    
    public void addAttachmentPoint(DeviceAttachmentPoint attachmentPoint) {
        attachmentPoints.put(attachmentPoint.getSwitchPort(), attachmentPoint);
    }
    
    public void addAttachmentPoint(SwitchPortTuple switchPort, Date lastSeen) {
        DeviceAttachmentPoint attachmentPoint = new DeviceAttachmentPoint(switchPort, lastSeen);
        attachmentPoints.put(switchPort, attachmentPoint);
    }
    
    public DeviceAttachmentPoint removeAttachmentPoint(DeviceAttachmentPoint attachmentPoint) {
        return attachmentPoints.remove(attachmentPoint.getSwitchPort());
    }
    
    public DeviceAttachmentPoint removeAttachmentPoint(SwitchPortTuple switchPort) {
        return attachmentPoints.remove(switchPort);
    }
    
    /**
     * @param attachmentPoints the new collection of attachment points for the device
     */
    public void setAttachmentPoints(Collection<DeviceAttachmentPoint> attachmentPoints) {
        // FIXME: Should we really be exposing this method? Who would use it?
        this.attachmentPoints = new ConcurrentHashMap<SwitchPortTuple, DeviceAttachmentPoint>();
        for (DeviceAttachmentPoint attachmentPoint: attachmentPoints) {
            assert(attachmentPoint.getSwitchPort() != null);
            this.attachmentPoints.put(attachmentPoint.getSwitchPort(), attachmentPoint);
        }
    }

    /**
     * @return the networkAddresses
     */
    public Collection<DeviceNetworkAddress> getNetworkAddresses() {
        return networkAddresses.values();
    }

    public DeviceNetworkAddress getNetworkAddress(Integer networkAddress) {
        return networkAddresses.get(networkAddress);
    }
    
    public void addNetworkAddress(DeviceNetworkAddress networkAddress) {
        networkAddresses.put(networkAddress.getNetworkAddress(), networkAddress);
    }
    
    public void addNetworkAddress(Integer networkAddress, Date lastSeen) {
        DeviceNetworkAddress deviceNetworkAddress = new DeviceNetworkAddress(networkAddress, lastSeen);
        networkAddresses.put(networkAddress, deviceNetworkAddress);
    }

    public DeviceNetworkAddress removeNetworkAddress(Integer networkAddress) {
        return networkAddresses.remove(networkAddress);
    }

    public DeviceNetworkAddress removeNetworkAddress(DeviceNetworkAddress networkAddress) {
        return networkAddresses.remove(networkAddress.getNetworkAddress());
    }
    
    /**
     * @param networkAddresses the networkAddresses to set
     */
    public void setNetworkAddresses(Collection<DeviceNetworkAddress> networkAddresses) {
        // FIXME: Should we really be exposing this method? Who would use it?
        this.networkAddresses = new ConcurrentHashMap<Integer, DeviceNetworkAddress>();
        for (DeviceNetworkAddress networkAddress: networkAddresses) {
            assert(networkAddress.getNetworkAddress() != null);
            this.networkAddresses.put(networkAddress.getNetworkAddress(), networkAddress);
        }
    }
    
//    /**
//     * Updates the last seen timestamp for the device
//     */
//    public void updateLastSeen() {
//        shouldUpdateLastSeenToDb = false;
//        lastSeen.setTime(System.currentTimeMillis());
//        /*
//         * We only update the DB if it has not been updated in an hour.
//         * This is done in order to prevent too many writes to the DB.
//         * TODO - Alex - find a better update strategy. This one will
//         * not work well in some edge cases (i.e. a flow starts at
//         * minute 59).
//         */
//        if ((lastSeenInDb == null) || (lastSeen.getTime() - lastSeenInDb.getTime()) > 3600000) {
//            if (lastSeenInDb == null) {
//                lastSeenInDb = new Date(lastSeen.getTime());
//            } else {
//                lastSeenInDb.setTime(lastSeen.getTime());
//            }
//            shouldUpdateLastSeenToDb = true;
//        }
//    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 2633;
        int result = 1;
        result = prime * result + Arrays.hashCode(dataLayerAddress);
        result = prime * result + ((networkAddresses == null) ? 0 : networkAddresses.hashCode());
        result = prime * result + ((attachmentPoints == null) ? 0 : attachmentPoints.hashCode());
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
        if (!(obj instanceof Device))
            return false;
        Device other = (Device) obj;
        if (!Arrays.equals(dataLayerAddress, other.dataLayerAddress))
            return false;
        if (networkAddresses == null) {
            if (other.networkAddresses != null)
                return false;
        } else if (!networkAddresses.equals(other.networkAddresses))
            return false;
        if (attachmentPoints == null) {
            if (other.attachmentPoints != null)
                return false;
        } else if (!attachmentPoints.equals(other.attachmentPoints))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        
        return "Device [dataLayerAddress=" + 
                ((dataLayerAddress == null) ? "null" : HexString.toHexString(dataLayerAddress)) +
                ", attachmentPoints=" + attachmentPoints + ", networkAddresses="
                + IPv4.fromIPv4AddressCollection(networkAddresses.keySet()) + "]";
    }
}
