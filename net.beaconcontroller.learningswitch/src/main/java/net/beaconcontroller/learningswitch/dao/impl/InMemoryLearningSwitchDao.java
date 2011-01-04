package net.beaconcontroller.learningswitch.dao.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.learningswitch.dao.ILearningSwitchDao;

import org.openflow.util.LRULinkedHashMap;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class InMemoryLearningSwitchDao implements ILearningSwitchDao {
    protected Map<IOFSwitch, Map<Integer, Short>> macTables =
        new ConcurrentHashMap<IOFSwitch, Map<Integer, Short>>();
    protected Map<IOFSwitch, ReentrantReadWriteLock> lockTables =
        new ConcurrentHashMap<IOFSwitch, ReentrantReadWriteLock>();

    @Override
    public Short getMapping(IOFSwitch sw, byte[] dataLayerDestination) {
        Map<Integer, Short> macTable = macTables.get(sw);
        if (macTable == null) {
            macTable = new LRULinkedHashMap<Integer, Short>(64001, 64000);
            macTables.put(sw, macTable);
            lockTables.put(sw, new ReentrantReadWriteLock());
        }

        try {
            lockTables.get(sw).readLock().lock();
            return macTable.get(Arrays.hashCode(dataLayerDestination));
        } finally {
            lockTables.get(sw).readLock().unlock();
        }
    }

    @Override
    public void setMapping(IOFSwitch sw, byte[] dataLayerDestination, Short port) {
        Map<Integer, Short> macTable = macTables.get(sw);
        if (macTable == null) {
            macTable = new LRULinkedHashMap<Integer, Short>(64001, 64000);
            macTables.put(sw, macTable);
            lockTables.put(sw, new ReentrantReadWriteLock());
        }

        try {
            lockTables.get(sw).writeLock().lock();
            macTable.put(Arrays.hashCode(dataLayerDestination), port);
        } finally {
            lockTables.get(sw).writeLock().unlock();
        }
    }

    @Override
    public void clearTables() {
        macTables.clear();
        lockTables.clear();
    }
}
