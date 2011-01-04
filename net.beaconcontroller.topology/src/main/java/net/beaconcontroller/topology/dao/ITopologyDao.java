package net.beaconcontroller.topology.dao;

import java.util.Set;

import net.beaconcontroller.topology.dao.DaoSwitchPortTuple;
import net.beaconcontroller.topology.dao.DaoLinkTuple;

public interface ITopologyDao {
    /**
     * Clear out all stored state
     */
    public void clear();

    /**
     * Adds the specified link
     * @param lt
     * @param timeStamp the time the link was added
     */
    public void addLink(DaoLinkTuple lt, Long timeStamp);

    /**
     * Updates the last valid time for the specified link
     * @param lt
     * @param timeStamp the time the link was updated
     */
    public void updateLink(DaoLinkTuple lt, Long timeStamp);

    /**
     * Gets the last known active time of the link, or null if it does not exist
     * @param lt link to request info on
     * @return last known active time of the link
     */
    public Long getLink(DaoLinkTuple lt);

    /**
     * Get DaoLinkTuples with an endpoint on the specified switch
     * @param id
     * @return
     */
    public Set<DaoLinkTuple> getLinks(Long id);

    /**
     * Get DaoLinkTuples with an endpoint on the specified switch and port
     * @param idPort
     * @return
     */
    public Set<DaoLinkTuple> getLinks(DaoSwitchPortTuple idPort);

    /**
     * Return all links that have not been updated since the deadline
     * @param deadline
     * @return
     */
    public Set<DaoLinkTuple> getLinksToExpire(Long deadline);

    /**
     * Remove the specified link
     * @param lt
     */
    public void removeLink(DaoLinkTuple lt);

    /**
     * Remove all links with an endpoint on the specified switch
     * @param id
     * @return the set of DaoLinkTuples removed
     */
    public Set<DaoLinkTuple> removeLinksBySwitch(Long id);

    /**
     * Remove all links with an endpoint on the specified switch and port
     * @param idPort
     * @return the set of DaoLinkTuples removed
     */
    public Set<DaoLinkTuple> removeLinksBySwitchPort(DaoSwitchPortTuple idPort);
}
