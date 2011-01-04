package net.beaconcontroller.devicemanager.dao.impl;

import java.util.HashMap;
import java.util.Map;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.dao.IDeviceManagerDao;
import net.beaconcontroller.packet.IPv4;
import net.beaconcontroller.storage.IResultSet;
import net.beaconcontroller.storage.IStorageSource;

import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSDeviceManagerDao implements IDeviceManagerDao {
    protected static Logger logger = LoggerFactory.getLogger(SSDeviceManagerDao.class);
    protected IStorageSource storageSource;

    private static final String DEVICE_TABLE_NAME = "controller_host";
    private static final String MAC_COLUMN_NAME = "mac";
    private static final String IP_COLUMN_NAME = "ip";
    private static final String SWITCH_COLUMN_NAME = "switch_id";
    private static final String PORT_COLUMN_NAME = "inport";

    public SSDeviceManagerDao() {}

    public void setStorageSource(IStorageSource storageSource) {
        this.storageSource = storageSource;
        storageSource.createTable(DEVICE_TABLE_NAME);
        /*
        try {
            storageSource.createTable(DEVICE_TABLE_NAME);
        }
        catch (Exception exc) {
            // Table may already exist, so ignore error.
            // FIXME: Should check for more specific exception and only
            // ignore the exception if it's because the table already exists.
        }
        */
        storageSource.setTablePrimaryKeyName(DEVICE_TABLE_NAME, "mac");
    }

    protected void addOrUpdateDevice(Device device) {
        Map<String, Object> rowValues = new HashMap<String, Object>();
        String macString = HexString.toHexString(device.getDataLayerAddress());
        rowValues.put(MAC_COLUMN_NAME, macString);
        String ipString = IPv4.fromIPv4Address(device.getNetworkAddresses().isEmpty() ? 0 :
                                               device.getNetworkAddresses().iterator().next());
        rowValues.put(IP_COLUMN_NAME, ipString);
        String switchString = HexString.toHexString(device.getSw().getId());
        rowValues.put(SWITCH_COLUMN_NAME, switchString);
        rowValues.put(PORT_COLUMN_NAME, device.getSwPort());
        rowValues.put("id", macString);
        storageSource.updateRow(DEVICE_TABLE_NAME, rowValues);
    }

    @Override
    public void addDevice(Device device) {
        addOrUpdateDevice(device);
    }

    @Override
    public Device getDevice(byte[] dlAddress) {
        String macString = HexString.toHexString(dlAddress);
        IResultSet rs = storageSource.getRow(DEVICE_TABLE_NAME, macString);
        if (!rs.next())
            return null;
        Device d = new Device();
        d.setDataLayerAddress(dlAddress);
        String ipString = rs.getString(IP_COLUMN_NAME);
        d.getNetworkAddresses().add(IPv4.toIPv4Address(ipString));
        String switchString = rs.getString(SWITCH_COLUMN_NAME);
        Long switchId = HexString.toLong(switchString);
        // d.setSwId(switchId); // FIXME xyx
        d.setSwPort(rs.getShort(PORT_COLUMN_NAME));
        return d;
    }

    @Override
    public void removeDevice(Device device) {
        String macString = HexString.toHexString(device.getDataLayerAddress());
        storageSource.deleteRow(DEVICE_TABLE_NAME, macString);
    }

    @Override
    public void updateDevice(Device device) {
        addOrUpdateDevice(device);
    }
}
