/**
 *
 */
package net.beaconcontroller.routing.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.routing.Link;
import net.beaconcontroller.routing.Route;
import net.beaconcontroller.routing.RouteId;

/**
 * Implementation of the APSP algorithm by Demetrescu and Italiano.
 * Code also based on an implementation in NOX, see http://noxrepo.org
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class AllPairsShortestPathRoutingImpl {
    protected Map<RouteId, Route> shortest;
    protected Map<RouteId, List<Route>> localRoutes;
    protected Map<Route, List<Route>> leftLocal;
    protected Map<Route, List<Route>> leftShortest;
    protected Map<Route, List<Route>> rightLocal;
    protected Map<Route, List<Route>> rightShortest;
    protected List<RouteId> affectedEndpoints = new ArrayList<RouteId>();

    public void update(IOFSwitch srcSwitch, Short srcPort, IOFSwitch dstSwitch,
            Short dstPort, boolean added) {
        Route route = new Route(srcSwitch.getId(), dstSwitch.getId());
        route.getPath().add(new Link(srcPort, dstPort, dstSwitch.getId()));
        cleanup(route, added);
        fixup(route);
    }

    protected void cleanup(Route route, boolean added) {
        List<Route> toClean = new ArrayList<Route>();
        boolean is_short = removeFromShortest(route);
        if (removeFromLocal(route)) {
        }
    }

    protected void fixup(Route route) {
    }

    protected boolean removeFromLocal(Route route) {
        List<Route> routes = this.localRoutes.get(route.getId());
        if (routes != null) {
            for (Iterator<Route> it = routes.iterator(); it.hasNext();) {
                Route rte = it.next();
                if (rte.equals(route)) {
                    it.remove();
                    if (routes.isEmpty()) {
                        this.localRoutes.remove(route.getId());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean removeFromShortest(Route route) {
        if (this.shortest.containsKey(route.getId())
                && this.shortest.get(route.getId()).equals(route)) {
            this.shortest.remove(route.getId());
            return true;
        }
        return false;
    }
}
