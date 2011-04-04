/**
 *
 */
package net.beaconcontroller.devicemanager.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFSwitchListener;
import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.devicemanager.IDeviceManagerAware;
import net.beaconcontroller.devicemanager.dao.IDeviceManagerDao;
import net.beaconcontroller.packet.ARP;
import net.beaconcontroller.packet.Ethernet;
import net.beaconcontroller.packet.IPv4;
import net.beaconcontroller.topology.ITopology;
import net.beaconcontroller.topology.ITopologyAware;
import net.beaconcontroller.topology.SwitchPortTuple;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPhysicalPort.OFPortConfig;
import org.openflow.protocol.OFPhysicalPort.OFPortState;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFPortStatus.OFPortReason;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DeviceManager creates Devices based upon MAC addresses seen in the network.
 * It tracks any network addresses mapped to the Device, and its location
 * within the network.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class DeviceManagerImpl implements IDeviceManager, IOFMessageListener,
        IOFSwitchListener, ITopologyAware {
    protected static Logger log = LoggerFactory.getLogger(DeviceManagerImpl.class);

    protected IBeaconProvider beaconProvider;
    protected Map<Long, Device> dataLayerAddressDeviceMap;
    protected Set<IDeviceManagerAware> deviceManagerAware;
    protected ReentrantReadWriteLock lock;
    protected volatile boolean shuttingDown = false;
    protected Map<IOFSwitch, Set<Device>> switchDeviceMap;
    protected Map<SwitchPortTuple, Set<Device>> switchPortDeviceMap;
    protected ITopology topology;
    protected BlockingQueue<Update> updates;
    protected Thread updatesThread;
    protected IDeviceManagerDao deviceManagerDao;

    protected enum UpdateType {
        ADDED, REMOVED, MOVED
    }

    /**
     * Used internally to feed the update queue for IDeviceManagerAware listeners
     */
    protected class Update {
        public Device device;
        public IOFSwitch oldSw;
        public Short oldSwPort;
        public IOFSwitch sw;
        public Short swPort;
        public UpdateType updateType;

        public Update(UpdateType type) {
            this.updateType = type;
        }
    }

    /**
     * 
     */
    public DeviceManagerImpl() {
        this.dataLayerAddressDeviceMap = new ConcurrentHashMap<Long, Device>();
        this.lock = new ReentrantReadWriteLock();
        this.switchDeviceMap = new ConcurrentHashMap<IOFSwitch, Set<Device>>();
        this.switchPortDeviceMap = new ConcurrentHashMap<SwitchPortTuple, Set<Device>>();
        this.updates = new LinkedBlockingQueue<Update>();
    }

    public void startUp() {
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
        beaconProvider.addOFMessageListener(OFType.PORT_STATUS, this);
        beaconProvider.addOFSwitchListener(this);

        updatesThread = new Thread(new Runnable () {
            @Override
            public void run() {
                while (true) {
                    try {
                        Update update = updates.take();
                        if (deviceManagerAware != null) {
                            for (IDeviceManagerAware dma : deviceManagerAware) {
                                try {
                                    switch (update.updateType) {
                                        case ADDED:
                                            dma.deviceAdded(update.device);
                                            break;
                                        case REMOVED:
                                            dma.deviceRemoved(update.device);
                                            break;
                                        case MOVED:
                                            dma.deviceMoved(update.device,
                                                    update.oldSw,
                                                    update.oldSwPort,
                                                    update.sw, update.swPort);
                                            break;
                                    }
                                } catch (Exception e) {
                                    log.error("Exception in callback", e);
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        log.warn("DeviceManager Updates thread interupted", e);
                        if (shuttingDown)
                            return;
                    }
                }
            }}, "DeviceManager Updates");
        updatesThread.start();
    }

    public void shutDown() {
        shuttingDown = true;
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
        beaconProvider.removeOFMessageListener(OFType.PORT_STATUS, this);
        beaconProvider.removeOFSwitchListener(this);
        updatesThread.interrupt();
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
            SwitchPortTuple id = new SwitchPortTuple(sw, ps.getDesc().getPortNumber());
            lock.writeLock().lock();
            try {
                if (switchPortDeviceMap.containsKey(id)) {

                    // Remove this switch:port mapping
                    Set<Device> switchPortDevices = switchPortDeviceMap.remove(id);

                    // Remove the individual devices
                    for (Device device : switchPortDevices) {
                        // Remove the device from the switch->device mapping
                        switchDeviceMap.get(id.getSw()).remove(device);
                        deviceManagerDao.updateDevice(device);
                    }
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
        dataLayerAddressDeviceMap.remove(Ethernet.toLong(device.getDataLayerAddress()));
        deviceManagerDao.removeDevice(device);
        updateStatus(device, false);
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

        Long dlAddr = Ethernet.toLong(match.getDataLayerSource());

        Integer nwSrc = 0;
        Ethernet eth = new Ethernet();
        eth.deserialize(pi.getPacketData(), 0, pi.getPacketData().length);
        if (eth.getPayload() instanceof ARP) {
            ARP arp = (ARP) eth.getPayload();
            if ((arp.getProtocolType() == ARP.PROTO_TYPE_IP)
                    && (Ethernet.toLong(arp.getSenderHardwareAddress()) == dlAddr)) {
                nwSrc = IPv4.toIPv4Address(arp.getSenderProtocolAddress());
            }
        }

        Device device = null;
        lock.readLock().lock();
        try {
            device = dataLayerAddressDeviceMap.get(dlAddr);
        } finally {
            lock.readLock().unlock();
        }
        SwitchPortTuple ipt = new SwitchPortTuple(sw, pi.getInPort());
        if (!topology.isInternal(ipt)) {
            if (device != null) {
                // Write lock is expensive, check if we have an update first
                boolean movedLocation = true;
                boolean addedNW = true;

                for (SwitchPortTuple currSwPort : device.getSwPorts()) {
                    if (currSwPort.equals(ipt)) {
                        movedLocation = false;
                        break;
                    }
                }
                for (Integer currAddr : device.getNetworkAddresses()) {
                    if ((nwSrc == 0) || (currAddr.intValue() == nwSrc)) {
                        addedNW = false;
                        break;
                    }
                }
            
                if (movedLocation || addedNW) {
                    // Update everything needed during one write lock
                    lock.writeLock().lock();
                    try {
                        // Update both mappings once so no duplicated work later
                        if (movedLocation) {
// FIXME: age out old device - swPort mappings
//                            IOFSwitch oldSw = device.getSw();
//                            Short oldPort = device.getSwPort();
//                            delSwitchDeviceMapping(device.getSw(), device);
//                            delSwitchPortDeviceMapping(
//                                    new SwitchPortTuple(device.getSw(),
//                                            device.getSwPort()), device);
                            device.getSwPorts().add(ipt);
                            addSwitchDeviceMapping(ipt.getSw(), device);
                            addSwitchPortDeviceMapping(ipt, device);
                            updateMoved(device, ipt, ipt);
                            log.info("Device {} added {}", device, ipt);
                        }
                        if (addedNW) {
                            // add the address
                            device.getNetworkAddresses().add(nwSrc);
                            log.info("Device {} added IP {}", device,
                                    IPv4.fromIPv4Address(nwSrc));
                        }
                        // FIXME: Probably shouldn't do storage operation while
                        // holding lock.
                        deviceManagerDao.updateDevice(device);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            } else {
                device = new Device();
                device.setDataLayerAddress(match.getDataLayerSource());
                device.getSwPorts().add(ipt);
                lock.writeLock().lock();
                try {
                    if (nwSrc != 0) {
                        device.getNetworkAddresses().add(nwSrc);
                    }
                    this.dataLayerAddressDeviceMap.put(dlAddr, device);
                    deviceManagerDao.addDevice(device);
                    addSwitchDeviceMapping(ipt.getSw(), device);
                    addSwitchPortDeviceMapping(ipt, device);
                    updateStatus(device, true);
                    log.info("New device {}", device);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        return Command.CONTINUE;
    }

    protected void addSwitchDeviceMapping(IOFSwitch sw, Device device) {
        if (switchDeviceMap.get(sw) == null) {
            switchDeviceMap.put(sw, new HashSet<Device>());
        }
        switchDeviceMap.get(sw).add(device);
    }

    protected void delSwitchDeviceMapping(IOFSwitch sw, Device device) {
        if (switchDeviceMap.get(sw) == null) {
            return;
        }
        switchDeviceMap.get(sw).remove(device);
        if (switchDeviceMap.get(sw).isEmpty()) {
            switchDeviceMap.remove(sw);
        }
    }

    protected void addSwitchPortDeviceMapping(SwitchPortTuple id, Device device) {
        if (switchPortDeviceMap.get(id) == null) {
            switchPortDeviceMap.put(id, new HashSet<Device>());
        }
        switchPortDeviceMap.get(id).add(device);
    }

    protected void delSwitchPortDeviceMapping(SwitchPortTuple id, Device device) {
        if (switchPortDeviceMap.get(id) == null) {
            return;
        }
        switchPortDeviceMap.get(id).remove(device);
        if (switchPortDeviceMap.get(id).isEmpty()) {
            switchPortDeviceMap.remove(id);
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
    public Device getDeviceByDataLayerAddress(byte[] address) {
        lock.readLock().lock();
        try {
            return this.dataLayerAddressDeviceMap.get(Ethernet.toLong(address));
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
            if (switchDeviceMap.get(sw) != null) {
                // Remove all switch:port mappings where the switch is sw
                for (Iterator<Map.Entry<SwitchPortTuple, Set<Device>>> it = switchPortDeviceMap
                        .entrySet().iterator(); it.hasNext();) {
                    Map.Entry<SwitchPortTuple, Set<Device>> entry = it.next();
                    if (entry.getKey().getSw().equals(sw)) {
                        it.remove();
                    }
                }

                // Remove all devices on this switch
                Set<Device> devices = switchDeviceMap.remove(sw);
                for (Device device : devices) {
                    delDevice(device);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    
    @Override
    public void addedLink(IOFSwitch srcSw, short srcPort, int srcPortState,
            IOFSwitch dstSw, short dstPort, int dstPortState)
    {
        updatedLink(srcSw, srcPort, srcPortState, dstSw, dstPort, dstPortState);
    }
    
    @Override
    public void updatedLink(IOFSwitch src, short srcPort, int srcPortState,
            IOFSwitch dst, short dstPort, int dstPortState)
    {
        if (((srcPortState & OFPortState.OFPPS_STP_MASK.getValue()) != OFPortState.OFPPS_STP_BLOCK.getValue()) &&
            ((dstPortState & OFPortState.OFPPS_STP_MASK.getValue()) != OFPortState.OFPPS_STP_BLOCK.getValue())) {
            // Remove all devices living on this switch:port now that it is internal
            SwitchPortTuple id = new SwitchPortTuple(dst, dstPort);
            lock.writeLock().lock();
            try {
                if (switchPortDeviceMap.containsKey(id)) {
                    // Remove this switch:port mapping
                    Set<Device> devices = switchPortDeviceMap.remove(id);
                    // Remove the devices
                    for (Device device : devices) {
                        // Remove the device from the switch->device mapping
                        delSwitchDeviceMapping(id.getSw(), device);
                        deviceManagerDao.updateDevice(device);
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    @Override
    public void removedLink(IOFSwitch src, short srcPort, IOFSwitch dst, short dstPort)
    {
    }

    /**
     * @param deviceManagerAware the deviceManagerAware to set
     */
    public void setDeviceManagerAware(Set<IDeviceManagerAware> deviceManagerAware) {
        this.deviceManagerAware = deviceManagerAware;
    }

    /**
     * @param deviceManagerDao the deviceManagerDao to set
     */
    public void setDeviceManagerDao(IDeviceManagerDao deviceManagerDao) {
        this.deviceManagerDao = deviceManagerDao;
    }

    /**
     * Puts an update in queue for the Device.  Must be called from within the
     * write lock.
     * @param device
     * @param added
     */
    protected void updateStatus(Device device, boolean added) {
        Update update;
        if (added) {
            update = new Update(UpdateType.ADDED);
        } else {
            update = new Update(UpdateType.REMOVED);
        }
        update.device = device;
        this.updates.add(update);
    }

    /**
     * Puts an update in queue to indicate the Device moved.  Must be called
     * from within the write lock.
     * @param device
     * @param oldSw
     * @param oldPort
     * @param sw
     * @param port
     */
    protected void updateMoved(Device device, SwitchPortTuple oldSwPort, SwitchPortTuple swPort) {
        Update update = new Update(UpdateType.MOVED);
        update.device = device;
        update.oldSw = oldSwPort.getSw();
        update.oldSwPort = oldSwPort.getPort();
        update.sw = swPort.getSw();
        update.swPort = swPort.getPort();
        this.updates.add(update);
    }
}
