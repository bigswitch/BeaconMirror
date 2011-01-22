package net.beaconcontroller.topology;

import java.util.Map;
import java.util.Set;
import net.beaconcontroller.core.IOFSwitch;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface ITopology {
    /**
     * Query to determine if the specified switch id and port tuple are
     * connected to another switch or not.  If so, this means the link
     * is passing LLDPs properly between two OpenFlow switches.
     * @param idPort
     * @return
     */
    public boolean isInternal(SwitchPortTuple idPort);

    /**
     * Retrieves a map of all known link connections between OpenFlow switches
     * and the last time each link was known to be functioning.
     * @return
     */
    public Map<LinkTuple, Long> getLinks();
    
    /**
     * Retrieves a set of all the switches in the same cluster as sw.
     * A cluster is a set of switches that are directly or indirectly
     * connected via Openflow switches that use the same controller
     * (not necessarily the same controller instance but any controller
     * instance in a group sharing the same network database).
     * @param sw The switch whose cluster we're obtaining
     * @return Set of switches in the cluster
     */
    public Set<IOFSwitch> getSwitchesInCluster(IOFSwitch sw);
    
    /**
     * Queries whether two switches are in the same cluster.
     * @param switch1
     * @param switch2
     * @return true if the switches are in the same cluster
     */
    public boolean inSameCluster(IOFSwitch switch1, IOFSwitch switch2);
    
    /**
     * Returns an unmodifiable map from switch id to a set of all links with it 
     * as an endpoint.
     */
    public Map<IOFSwitch, Set<LinkTuple>> getSwitchLinks();
}
