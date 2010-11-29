/**
 *
 */
package net.beaconcontroller.devicemanager.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.packet.IPv4;
import net.beaconcontroller.topology.ITopology;
import net.beaconcontroller.topology.IdPortTuple;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class DeviceManagerImpl implements IDeviceManager, IOFMessageListener {
    protected static Logger log = LoggerFactory.getLogger(DeviceManagerImpl.class);

    protected IBeaconProvider beaconProvider;
    protected Map<Integer, Device> dataLayerAddressDeviceMap;
    protected Map<Integer, Device> networkLayerAddressDeviceMap;
    protected ITopology topology;

    /**
     * 
     */
    public DeviceManagerImpl() {
        this.dataLayerAddressDeviceMap = new ConcurrentHashMap<Integer, Device>();
        this.networkLayerAddressDeviceMap = new ConcurrentHashMap<Integer, Device>();
    }

    public void startUp() {
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }

    public void shutDown() {
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
    }

    @Override
    public String getName() {
        return "devicemanager";
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg) {
        OFPacketIn pi = (OFPacketIn) msg;
        OFMatch match = new OFMatch();
        match.loadFromPacket(pi.getPacketData(), pi.getInPort());

        // if the source is multicast/broadcast ignore it
        if ((match.getDataLayerSource()[0] & 0x1) != 0)
            return Command.CONTINUE;

        Integer dlAddrHash = Arrays.hashCode(match.getDataLayerSource());
        Device device = dataLayerAddressDeviceMap.get(dlAddrHash);
        Integer nwSrc = match.getNetworkSource();
        Device nwDevice = networkLayerAddressDeviceMap.get(nwSrc);
        IdPortTuple ipt = new IdPortTuple(sw.getId(), pi.getInPort());
        if (!topology.isInternal(ipt)) {
            if (device != null) {
                if (sw.getId() != device.getSwId().longValue()) {
                    log.debug("Device {} moved to switch id {}", device, sw.getId());
                    device.setSwId(sw.getId());
                }
                if (pi.getInPort() != device.getSwPort().shortValue()) {
                    log.debug("Device {} moved to port {}", device, pi.getInPort());
                    device.setSwPort(pi.getInPort());
                }
                if (nwDevice == null && nwSrc != 0) {
                    // add the address
                    device.getNetworkAddresses().add(nwSrc);
                    this.networkLayerAddressDeviceMap.put(nwSrc, device);
                    log.debug("Added IP {} to MAC {}",
                            IPv4.fromIPv4Address(nwSrc),
                            HexString.toHexString(device.getDataLayerAddress()));
                } else if (nwDevice != null && !device.equals(nwDevice)) {
                    // IP changed MACs.. really rare, potentially an error
                    nwDevice.getNetworkAddresses().remove(nwSrc);
                    device.getNetworkAddresses().add(nwSrc);
                    this.networkLayerAddressDeviceMap.put(nwSrc, device);
                    log.warn(
                            "IP Address {} changed from MAC {} to {}",
                            new Object[] {
                                    IPv4.fromIPv4Address(nwSrc),
                                    HexString.toHexString(nwDevice
                                            .getDataLayerAddress()),
                                    HexString.toHexString(device
                                            .getDataLayerAddress()) });
                }
            } else {
                device = new Device();
                device.setDataLayerAddress(match.getDataLayerSource());
                device.setSwId(sw.getId());
                device.setSwPort(pi.getInPort());
                this.dataLayerAddressDeviceMap.put(dlAddrHash, device);
                if (nwSrc != 0) {
                    device.getNetworkAddresses().add(nwSrc);
                    this.networkLayerAddressDeviceMap.put(nwSrc, device);
                }
                log.debug("New Device: {}", device);
                if (nwDevice != null) {
                    nwDevice.getNetworkAddresses().remove(nwSrc);
                    log.warn(
                            "IP Address {} changed from MAC {} to {}",
                            new Object[] {
                                    IPv4.fromIPv4Address(nwSrc),
                                    HexString.toHexString(nwDevice
                                            .getDataLayerAddress()),
                                    HexString.toHexString(device
                                            .getDataLayerAddress()) });
                }
            }
        }

        return Command.CONTINUE;
    }

    @Override
    public Device getDeviceByNetworkLayerAddress(Integer address) {
        return this.networkLayerAddressDeviceMap.get(address);
    }

    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

    /**
     * @param topology the topology to set
     */
    public void setTopology(ITopology topology) {
        this.topology = topology;
    }

    @Override
    public Device getDeviceByDataLayerAddress(Integer hashCode) {
        return this.dataLayerAddressDeviceMap.get(hashCode);
    }

    @Override
    public Device getDeviceByDataLayerAddress(byte[] address) {
        return this.getDeviceByDataLayerAddress(Arrays.hashCode(address));
    }

    @Override
    public List<Device> getDevices() {
        return new ArrayList<Device>(this.dataLayerAddressDeviceMap.values());
    }
}
