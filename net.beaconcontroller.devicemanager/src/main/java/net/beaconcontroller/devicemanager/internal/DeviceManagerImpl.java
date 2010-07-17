/**
 *
 */
package net.beaconcontroller.devicemanager.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.topology.ITopology;
import net.beaconcontroller.topology.IdPortTuple;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author derickso
 *
 */
public class DeviceManagerImpl implements IDeviceManager, IOFMessageListener {
    protected static Logger logger = LoggerFactory.getLogger(DeviceManagerImpl.class);

    protected IBeaconProvider beaconProvider;
    protected Map<Integer, Device> dataLayerAddressDeviceMap;
    protected ITopology topology;

    /**
     * 
     */
    public DeviceManagerImpl() {
        this.dataLayerAddressDeviceMap = new HashMap<Integer, Device>();
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
        Integer dlAddrHash = Arrays.hashCode(match.getDataLayerSource());
        Device device = dataLayerAddressDeviceMap.get(dlAddrHash);
        IdPortTuple ipt = new IdPortTuple(sw.getId(), pi.getInPort());
        if (!topology.isInternal(ipt)) {
            if (device != null) {
                if (sw.getId() != device.getSwId().longValue()) {
                    logger.debug("Device {} moved to switch id {}", device, sw.getId());
                    device.setSwId(sw.getId());
                }
                if (pi.getInPort() != device.getSwPort().shortValue()) {
                    logger.debug("Device {} moved to port {}", device, pi.getInPort());
                    device.setSwPort(pi.getInPort());
                }
            } else {
                device = new Device();
                device.setDataLayerAddress(match.getDataLayerSource());
                device.setSwId(sw.getId());
                device.setSwPort(pi.getInPort());
                // TODO locking
                this.dataLayerAddressDeviceMap.put(dlAddrHash, device);
                logger.debug("New Device: {}", device);
            }
        }

        return Command.CONTINUE;
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
}
