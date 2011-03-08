package net.beaconcontroller.core.types;

public class MacVlanPair {
    public Long mac;
    public Short vlan;
    public MacVlanPair(Long mac, Short vlan) {
        this.mac = mac;
        this.vlan = vlan;
    }
    
    public long getMac() {
        return mac.longValue();
    }
    
    public short getVlan() {
        return vlan.shortValue();
    }
    
    public boolean equals(Object o) {
        return (o instanceof MacVlanPair) && (mac.equals(((MacVlanPair) o).mac))
            && (vlan.equals(((MacVlanPair) o).vlan));
    }
    
    public int hashCode() {
        return mac.hashCode() ^ vlan.hashCode();
    }
}