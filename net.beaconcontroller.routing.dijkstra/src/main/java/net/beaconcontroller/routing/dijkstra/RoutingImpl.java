/**
 * Beacon component to find shortest paths based on dijkstra's algorithm
 */
package net.beaconcontroller.routing.dijkstra;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.routing.IRoutingEngine;
import net.beaconcontroller.routing.Link;
import net.beaconcontroller.routing.Route;
import net.beaconcontroller.routing.RouteId;
import net.beaconcontroller.topology.ITopologyAware;

/**
 * Beacon component to find shortest paths based on dijkstra's algorithm
 *
 * @author Mandeep Dhami (mandeep.dhami@bigswitch.com)
 */
public class RoutingImpl implements IRoutingEngine, ITopologyAware {
    
    public static final int MAX_LINK_WEIGHT = 1000;
    public static final int MAX_PATH_WEIGHT = Integer.MAX_VALUE - MAX_LINK_WEIGHT - 1;
    public static final int PATH_CACHE_SIZE = 1000;

    protected static Logger log;
    protected ReentrantReadWriteLock lock;
    protected HashMap<Long, TreeMap<Short, Link>> network;
    protected HashMap<Long, HashMap<Long, Link>> nexthoplinkmaps;
    protected HashMap<Long, HashMap<Long, Long>> nexthopnodemaps;
    protected LRUHashMap<RouteId, Route> pathcache;

    protected class NextHop {
        public HashMap<Long, Link> links = null;
        public HashMap<Long, Long> nodes = null;
    }
    
    public RoutingImpl() {
        log = LoggerFactory.getLogger(RoutingImpl.class);
        lock = new ReentrantReadWriteLock();

        network = new HashMap<Long, TreeMap<Short, Link>>();
        nexthoplinkmaps = new HashMap<Long, HashMap<Long, Link>>();
        nexthopnodemaps = new HashMap<Long, HashMap<Long, Long>>();
        pathcache = new LRUHashMap<RouteId, Route>(PATH_CACHE_SIZE);
        
        log.info("Initialized Dijkstra RouterImpl");
    }
    
    @Override
    public boolean routeExists(Long srcId, Long dstId) {
        // self route check
        if (srcId.equals(dstId))
            return true;

        // Check if next hop exists
        lock.readLock().lock();
        HashMap<Long, Long> nexthopnodes = nexthopnodemaps.get(srcId);
        boolean exists = (nexthopnodes!=null) && (nexthopnodes.get(dstId)!=null);
        lock.readLock().unlock();

        return exists;
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        network.clear();
        pathcache.clear();
        nexthoplinkmaps.clear();
        nexthopnodemaps.clear();
        lock.writeLock().unlock();
    }

    @Override
    public boolean routeExists(Long srcId, Long dstId) {
        // self route check
        if (srcId.equals(dstId))
            return true;

        // Check if next hop exists
        lock.readLock().lock();
        HashMap<Long, Long> nexthopnodes = nexthopnodemaps.get(srcId);
        boolean exists = (nexthopnodes!=null) && (nexthopnodes.get(dstId)!=null);
        lock.readLock().unlock();

        return exists;
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        network.clear();
        pathcache.clear();
        nexthoplinkmaps.clear();
        nexthopnodemaps.clear();
        lock.writeLock().unlock();
    }
    
    @Override
    public Route getRoute(IOFSwitch src, IOFSwitch dst) {
        return getRoute(src.getId(), dst.getId());
    }

    @Override
    public Route getRoute(Long srcId, Long dstId) {
        lock.readLock().lock();

        RouteId id = new RouteId(srcId, dstId);
        Route result = null;
        if (pathcache.containsKey(id)) {
            result = pathcache.get(id);
        }
        else {
            result = buildroute(id, srcId, dstId);
            pathcache.put(id, result);
        }

        lock.readLock().unlock();
        log.debug("getRoute: {} -> {}", id, result);
        return result;
    }

    private Route buildroute(RouteId id, Long srcId, Long dstId) {
        LinkedList<Link> path = null;
        HashMap<Long, Link> nexthoplinks = nexthoplinkmaps.get(srcId);
        HashMap<Long, Long> nexthopnodes = nexthopnodemaps.get(srcId);
        
        if (!network.containsKey(srcId)) {
            // This is a switch that is not connected to any other switch
            // hence there was no update for links (and hence it is not in the network)
            log.debug("buildroute: Standalone switch: {}", srcId);
            
            // The only possible non-null path for this case is
            // if srcId == dstId --- and that too is an 'empty' path []
            if (srcId == dstId) path = new LinkedList<Link>();
        }
        else if ((nexthoplinks!=null) && (nexthoplinks.get(dstId)!=null) &&
                 (nexthopnodes!=null) && (nexthopnodes.get(dstId)!=null)) {
            // A valid path exits, calculate it
            path = new LinkedList<Link>();
            while (srcId != dstId) {
                Link l = nexthoplinks.get(dstId);
                path.addFirst(l);
                dstId = nexthopnodes.get(dstId);
            }
        }
        // else, no path exists, and path == null

        Route result = null;
        if (path != null) result = new Route(id, path);
        log.debug("buildroute: {}", result);
        return result;
    }

