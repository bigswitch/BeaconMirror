package net.beaconcontroller.topology;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */

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
     * Returns an unmodifiable map from switch id to a set of all links with it 
     * as an endpoint.
     */
    public Map<IOFSwitch, Set<LinkTuple>> getSwitchLinks();
}
