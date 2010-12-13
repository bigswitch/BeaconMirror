/**
 *
 */
package net.beaconcontroller.devicemanager.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFSwitchListener;
import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.packet.IPv4;
import net.beaconcontroller.topology.ITopology;
import net.beaconcontroller.topology.IdPortTuple;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPhysicalPort.OFPortConfig;
import org.openflow.protocol.OFPhysicalPort.OFPortState;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFPortStatus.OFPortReason;
import org.openflow.protocol.OFType;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class DeviceManagerImpl implements IDeviceManager, IOFMessageListener, IOFSwitchListener {
    protected static Logger log = LoggerFactory.getLogger(DeviceManagerImpl.class);

    protected IBeaconProvider beaconProvider;
    protected Map<Integer, Device> dataLayerAddressDeviceMap;
    protected ReentrantReadWriteLock lock;
    protected Map<Integer, Device> networkLayerAddressDeviceMap;
    protected Map<Long, Set<Device>> switchDeviceMap;
    protected Map<IdPortTuple, Set<Device>> switchPortDeviceMap;
    protected ITopology topology;

    /**
     * 
     */
    public DeviceManagerImpl() {
        this.dataLayerAddressDeviceMap = new ConcurrentHashMap<Integer, Device>();
        this.networkLayerAddressDeviceMap = new ConcurrentHashMap<Integer, Device>();
        this.switchDeviceMap = new ConcurrentHashMap<Long, Set<Device>>();
        this.switchPortDeviceMap = new ConcurrentHashMap<IdPortTuple, Set<Device>>();
        this.lock = new ReentrantReadWriteLock();
    }

    public void startUp() {
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
        beaconProvider.addOFMessageListener(OFType.PORT_STATUS, this);
    }

    public void shutDown() {
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
        beaconProvider.removeOFMessageListener(OFType.PORT_STATUS, this);
    }

    @Override
    public String getName() {
        return "devicemanager";
    }

    public Command handlePortStatus(IOFSwitch sw, OFPortStatus ps) {
        // if ps is a delete, or a modify where the port is down or configured down
        if ((byte)OFPortReason.OFPPR_DELETE.ordinal() == ps.getReason() ||
            ((byte)OFPortReason.OFPPR_MODIFY.ordinal() == ps.getReason() &&
                        (((OFPortConfig.OFPPC_PORT_DOWN.getValue() & ps.getDesc().getConfig()) > 0) ||
                                ((OFPortState.OFPPS_LINK_DOWN.getValue() & ps.getDesc().getState()) > 0)))) {
            IdPortTuple id = new IdPortTuple(sw.getId(), ps.getDesc().getPortNumber());
            lock.writeLock().lock();
            try {
                if (switchPortDeviceMap.containsKey(id)) {
                    // Remove the devices
                    for (Device device : switchPortDeviceMap.get(id)) {
                        delDevice(device);
                        // Remove the device from the switch->device mapping
                        switchDeviceMap.get(id.getId()).remove(device);
                    }
                    // Remove this switch:port mapping
                    switchPortDeviceMap.remove(id);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return Command.CONTINUE;
    }

    /**
     * Removes the specified device from data layer and network layer maps.
     * Does NOT remove the device from switch and switch:port level maps.
     * Must be called from within a write lock.
     * @param device
     */
    protected void delDevice(Device device) {
        dataLayerAddressDeviceMap.remove(Arrays.hashCode(device.getDataLayerAddress()));
        if (!device.getNetworkAddresses().isEmpty()) {
            for (Integer nwAddress : device.getNetworkAddresses()) {
                networkLayerAddressDeviceMap.remove(nwAddress);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Removed device {}", device);
        }
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg) {
        if (msg instanceof OFPortStatus) {
            return handlePortStatus(sw, (OFPortStatus) msg);
        }
        OFPacketIn pi = (OFPacketIn) msg;
        OFMatch match = new OFMatch();
        match.loadFromPacket(pi.getPacketData(), pi.getInPort());

        // if the source is multicast/broadcast ignore it
        if ((match.getDataLayerSource()[0] & 0x1) != 0)
            return Command.CONTINUE;

        Integer dlAddrHash = Arrays.hashCode(match.getDataLayerSource());
        Integer nwSrc = match.getNetworkSource();
        Device device = null;
        Device nwDevice = null;
        lock.readLock().lock();
        try {
            device = dataLayerAddressDeviceMap.get(dlAddrHash);
            nwDevice = networkLayerAddressDeviceMap.get(nwSrc);
        } finally {
            lock.readLock().unlock();
        }
        IdPortTuple ipt = new IdPortTuple(sw.getId(), pi.getInPort());
        if (!topology.isInternal(ipt)) {
            if (device != null) {
                // Write lock is expensive, check if we have an update first
                boolean updateNeeded = false;
                boolean movedLocation = false;
                boolean addedNW = false;
                boolean nwChanged = false;

                if ((sw.getId() != device.getSwId().longValue())
                        || (pi.getInPort() != device.getSwPort().shortValue())) {
                    movedLocation = true;
                }
                if (nwDevice == null && nwSrc != 0) {
                    addedNW = true;
                } else if (nwDevice != null && !device.equals(nwDevice)) {
                    nwChanged = true;
                }

                if (movedLocation || addedNW || nwChanged) {
                    updateNeeded = true;
                }

                if (updateNeeded) {
                    // Update everything needed during one write lock
                    lock.writeLock().lock();
                    try {
                        // Update both mappings once so no duplicated work later
                        if (movedLocation) {
                            delSwitchDeviceMapping(device.getSwId(), device);
                            delSwitchPortDeviceMapping(
                                    new IdPortTuple(device.getSwId(),
                                            device.getSwPort()), device);
                            if (log.isDebugEnabled()) {
                                log.debug(
                                        "Device {} moved to switch: {} port: {}",
                                        new Object[] {
                                                device,
                                                HexString.toHexString(sw
                                                        .getId()),
                                                0xffff & pi.getInPort() });
                            }
                            device.setSwId(sw.getId());
                            device.setSwPort(pi.getInPort());
                            addSwitchDeviceMapping(sw.getId(), device);
                            addSwitchPortDeviceMapping(
                                    new IdPortTuple(device.getSwId(),
                                            device.getSwPort()), device);
                        }
                        if (addedNW) {
                            // add the address
                            device.getNetworkAddresses().add(nwSrc);
                            this.networkLayerAddressDeviceMap.put(nwSrc, device);
                            if (log.isDebugEnabled()) {
                                log.debug("Added IP {} to MAC {}",
                                        IPv4.fromIPv4Address(nwSrc),
                                        HexString.toHexString(device.getDataLayerAddress()));
                            }
                        } else if (nwChanged) {
                            // IP changed MACs.. really rare, potentially an error
                            nwDevice.getNetworkAddresses().remove(nwSrc);
                            device.getNetworkAddresses().add(nwSrc);
                            this.networkLayerAddressDeviceMap.put(nwSrc, device);
                            if (log.isWarnEnabled()) {
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
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            } else {
                device = new Device();
                device.setDataLayerAddress(match.getDataLayerSource());
                device.setSwId(sw.getId());
                device.setSwPort(pi.getInPort());
                lock.writeLock().lock();
                try {
                    this.dataLayerAddressDeviceMap.put(dlAddrHash, device);
                    if (nwSrc != 0) {
                        device.getNetworkAddresses().add(nwSrc);
                        this.networkLayerAddressDeviceMap.put(nwSrc, device);
                    }
                    addSwitchDeviceMapping(device.getSwId(), device);
                    addSwitchPortDeviceMapping(new IdPortTuple(
                            device.getSwId(), device.getSwPort()), device);
                    if (nwDevice != null) {
                        nwDevice.getNetworkAddresses().remove(nwSrc);
                        if (log.isWarnEnabled()) {
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
                } finally {
                    lock.writeLock().unlock();
                }
                log.debug("New Device: {}", device);
            }
        }

        return Command.CONTINUE;
    }

    protected void addSwitchDeviceMapping(Long id, Device device) {
        if (switchDeviceMap.get(id) == null) {
            switchDeviceMap.put(id, new HashSet<Device>());
        }
        switchDeviceMap.get(id).add(device);
    }

    protected void delSwitchDeviceMapping(Long id, Device device) {
        switchDeviceMap.get(id).remove(device);
        if (switchDeviceMap.get(id).isEmpty()) {
            switchDeviceMap.remove(id);
        }
    }

    protected void addSwitchPortDeviceMapping(IdPortTuple id, Device device) {
        if (switchPortDeviceMap.get(id) == null) {
            switchPortDeviceMap.put(id, new HashSet<Device>());
        }
        switchPortDeviceMap.get(id).add(device);
    }

    protected void delSwitchPortDeviceMapping(IdPortTuple id, Device device) {
        switchPortDeviceMap.get(id).remove(device);
        if (switchPortDeviceMap.get(id).isEmpty()) {
            switchPortDeviceMap.remove(id);
        }
    }

    @Override
    public Device getDeviceByNetworkLayerAddress(Integer address) {
        lock.readLock().lock();
        try {
            return this.networkLayerAddressDeviceMap.get(address);
        } finally {
            lock.readLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            return this.dataLayerAddressDeviceMap.get(hashCode);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Device getDeviceByDataLayerAddress(byte[] address) {
        lock.readLock().lock();
        try {
            return this.getDeviceByDataLayerAddress(Arrays.hashCode(address));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Device> getDevices() {
        lock.readLock().lock();
        try {
            return new ArrayList<Device>(this.dataLayerAddressDeviceMap.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addedSwitch(IOFSwitch sw) {
    }

    @Override
    public void removedSwitch(IOFSwitch sw) {
        // remove all devices attached to this switch
        lock.writeLock().lock();
        try {
            Long id = sw.getId();
            if (switchDeviceMap.get(id) != null) {
                // Remove all devices on this switch
                for (Device device : switchDeviceMap.get(id)) {
                    delDevice(device);
                }
                switchDeviceMap.remove(id);
                // Remove all switch:port mappings where the switch is sw
                for (Iterator<Map.Entry<IdPortTuple, Set<Device>>> it = switchPortDeviceMap
                        .entrySet().iterator(); it.hasNext();) {
                    Map.Entry<IdPortTuple, Set<Device>> entry = it.next();
                    if (entry.getKey().getId().equals(id)) {
                        it.remove();
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
