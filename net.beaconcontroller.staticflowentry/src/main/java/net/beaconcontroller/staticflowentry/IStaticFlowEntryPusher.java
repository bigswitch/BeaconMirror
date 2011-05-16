package net.beaconcontroller.staticflowentry;

import java.util.HashMap;
import java.util.List;

import net.beaconcontroller.core.IOFSwitch;
import org.openflow.protocol.OFFlowMod;

/**
 * Represents the parts of the staticflowentry that are exposed as a service to other beacon apps
 *
 */
public interface IStaticFlowEntryPusher {
    
    // IFFlowMod based functions

    /**
     * Pushes a flow-mod to this switch as a one-time push
     * 
     * @param dpid
     * @param flowMods
     */
    public void pushEntry(long dpid, OFFlowMod fm);

    /**
     * Adds a flow-mod to the list of flow-mods being pushed regularly
     * (and also pushes it right away calling pushEntry, as appropriate) 
     * 
     * @param dpid
     * @param name
     * @param active
     * @param fm
     */
    public void addEntry(long dpid, String name, boolean active, OFFlowMod fm);

    /**
     * Remove a flow-mod entry that has been added previously
     * returns the flow-mod that has just been removed
     * 
     * @param sw
     * @param name
     */
    public OFFlowMod removeEntry(IOFSwitch sw, String name);
 
    /**
     * Get all flow-mod entries that have been pushed previously (for all switches)
     * 
     * returns a HashMap with:
     *             key = switch-id
     *             value = HashMap of entries where each entry in the entry HashMap has
     *                       key = flow-mod-name
     *                       value = OFFlowMod
     * 
     */
    public HashMap<Long, HashMap<String, OFFlowMod>> getEntries();
    
    // JSON based interfaces

    /**
     * Adds a flow-mod (as JSON string) to the list of flow-mods being pushed regularly.
     * If a flow-mod already exists for that switch/name, update it based on new entry.
     * (and also pushes it right away calling pushEntry, as appropriate) 
     * 
     * @param fmJson
     */
    public void addEntry(String fmJson);

    /**
     * Remove a flow-mod entry that has been added previously.
     * Only the switch name and flow-mod name are read from the input argument fmJson,
     * the full JSON for the flow-mod entry being removed is returned.
     * 
     * @param fmJson
     */
    public String removeEntry(String fmJson);
    
    /**
     * Get an array JSON strings representing of all flow-mod entries that have been pushed previously
     * (for all switches)
     * 
     */
    public List<String> getEntryList();

    /** 
     * Get the list of all active switches
     */
    public List<IOFSwitch> getActiveSwitches();

}
