package net.beaconcontroller.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFSwitchListener;

import org.openflow.protocol.OFType;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class MockBeaconProvider implements IBeaconProvider {
    protected Map<OFType, List<IOFMessageListener>> listeners;

    /**
     * 
     */
    public MockBeaconProvider() {
        listeners = new ConcurrentHashMap<OFType, List<IOFMessageListener>>();
    }

    public void addOFMessageListener(OFType type, IOFMessageListener listener) {
        if (!listeners.containsKey(type)) {
            listeners.put(type, new ArrayList<IOFMessageListener>());
        }
        listeners.get(type).add(listener);
    }

    public void removeOFMessageListener(OFType type, IOFMessageListener listener) {
        listeners.get(type).remove(listener);
    }

    /**
     * @return the listeners
     */
    public Map<OFType, List<IOFMessageListener>> getListeners() {
        return listeners;
    }

    /**
     * @param listeners the listeners to set
     */
    public void setListeners(Map<OFType, List<IOFMessageListener>> listeners) {
        this.listeners = listeners;
    }

    @Override
    public Map<Long, IOFSwitch> getSwitches() {
        return null;
    }

    @Override
    public void addOFSwitchListener(IOFSwitchListener listener) {
    }

    @Override
    public void removeOFSwitchListener(IOFSwitchListener listener) {
    }
}
