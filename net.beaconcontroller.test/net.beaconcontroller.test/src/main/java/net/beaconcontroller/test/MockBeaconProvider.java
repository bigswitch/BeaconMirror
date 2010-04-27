package net.beaconcontroller.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openflow.protocol.OFType;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;

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

    @Override
    public void addListener(OFType type, IOFMessageListener listener) {
        if (!listeners.containsKey(type)) {
            listeners.put(type, new ArrayList<IOFMessageListener>());
        }
        listeners.get(type).add(listener);
    }

    @Override
    public void removeListener(OFType type, IOFMessageListener listener) {
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
}
