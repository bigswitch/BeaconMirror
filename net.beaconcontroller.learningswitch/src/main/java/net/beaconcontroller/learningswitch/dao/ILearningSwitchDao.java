package net.beaconcontroller.learningswitch.dao;

import net.beaconcontroller.core.IOFSwitch;

/**
 * Objects implementing this interface should be threadsafe
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public interface ILearningSwitchDao {
    /**
     * Gets the given port mapping for a given switch and destination MAC, null
     * if none exists
     * @param sw
     * @param dataLayerDestination
     * @return
     */
    public Short getMapping(IOFSwitch sw, byte[] dataLayerDestination);

    public void setMapping(IOFSwitch sw, byte[] dataLayerDestination, Short port);

    /**
     * Clear all data from the MAC tables
     */
    public void clearTables();
}
