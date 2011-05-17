/**
 *
 */
package net.beaconcontroller.devicemanager.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import net.beaconcontroller.devicemanager.DeviceAttachmentPoint;
import net.beaconcontroller.devicemanager.DeviceNetworkAddress;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.devicemanager.IDeviceManagerAware;
import net.beaconcontroller.packet.ARP;
import net.beaconcontroller.packet.Ethernet;
import net.beaconcontroller.packet.IPv4;
import net.beaconcontroller.storage.IResultSet;
import net.beaconcontroller.storage.IStorageSource;
import net.beaconcontroller.storage.OperatorPredicate;
import net.beaconcontroller.storage.StorageException;
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
import org.openflow.util.HexString;
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
    //protected IDeviceManagerDao deviceManagerDao;
    protected IStorageSource storageSource;

    // Constants for accessing storage
    // Table names
    private static final String DEVICE_TABLE_NAME = "controller_host";
    private static final String DEVICE_ATTACHMENT_POINT_TABLE_NAME = "controller_hostattachmentpoint";
    private static final String DEVICE_NETWORK_ADDRESS_TABLE_NAME = "controller_hostnetworkaddress";
    // Column names for the host table
    private static final String MAC_COLUMN_NAME = "mac"; 
    private static final String NAME_COLUMN_NAME = "name"; 
    // Column names for both the attachment point and network address tables
    private static final String ID_COLUMN_NAME = "id";
    private static final String DEVICE_COLUMN_NAME = "host_id";
    private static final String LAST_SEEN_COLUMN_NAME = "last_seen";
    // Column names for the attachment point table
    private static final String SWITCH_COLUMN_NAME = "switch_id"; 
    private static final String PORT_COLUMN_NAME = "inport";
    // Column names for the network address table
    private static final String NETWORK_ADDRESS_COLUMN_NAME = "ip";

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
                        log.warn("DeviceManager Updates thread interrupted", e);
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
                        
                        // Remove the attachment point from the device
                        DeviceAttachmentPoint attachmentPoint = device.removeAttachmentPoint(id);

                        // Update the device in storage
                        // FIXME: Making the storage calls could be a relatively time-consuming
                        // operation, so it's bad that we do it here while we hold the lock for
                        // the entire device manager. Could severely increase lock contention.
                        //
                        // There are a number of different ways to address this problem:
                        //
                        // 1) Make devices immutable. Any update to a logical device would
                        // require creating a new physical Device objecy reflecting the change.
                        // That way clients of the device manager (and the internal storage code
                        // could reference the device without needing to hold a lock. This wouldn't
                        // work well if a device is modified a lot, but in most cases I don't think
                        // devices will be changing too frequently. Also, not sure how this would
                        // work with the lastSeen attributes of the device attachment points and
                        // network addresses. Probably would need to refactor that somehow.
                        //
                        // 2) Save off the modified device state into a separate unshared data
                        // structure while holding the lock and then pass that data structure to
                        // the storage code after releasing the lock. This could still have the
                        // problem that storage updates for a single device could happen
                        // concurrently on separate threads and I could imagine the stored
                        // device getting into an inconsistent state.
                        //
                        // 3) Could do something like queuing up storage updates (captured in
                        // separate unshared data structures ala #2 above) and then servicing the
                        // updates from a separate thread in charge of accessing storage. This
                        // way updates to storage would be serialized and thus would address the
                        // concurrency issue raised in #2. This could potentially be merged with
                        // the existing update thread.
                        
                        removeAttachmentPointFromStorage(device, attachmentPoint);
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
        removeDeviceFromStorage(device);
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
        SwitchPortTuple switchPort = new SwitchPortTuple(sw, pi.getInPort());
        if (!topology.isInternal(switchPort)) {
            
            Date currentDate = new Date();
            
            if (device != null) {
                
                // Write lock is expensive, check if we have an update first
                boolean newAttachmentPoint = false;
                boolean newNetworkAddress = false;
                boolean updateAttachmentPointLastSeen = false;
                boolean updateNetworkAddressLastSeen = false;
                
                DeviceAttachmentPoint attachmentPoint = null;
                DeviceAttachmentPoint oldAttachmentPoint = null;
                DeviceNetworkAddress networkAddress = null;

                // FIXME: Should refactor code to not acquire read lock multiple times
                // (i.e. this one and the one above where we look up the device).
                // Also, are we convinced that it's thread-safe to have a reference to
                // a device and access fields without holding the read or write lock?
                
                lock.readLock().lock();
                
                try {
                    attachmentPoint = device.getAttachmentPoint(switchPort);
                    if (attachmentPoint != null) {
                        // FIXME: Problem updating last seen while only holding read lock, not write lock
                        attachmentPoint.setLastSeen(currentDate);
                        updateAttachmentPointLastSeen = attachmentPoint.shouldWriteLastSeenToStorage(currentDate);
                    } else {
                        attachmentPoint = new DeviceAttachmentPoint(switchPort, currentDate);
                        DeviceAttachmentPoint sameClusterAttachmentPoint = null;
                        for (DeviceAttachmentPoint existingAttachmentPoint: device.getAttachmentPoints()) {
                            IOFSwitch existingSwitch = existingAttachmentPoint.getSwitchPort().getSw();
                            IOFSwitch newSwitch = switchPort.getSw();
                            if ((newSwitch == existingSwitch) || ((topology != null) && topology.inSameCluster(newSwitch, existingSwitch))) {
                                oldAttachmentPoint = existingAttachmentPoint;
                            }
                        }
                        newAttachmentPoint = true;
                    }
    
                    if (nwSrc != 0) {
                        networkAddress = device.getNetworkAddress(nwSrc);
                        if (networkAddress != null) {
                            // FIXME: Problem updating last seen while only holding read lock, not write lock
                            updateNetworkAddressLastSeen = networkAddress.shouldWriteLastSeenToStorage(currentDate);
                        } else {
                            networkAddress = new DeviceNetworkAddress(nwSrc, currentDate);
                            newNetworkAddress = true;
                        }
                    }
                }
                finally {
                    lock.readLock().unlock();
                }
                
                if (newAttachmentPoint || newNetworkAddress || updateAttachmentPointLastSeen || updateNetworkAddressLastSeen) {
                    // Update everything needed during one write lock
                    lock.writeLock().lock();
                    try {
                        // Update both mappings once so no duplicated work later
                        if (newAttachmentPoint) {
// FIXME: age out old device - swPort mappings
//                            IOFSwitch oldSw = device.getSw();
//                            Short oldPort = device.getSwPort();
//                            delSwitchDeviceMapping(device.getSw(), device);
//                            delSwitchPortDeviceMapping(
//                                    new SwitchPortTuple(device.getSw(),
//                                            device.getSwPort()), device);
                            if (oldAttachmentPoint != null) {
                                removeAttachmentPointFromStorage(device, oldAttachmentPoint);
                            }
                            device.addAttachmentPoint(attachmentPoint);
                            addSwitchDeviceMapping(switchPort.getSw(), device);
                            addSwitchPortDeviceMapping(switchPort, device);
                            writeAttachmentPointToStorage(device, attachmentPoint, currentDate);
                            updateMoved(device, switchPort, switchPort);
                            log.debug("Device {} added {}", device, switchPort);
                        }
                        if (updateAttachmentPointLastSeen) {
                            writeAttachmentPointLastSeenToStorage(device, attachmentPoint, currentDate);
                        }
                        if (newNetworkAddress) {
                            // add the address
                            device.addNetworkAddress(networkAddress);
                            writeNetworkAddressToStorage(device, networkAddress, currentDate);
                            log.debug("Device {} added IP {}", device, IPv4.fromIPv4Address(nwSrc));
                        }
                        if (updateNetworkAddressLastSeen) {
                            writeNetworkAddressLastSeenToStorage(device, networkAddress, currentDate);
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            } else {
                // Create the new device with attachment point and network address
                device = new Device();
                device.setDataLayerAddress(match.getDataLayerSource());
                device.addAttachmentPoint(switchPort, currentDate);
                if (nwSrc != 0) {
                    device.addNetworkAddress(nwSrc, currentDate);
                }

                // Update data structures and write to storage while holding write lock.
                // FIXME: See comment above about concerns with writing to storage while
                // holding the lock.
                lock.writeLock().lock();
                try {
                    this.dataLayerAddressDeviceMap.put(dlAddr, device);
                    addSwitchDeviceMapping(switchPort.getSw(), device);
                    addSwitchPortDeviceMapping(switchPort, device);
                    writeDeviceToStorage(device, currentDate);
                    updateStatus(device, true);
                    log.debug("New device {}", device);
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
            SwitchPortTuple switchPort = new SwitchPortTuple(dst, dstPort);
            lock.writeLock().lock();
            try {
                if (switchPortDeviceMap.containsKey(switchPort)) {
                    // Remove this switch:port mapping
                    Set<Device> devices = switchPortDeviceMap.remove(switchPort);
                    // Remove the devices
                    for (Device device : devices) {
                        // Remove the device from the switch->device mapping
                        delSwitchDeviceMapping(switchPort.getSw(), device);
                        DeviceAttachmentPoint attachmentPoint = device.removeAttachmentPoint(switchPort);
                        if (attachmentPoint != null)
                            removeAttachmentPointFromStorage(device, attachmentPoint);
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

//    /**
//     * @param deviceManagerDao the deviceManagerDao to set
//     */
//    public void setDeviceManagerDao(IDeviceManagerDao deviceManagerDao) {
//        this.deviceManagerDao = deviceManagerDao;
//    }

    public void setStorageSource(IStorageSource storageSource) {
        this.storageSource = storageSource;
        storageSource.createTable(DEVICE_TABLE_NAME);
        storageSource.setTablePrimaryKeyName(DEVICE_TABLE_NAME, MAC_COLUMN_NAME);
        storageSource.createTable(DEVICE_ATTACHMENT_POINT_TABLE_NAME);
        storageSource.setTablePrimaryKeyName(DEVICE_ATTACHMENT_POINT_TABLE_NAME, ID_COLUMN_NAME);
        storageSource.createTable(DEVICE_NETWORK_ADDRESS_TABLE_NAME);
        storageSource.setTablePrimaryKeyName(DEVICE_NETWORK_ADDRESS_TABLE_NAME, ID_COLUMN_NAME);
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
    
    // Storage Methods
    
    protected void writeDeviceToStorage(Device device, Date currentDate) {
        if (storageSource != null) {
            try {
                Map<String, Object> rowValues = new HashMap<String, Object>();
                String macString = HexString.toHexString(device.getDataLayerAddress());
                rowValues.put(MAC_COLUMN_NAME, macString);
                rowValues.put(NAME_COLUMN_NAME, "");
//                Integer nwAddr = null;
//                for (Integer a : device.getNetworkAddresses()) { nwAddr = a; }
//                rowValues.put(IP_COLUMN_NAME, (nwAddr == null) ? "" : IPv4.fromIPv4Address(nwAddr));
//                SwitchPortTuple swPort = null;
//                for (SwitchPortTuple t : device.getSwPorts()) { swPort = t; }
//                rowValues.put(SWITCH_COLUMN_NAME, (swPort == null) ? "" : HexString.toHexString(swPort.getSw().getId()));
//                rowValues.put(PORT_COLUMN_NAME, (swPort == null) ? "" : swPort.getPort());
//                device.updateLastSeen();
//                if (device.shouldUpdateLastSeenStorage()) {
//                    rowValues.put(LAST_SEEN_COLUMN_NAME, device.getLastSeenDate());
//                }
//                rowValues.put("id", macString);
                storageSource.updateRow(DEVICE_TABLE_NAME, rowValues);
                
                for (DeviceAttachmentPoint attachmentPoint: device.getAttachmentPoints()) {
                    writeAttachmentPointToStorage(device, attachmentPoint, currentDate);
                }
                for (DeviceNetworkAddress networkAddress: device.getNetworkAddresses()) {
                    writeNetworkAddressToStorage(device, networkAddress, currentDate);
                }
            }
            catch (StorageException e) {
                log.error("Error writing device to storage", e);
            }
        }
    }

    
    protected void writeAttachmentPointToStorage(Device device,
            DeviceAttachmentPoint attachmentPoint, Date currentDate) {
        if (storageSource != null) {
            try {
                assert(device != null);
                assert(attachmentPoint != null);
                String deviceId = HexString.toHexString(device.getDataLayerAddress());
                SwitchPortTuple switchPort = attachmentPoint.getSwitchPort();
                assert(switchPort != null);
                String switchId = HexString.toHexString(switchPort.getSw().getId());
                Short port = switchPort.getPort();
                String attachmentPointId = deviceId + "-" + switchId + "-" + port.toString();
                
                Map<String, Object> rowValues = new HashMap<String, Object>();
                rowValues.put(ID_COLUMN_NAME, attachmentPointId);
                rowValues.put(DEVICE_COLUMN_NAME, deviceId);
                rowValues.put(SWITCH_COLUMN_NAME, switchId);
                rowValues.put(PORT_COLUMN_NAME, port);
                rowValues.put(LAST_SEEN_COLUMN_NAME, attachmentPoint.getLastSeen());
                storageSource.updateRow(DEVICE_ATTACHMENT_POINT_TABLE_NAME, rowValues);
                attachmentPoint.lastSeenWrittenToStorage(currentDate);
            }
            catch (StorageException e) {
                log.error("Error writing device attachment point to storage", e);
            }
        }
    }
    
    protected void writeAttachmentPointLastSeenToStorage(Device device,
            DeviceAttachmentPoint attachmentPoint, Date currentDate) {
        if ((storageSource != null) && attachmentPoint.shouldWriteLastSeenToStorage(currentDate)) {
            try {
                assert(device != null);
                assert(attachmentPoint != null);
                String deviceId = HexString.toHexString(device.getDataLayerAddress());
                SwitchPortTuple switchPort = attachmentPoint.getSwitchPort();
                assert(switchPort != null);
                String switchId = HexString.toHexString(switchPort.getSw().getId());
                Short port = switchPort.getPort();
                String attachmentPointId = deviceId + "-" + switchId + "-" + port.toString();
                
                Map<String, Object> rowValues = new HashMap<String, Object>();
                rowValues.put(ID_COLUMN_NAME, attachmentPointId);
                rowValues.put(LAST_SEEN_COLUMN_NAME, attachmentPoint.getLastSeen());
                storageSource.updateRow(DEVICE_ATTACHMENT_POINT_TABLE_NAME, rowValues);
                attachmentPoint.lastSeenWrittenToStorage(currentDate);
            }
            catch (StorageException e) {
                log.error("Error writing device attachment point to storage", e);
            }
        }
    }
    
    protected void removeAttachmentPointFromStorage(Device device, DeviceAttachmentPoint attachmentPoint) {
        if (storageSource != null) {
            try {
                assert(device != null);
                assert(attachmentPoint != null);
                String deviceId = HexString.toHexString(device.getDataLayerAddress());
                SwitchPortTuple switchPort = attachmentPoint.getSwitchPort();
                assert(switchPort != null);
                String switchId = HexString.toHexString(switchPort.getSw().getId());
                Short port = switchPort.getPort();
                String attachmentPointId = deviceId + "-" + switchId + "-" + port.toString();
                
                storageSource.deleteRow(DEVICE_ATTACHMENT_POINT_TABLE_NAME, attachmentPointId);
            }
            catch (StorageException e) {
                log.error("Error writing device attachment point to storage", e);
            }
        }
    }

    protected void writeNetworkAddressToStorage(Device device,
            DeviceNetworkAddress networkAddress, Date currentDate) {
        if (storageSource != null) {
            try {
                assert(device != null);
                assert(networkAddress != null);
                String deviceId = HexString.toHexString(device.getDataLayerAddress());
                String networkAddressString = IPv4.fromIPv4Address(networkAddress.getNetworkAddress());
                String networkAddressId = deviceId + "-" + networkAddressString;
                
                Map<String, Object> rowValues = new HashMap<String, Object>();
                rowValues.put(ID_COLUMN_NAME, networkAddressId);
                rowValues.put(DEVICE_COLUMN_NAME, deviceId);
                rowValues.put(NETWORK_ADDRESS_COLUMN_NAME, networkAddressString);
                rowValues.put(LAST_SEEN_COLUMN_NAME, networkAddress.getLastSeen());
                storageSource.updateRow(DEVICE_NETWORK_ADDRESS_TABLE_NAME, rowValues);
                networkAddress.lastSeenWrittenToStorage(currentDate);
            }
            catch (StorageException e) {
                log.error("Error writing device attachment point to storage", e);
            }
        }
    }
    
    protected void writeNetworkAddressLastSeenToStorage(Device device,
            DeviceNetworkAddress networkAddress, Date currentDate) {
        if ((storageSource != null) && networkAddress.shouldWriteLastSeenToStorage(currentDate)) {
            try {
                assert(device != null);
                assert(networkAddress != null);
                String deviceId = HexString.toHexString(device.getDataLayerAddress());
                String networkAddressString = IPv4.fromIPv4Address(networkAddress.getNetworkAddress());
                String networkAddressId = deviceId + "-" + networkAddressString;
                
                Map<String, Object> rowValues = new HashMap<String, Object>();
                rowValues.put(ID_COLUMN_NAME, networkAddressId);
                rowValues.put(LAST_SEEN_COLUMN_NAME, networkAddress.getLastSeen());
                storageSource.updateRow(DEVICE_NETWORK_ADDRESS_TABLE_NAME, rowValues);
                networkAddress.lastSeenWrittenToStorage(currentDate);
            }
            catch (StorageException e) {
                log.error("Error writing device attachment point to storage", e);
            }
        }
    }

    protected void removeNetworkAddressFromStorage(Device device, DeviceNetworkAddress networkAddress) {
        if (storageSource != null) {
            try {
                assert(device != null);
                assert(networkAddress != null);
                String deviceId = HexString.toHexString(device.getDataLayerAddress());
                String networkAddressString = IPv4.fromIPv4Address(networkAddress.getNetworkAddress());
                String networkAddressId = deviceId + "-" + networkAddressString;
                
                storageSource.deleteRow(DEVICE_NETWORK_ADDRESS_TABLE_NAME, networkAddressId);
            }
            catch (StorageException e) {
                log.error("Error writing device attachment point to storage", e);
            }
        }
    }


// FIXME: This function isn't being use currently, so I've commented it out.
// When we eventually revive it, it will need to be updated to handle the
// changes for DeviceAttachementPoint and DeviceNetworkAddress
// 
//    public Device readDeviceFromStorage(byte[] dlAddress) {
//        Device returnDevice = null;
//        if (storageSource != null) {
//            try {
//                String macString = HexString.toHexString(dlAddress);
//                IResultSet rs = storageSource.getRow(DEVICE_TABLE_NAME, macString);
//                if (!rs.next())
//                    return null;
//                returnDevice = new Device();
//                returnDevice.setDataLayerAddress(dlAddress);
//                String ipString = rs.getString(NETWORK_ADDRESS_COLUMN_NAME);
//                returnDevice.addNetworkAddress(IPv4.toIPv4Address(ipString));
//                // String switchString = rs.getString(SWITCH_COLUMN_NAME);
//                // Long switchId = HexString.toLong(switchString);
//                // d.setSwId(switchId); // FIXME xyx
//                // d.setSwPort(rs.getShort(PORT_COLUMN_NAME)); // FIXME xyx
//            }
//            catch (StorageException e) {
//                log.error("Error reading device from storage", e);
//            }
//        }
//        return returnDevice;
//    }

    public void removeDeviceFromStorage(Device device) {
        String deviceId = HexString.toHexString(device.getDataLayerAddress());
        
        if (storageSource != null) {
            IResultSet resultSet = null;
            try {
                // Remove all of the attachment points
                resultSet = storageSource.executeQuery(DEVICE_ATTACHMENT_POINT_TABLE_NAME, null,
                                new OperatorPredicate(DEVICE_COLUMN_NAME, OperatorPredicate.Operator.EQ, deviceId), null);
                while (resultSet.next()) {
                    resultSet.deleteRow();
                }
                resultSet.save();
                resultSet.close();
                resultSet = null;
                
                // Remove all of the attachment points
                resultSet = storageSource.executeQuery(DEVICE_NETWORK_ADDRESS_TABLE_NAME, null,
                                new OperatorPredicate(DEVICE_COLUMN_NAME, OperatorPredicate.Operator.EQ, deviceId), null);
                while (resultSet.next()) {
                    resultSet.deleteRow();
                }
                resultSet.save();
                resultSet.close();
                resultSet = null;
                
                // Remove the device
                storageSource.deleteRow(DEVICE_TABLE_NAME, deviceId);
            }
            catch (StorageException e) {
                log.error("Error writing device attachment point to storage", e);
            }
            finally {
                if (resultSet != null)
                    resultSet.close();
            }
        }
    }
}
