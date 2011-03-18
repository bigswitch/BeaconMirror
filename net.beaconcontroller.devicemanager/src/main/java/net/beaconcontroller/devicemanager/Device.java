package net.beaconcontroller.devicemanager;

import java.util.Arrays;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    protected Queue<Integer> networkAddresses;
    protected Queue<SwitchPortTuple> swPorts;
    protected Date lastSeen;
    protected Date lastSeenInDb;
    boolean shouldUpdateLastSeenToDb;

    public Device() {
        this.networkAddresses = new ConcurrentLinkedQueue<Integer>();
        this.swPorts = new ConcurrentLinkedQueue<SwitchPortTuple>();
        lastSeen = new Date();
        lastSeenInDb = null;
        shouldUpdateLastSeenToDb = true;
        // TODO - Alex - we need to read this from the db because it might be aged out in Beacon?
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
    public Queue<SwitchPortTuple> getSwPorts() {
        return swPorts;
    }

    /**
     * @param swPort the swPort to set
     */
    public void setSwPorts(Queue<SwitchPortTuple> swPorts) {
        this.swPorts = swPorts;
    }

    /**
     * @return the networkAddresses
     */
    public Queue<Integer> getNetworkAddresses() {
        return networkAddresses;
    }

    /**
     * @param networkAddresses the networkAddresses to set
     */
    public void setNetworkAddresses(Queue<Integer> networkAddresses) {
        this.networkAddresses = networkAddresses;
    }
    
    /**
     * Updates the last seen timestamp for the device
     */
    public void updateLastSeen() {
        shouldUpdateLastSeenToDb = false;
        lastSeen.setTime(System.currentTimeMillis());
        /*
         * We only update the DB if it has not been updated in an hour.
         * This is done in order to prevent too many writes to the DB.
         * TODO - Alex - find a better update strategy. This one will
         * not work well in some edge cases (i.e. a flow starts at
         * minute 59).
         */
        if ((lastSeenInDb == null) || (lastSeen.getTime() - lastSeenInDb.getTime()) > 3600000) {
            if (lastSeenInDb == null) {
                lastSeenInDb = new Date(lastSeen.getTime());
            } else {
                lastSeenInDb.setTime(lastSeen.getTime());
            }
            shouldUpdateLastSeenToDb = true;
        }
    }
    
    /**
     * Checks to see if we should write the last seen timestamp to the storage service.
     * @return True if we should udpate the storage service, false otherwise
     */
    public boolean shouldUpdateLastSeenStorage() {
        return shouldUpdateLastSeenToDb;
    }
    
    /**
     * Gets the UTC timestamp when the device was last seen.
     * @return The UTC timestamp of when the device was last seen.
     */
    public Date getLastSeenDate() {
        return lastSeen;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 2633;
        int result = 1;
        result = prime * result + Arrays.hashCode(dataLayerAddress);
        result = prime
                * result
                + ((networkAddresses == null) ? 0 : networkAddresses.hashCode());
        result = prime * result + ((swPorts == null) ? 0 : swPorts.hashCode());
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
        } else if (!Arrays.equals(networkAddresses.toArray(new Integer[0]),
                other.networkAddresses.toArray(new Integer[0])))
            return false;
        if (swPorts == null) {
            if (other.swPorts != null)
                return false;
        } else if (!Arrays.equals(swPorts.toArray(new SwitchPortTuple[0]),
                other.swPorts.toArray(new SwitchPortTuple[0])))
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
                ", swPorts=" + swPorts + ", networkAddresses="
                + IPv4.fromIPv4AddressCollection(networkAddresses) + "]";
    }
}