    @Override
    public void linkUpdate(IOFSwitch src, short srcPort, IOFSwitch dst, short dstPort, boolean added) {
        update(src.getId(), srcPort, dst.getId(), dstPort, added);
    }

    @Override
    public void update(Long srcId, Integer srcPort, Long dstId, Integer dstPort, boolean added) {
        update(srcId, srcPort.shortValue(), dstId, dstPort.shortValue(), added);
    }

    @Override
    public void update(Long srcId, Short srcPort, Long dstId, Short dstPort, boolean added) {
        lock.writeLock().lock();
        boolean network_updated = false;
       
        TreeMap<Short, Link> src = network.get(srcId);
        if (src == null) {
            log.debug("update: new node: {}", srcId);
            src = new TreeMap<Short, Link>();
            network.put(srcId, src);
            network_updated = true;
        }

        TreeMap<Short, Link> dst = network.get(dstId);
        if (dst == null) {
            log.debug("update: new node: {}", dstId);
            dst = new TreeMap<Short, Link>();
            network.put(dstId, dst);
            network_updated = true;
        }

        if (added) {
            Link srcLink = new Link(srcPort, dstPort, dstId);
            if (src.containsKey(srcPort)) {
                log.debug("update: unexpected link add request - srcPort in use src, link: {}, {}", srcId, src.get(srcPort));
            }
            log.debug("update: added link: {}, {}", srcId, srcLink);
            src.put(srcPort, srcLink);
            network_updated = true;
        }
        else {
            // Only remove if that link actually exists.
            if (src.containsKey(srcPort) && src.get(srcPort).getDst()==dstId) {
                log.debug("update: removed link: {}, {}", srcId, srcPort);
                src.remove(srcPort);
                network_updated = true;
            }
            else {
                log.debug("update: unexpected link delete request (ignored) - src, port: {}, {}", srcId, srcPort);
                log.debug("update: current port value is being kept: {}", src.get(srcPort));
            }
        }
       
        if (network_updated) {
        recalculate();
            log.debug("update: dijkstra recalulated");
        }
        else {
            log.debug("update: dijkstra not recalculated");
        }
        
        lock.writeLock().unlock();
        return;
    }

    private void recalculate() {
        pathcache.clear();
        nexthoplinkmaps.clear();
        nexthopnodemaps.clear();
        
        for (Long node : network.keySet()) {
            NextHop nexthop = dijkstra(node);
            nexthoplinkmaps.put(node, nexthop.links);
            nexthopnodemaps.put(node, nexthop.nodes);            
        }
        return;
    }
    
    private class NodeDist implements Comparable<NodeDist> {
        private Long node;
        public Long getNode() {
            return node;
        }

        private int dist; 
        public int getDist() {
            return dist;
        }
        
        public NodeDist(Long node, int dist) {
            this.node = node;
            this.dist = dist;
        }
        
        public int compareTo(NodeDist o) {
            return o.dist - this.dist;
        }
    }
    
    private NextHop dijkstra(Long src) {
        HashMap<Long, Link> nexthoplinks = new HashMap<Long, Link>();
        HashMap<Long, Long> nexthopnodes = new HashMap<Long, Long>();
        HashMap<Long, Integer> dist = new HashMap<Long, Integer>();
        for (Long node: network.keySet()) {
            nexthoplinks.put(node, null);
            nexthopnodes.put(node, null);
            dist.put(node, MAX_PATH_WEIGHT);
        }
        
        HashMap<Long, Boolean> seen = new HashMap<Long, Boolean>();
        PriorityQueue<NodeDist> nodeq = new PriorityQueue<NodeDist>();
        nodeq.add(new NodeDist(src, 0));
        while (nodeq.peek() != null) {
            NodeDist n = nodeq.poll();
            Long cnode = n.getNode();
            int cdist = n.getDist();
            if (cdist >= MAX_PATH_WEIGHT) break;
            if (seen.containsKey(cnode)) continue;
            seen.put(cnode, true);
            
            TreeMap<Short,Link> ports = network.get(cnode);
            for (Short port : ports.keySet()) {
                Link link = ports.get(port);
                Long neighbor = link.getDst();
                int ndist = cdist + 1; // the weight of the link, always 1 in current version of beacon.
                if (ndist < dist.get(neighbor)) {
                    dist.put(neighbor, ndist);
                    nexthoplinks.put(neighbor, link);
                    nexthopnodes.put(neighbor, cnode);
                    nodeq.add(new NodeDist(neighbor, ndist));
                }
            }
        }
        
        NextHop ret = new NextHop();
        ret.links = nexthoplinks;
        ret.nodes = nexthopnodes;
        return ret;
    }

    public void startUp() {}
    public void shutDown() {}

}
