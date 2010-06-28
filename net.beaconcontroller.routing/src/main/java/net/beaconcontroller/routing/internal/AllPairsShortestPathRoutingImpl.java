/**
 *
 */
package net.beaconcontroller.routing.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.routing.IRouting;
import net.beaconcontroller.routing.Link;
import net.beaconcontroller.routing.Route;
import net.beaconcontroller.routing.RouteId;
import net.beaconcontroller.topology.TopologyAware;

/**
 * Implementation of the APSP algorithm by Demetrescu and Italiano.
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class AllPairsShortestPathRoutingImpl implements IRouting, TopologyAware {
    public IBeaconProvider beaconProvider;

    protected Map<RouteId, Route> shortest;
    protected Map<RouteId, List<Route>> localRoutes;
    protected Map<Route, List<Route>> leftLocal;
    protected Map<Route, List<Route>> leftShortest;
    protected Map<Route, List<Route>> rightLocal;
    protected Map<Route, List<Route>> rightShortest;

    public AllPairsShortestPathRoutingImpl() {
        shortest = new HashMap<RouteId, Route>();
        localRoutes = new HashMap<RouteId, List<Route>>();
        leftLocal = new HashMap<Route, List<Route>>();
        leftShortest = new HashMap<Route, List<Route>>();
        rightLocal = new HashMap<Route, List<Route>>();
        rightShortest = new HashMap<Route, List<Route>>();
    }

    public void startUp() {
    }

    public void shutDown() {
    }

    @Override
    public void linkAdded(IOFSwitch srcSwitch, short srcPort,
            IOFSwitch dstSwitch, short dstPort) {
        update(srcSwitch.getId(), srcPort, dstSwitch.getId(), dstPort, true);
    }

    @Override
    public void linkRemoved(IOFSwitch srcSwitch, short srcPort,
            IOFSwitch dstSwitch, short dstPort) {
        update(srcSwitch.getId(), srcPort, dstSwitch.getId(), dstPort, false);
    }

    @Override
    public Route getRoute(IOFSwitch src, IOFSwitch dst) {
        return shortest.get(new RouteId(src.getId(), dst.getId()));
    }

    @Override
    public Route getRoute(Long srcDpid, Long dstDpid) {
        return shortest.get(new RouteId(srcDpid, dstDpid));
    }

    /* (non-Javadoc)
     * @see net.beaconcontroller.routing.internal.IRouting#update(net.beaconcontroller.core.IOFSwitch, short, net.beaconcontroller.core.IOFSwitch, short, boolean)
     */
    public void update(Long srcId, short srcPort, Long dstId,
            short dstPort, boolean added) {
        Route route = new Route(srcId, dstId);
        route.getPath().add(new Link(srcPort, dstPort, dstId));
        cleanup(route, added);
        fixup(route, added);
    }

    protected void cleanup(Route route, boolean added) {
        Queue<Route> toClean = new LinkedList<Route>();
        toClean.add(route);

        while (toClean.size() > 0) {
            Route r = toClean.remove();
            Route leftsubPath = subPath(r, true);
            Route rightsubPath = subPath(r, false);
            removeFromLocal(r);
            remove(leftLocal, r, rightsubPath);
            remove(rightLocal, r, leftsubPath);

            if (r.equals(shortest.get(r.getId()))) {
                shortest.remove(r.getId());
                remove(leftShortest, r, rightsubPath);
                remove(rightShortest, r, leftsubPath);
            }

            if (leftLocal.containsKey(r))
                toClean.addAll(leftLocal.get(r));
            if (rightLocal.containsKey(r))
                toClean.addAll(rightLocal.get(r));

// Code from the journal version of the algorithm
//            Set<Route> prefixPostfixRoutes = new HashSet<Route>();
//            if (leftLocal.containsKey(r))
//                prefixPostfixRoutes.addAll(leftLocal.get(r));
//            if (rightLocal.containsKey(r))
//                prefixPostfixRoutes.addAll(rightLocal.get(r));
//
//            for (Route r2 : prefixPostfixRoutes) {
//                toClean.add(r2);
//                Route leftsubPath = subPath(r2, true);
//                Route rightsubPath = subPath(r2, false);
//                removeFromLocal(r2);
//                remove(leftLocal, r2, rightsubPath);
//                remove(rightLocal, r2, leftsubPath);
//
//                if (r2.equals(shortest.get(r2.getId()))) {
//                    shortest.remove(r2.getId());
//                    remove(leftShortest, r2, rightsubPath);
//                    remove(rightShortest, r2, leftsubPath);
//                }
//            }
        }
    }

    protected void fixup(Route route, boolean added) {
        // Phase 1
        if (added) {
            addLocal(route);
            add(leftLocal, route, subPath(route, false));
            add(rightLocal, route, subPath(route, true));
        }

        // Phase 2
        Queue<Route> h = new PriorityQueue<Route>(10, new Comparator<Route>() {
            @Override
            public int compare(Route r1, Route r2) {
                return ((Integer)r1.getPath().size()).compareTo(r2.getPath().size());
            }
        });

        for (Map.Entry<RouteId, List<Route>> entry : localRoutes.entrySet()) {
            h.add(entry.getValue().get(0));
        }

        // Phase 3
        while (!h.isEmpty()) {
            Route r = h.remove();
            if (shortest.containsKey(r.getId())) {
                if (r.compareTo(shortest.get(r.getId())) >= 0)
                    continue;
            } else if (r.getId().getSrc().equals(r.getId().getDst())) {
                continue;
            }

            Route leftSubPath = subPath(r, true);
            Route rightSubPath = subPath(r, false);
            addShortest(r);
            add(leftShortest, r, rightSubPath);
            add(rightShortest, r, leftSubPath);
            addNewLocalRoutes(r, leftSubPath, rightSubPath, h);
        }
    }

    protected void addNewLocalRoutes(Route route, Route leftSubPath, Route rightSubPath, Queue<Route> h) {
        if (leftShortest.containsKey(leftSubPath))
            for (Route r : leftShortest.get(leftSubPath)) {
                Route newLocal = null;
                try {
                    newLocal = (Route) route.clone();
                } catch (CloneNotSupportedException e) {
                }
                newLocal.getPath().add(0, r.getPath().get(0));
                newLocal.getId().setSrc(r.getId().getSrc());
                addLocal(newLocal);
                add(leftLocal, newLocal, route);
                add(rightLocal, newLocal, r);
                h.add(newLocal);
            }

        if (rightShortest.containsKey(rightSubPath))
            for (Route r : rightShortest.get(rightSubPath)) {
                Route newLocal = null;
                try {
                    newLocal = (Route) route.clone();
                } catch (CloneNotSupportedException e) {
                }
                newLocal.getPath().add(r.getPath().get(r.getPath().size()-1));
                newLocal.getId().setDst(r.getId().getDst());
                addLocal(newLocal);
                add(leftLocal, newLocal, r);
                add(rightLocal, newLocal, route);
                h.add(newLocal);
            }
    }

    protected void addLocal(Route route) {
        if (!localRoutes.containsKey(route.getId())) {
            localRoutes.put(route.getId(), new ArrayList<Route>());
            localRoutes.get(route.getId()).add(route);
        } else {
            List<Route> routes = localRoutes.get(route.getId());
            for (int i = 0; i < routes.size(); ++i) {
                if (route.compareTo(routes.get(i)) < 0) {
                    routes.add(i, route);
                    return;
                }
            }
        }
    }

    protected void addShortest(Route route) {
        shortest.put(route.getId(), route);
    }

    protected void add(Map<Route, List<Route>> routeMap, Route route, Route subPath) {
        if (!routeMap.containsKey(subPath))
            routeMap.put(subPath, new ArrayList<Route>());
        routeMap.get(subPath).add(route);
    }

    protected boolean removeFromLocal(Route route) {
        List<Route> routes = this.localRoutes.get(route.getId());
        if (routes != null) {
            if (routes.remove(route)) {
                if (routes.isEmpty()) {
                    this.localRoutes.remove(route.getId());
                }
                return true;
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

    /**
     * Removes the given route from the list in routeMap indexed by subPath,
     * if it exists.  Returns true if it was removed, false otherwise.
     * @param routeMap
     * @param route
     * @param subPath
     * @return
     */
    protected boolean remove(Map<Route, List<Route>> routeMap, Route route, Route subPath) {
        List<Route> routes = routeMap.get(subPath);
        if (routes != null) {
            if (routes.remove(route)) {
                if (routes.isEmpty())
                    routeMap.remove(subPath);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the subPath of the given route, namely l(route) or r(route) as
     * given in the algorithm. The returned Route is a clone of the supplied
     * Route and safe for modification.
     * @param route
     * @param isLeft
     * @return
     */
    protected Route subPath(Route route, boolean isLeft) {
        Route clone = null;
        try {
            clone = (Route) route.clone();
        } catch (CloneNotSupportedException e) {
            // this will never happen
        }
        List<Link> path = clone.getPath();
        if (isLeft) {
            if (path.size() > 0)
                path.remove(path.size()-1);
            if (path.isEmpty())
                clone.getId().setDst(clone.getId().getSrc());
        } else {
            if (path.size() > 0)
                path.remove(0);
            if (path.isEmpty())
                clone.getId().setSrc(clone.getId().getDst());
        }
        return clone;
    }

    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }
}
