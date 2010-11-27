package net.beaconcontroller.devicemanager;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import net.beaconcontroller.packet.IPv4;

import org.openflow.util.HexString;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class Device {
    protected byte[] dataLayerAddress;
    protected Set<Integer> networkAddresses;
    protected Long swId;
    protected Short swPort;

    public Device() {
        this.networkAddresses = new ConcurrentSkipListSet<Integer>();
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
     * @return the swPort
     */
    public Short getSwPort() {
        return swPort;
    }

    /**
     * @param swPort the swPort to set
     */
    public void setSwPort(Short swPort) {
        this.swPort = swPort;
    }

    /**
     * @return the swId
     */
    public Long getSwId() {
        return swId;
    }

    /**
     * @param swId the swId to set
     */
    public void setSwId(Long swId) {
        this.swId = swId;
    }

    /**
     * @return the networkAddresses
     */
    public Set<Integer> getNetworkAddresses() {
        return networkAddresses;
    }

    /**
     * @param networkAddresses the networkAddresses to set
     */
    public void setNetworkAddresses(Set<Integer> networkAddresses) {
        this.networkAddresses = networkAddresses;
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
        result = prime * result + ((swId == null) ? 0 : swId.hashCode());
        result = prime * result + ((swPort == null) ? 0 : swPort.hashCode());
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
        if (swId == null) {
            if (other.swId != null)
                return false;
        } else if (!swId.equals(other.swId))
            return false;
        if (swPort == null) {
            if (other.swPort != null)
                return false;
        } else if (!swPort.equals(other.swPort))
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
                ", swId=" + ((swId == null) ? "null" : HexString.toHexString(swId)) +
                ", swPort=" + ((swPort == null) ? "null" : (0xffff & swPort)) +
                ", networkAddresses="
                + IPv4.fromIPv4AddressCollection(networkAddresses) + "]";
    }
}
