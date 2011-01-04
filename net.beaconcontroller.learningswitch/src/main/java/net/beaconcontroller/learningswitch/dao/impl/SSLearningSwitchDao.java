package net.beaconcontroller.learningswitch.dao.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.learningswitch.dao.ILearningSwitchDao;
import net.beaconcontroller.storage.IResultSet;
import net.beaconcontroller.storage.IStorageSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSLearningSwitchDao implements ILearningSwitchDao {
    protected static Logger logger = LoggerFactory.getLogger(SSLearningSwitchDao.class);
    protected IStorageSource storageSource;

    public void setStorageSource(IStorageSource storageSource) {
        this.storageSource = storageSource;
        storageSource.createTable("switchlearnedmacs");
    }

    @Override
    public Short getMapping(IOFSwitch sw, byte[] dataLayerDestination) {
        IResultSet rs = storageSource.getRow("switchlearnedmacs", Long.toString(sw.getId()));
        String col = Integer.toString(Arrays.hashCode(dataLayerDestination));
        if (!rs.next() || !rs.containsColumn(col))
            return null;
        return rs.getShort(col);
    }

    @Override
    public void setMapping(IOFSwitch sw, byte[] dataLayerDestination, Short port) {
        Map<String, Object> rowValues = new HashMap<String, Object>();
        rowValues.put("id", Long.toString(sw.getId()));
        rowValues.put(Integer.toString(Arrays.hashCode(dataLayerDestination)), port);
        storageSource.updateRow("switchlearnedmacs", rowValues);
    }

    @Override
    public void clearTables() {
        IResultSet resultSet = storageSource.executeQuery("switchlearnedmacs", null, null, null);
        while (resultSet.next())
            resultSet.deleteRow();
        resultSet.save();
    }

}
