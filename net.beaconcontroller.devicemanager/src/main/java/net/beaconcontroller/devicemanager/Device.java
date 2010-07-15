package net.beaconcontroller.devicemanager;

import java.util.Arrays;

import net.beaconcontroller.core.IOFSwitch;

public class Device {
    protected byte[] dataLayerAddress;
    protected Integer networkAddress;
    protected IOFSwitch sw;
    protected Short swPort;

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
     * @return the networkAddress
     */
    public Integer getNetworkAddress() {
        return networkAddress;
    }

    /**
     * @param networkAddress the networkAddress to set
     */
    public void setNetworkAddress(Integer networkAddress) {
        this.networkAddress = networkAddress;
    }

    /**
     * @return the sw
     */
    public IOFSwitch getSw() {
        return sw;
    }

    /**
     * @param sw the sw to set
     */
    public void setSw(IOFSwitch sw) {
        this.sw = sw;
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

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 2633;
        int result = 1;
        result = prime * result + Arrays.hashCode(dataLayerAddress);
        result = prime * result
                + ((networkAddress == null) ? 0 : networkAddress.hashCode());
        result = prime * result + ((sw == null) ? 0 : sw.hashCode());
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
        if (networkAddress == null) {
            if (other.networkAddress != null)
                return false;
        } else if (!networkAddress.equals(other.networkAddress))
            return false;
        if (sw == null) {
            if (other.sw != null)
                return false;
        } else if (!sw.equals(other.sw))
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
        return "Device [dataLayerAddress=" + Arrays.toString(dataLayerAddress)
                + ", networkAddress=" + networkAddress + ", sw=" + sw
                + ", swPort=" + swPort + "]";
    }
}
