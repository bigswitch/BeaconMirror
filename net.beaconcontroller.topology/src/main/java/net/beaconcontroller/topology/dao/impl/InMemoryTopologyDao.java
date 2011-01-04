package net.beaconcontroller.topology.dao.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.beaconcontroller.topology.dao.DaoSwitchPortTuple;
import net.beaconcontroller.topology.dao.DaoLinkTuple;
import net.beaconcontroller.topology.dao.ITopologyDao;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class InMemoryTopologyDao implements ITopologyDao {
    /**
     * Map from link to the most recent time it was verified functioning
     */
    protected Map<DaoLinkTuple, Long> links;

    /**
     * Map from a id:port to the set of links containing it as an endpoint
     */
    protected Map<DaoSwitchPortTuple, Set<DaoLinkTuple>> portLinks;

    /**
     * Map from switch id to a set of all links with it as an endpoint
     */
    protected Map<Long, Set<DaoLinkTuple>> switchLinks;

    public InMemoryTopologyDao() {
        links = new HashMap<DaoLinkTuple, Long>();
        portLinks = new HashMap<DaoSwitchPortTuple, Set<DaoLinkTuple>>();
        switchLinks = new HashMap<Long, Set<DaoLinkTuple>>();
    }

    @Override
    public void addLink(DaoLinkTuple lt, Long timeStamp) {
        // index it by switch source
        if (!switchLinks.containsKey(lt.getSrc().getId()))
            switchLinks.put(lt.getSrc().getId(), new HashSet<DaoLinkTuple>());
        switchLinks.get(lt.getSrc().getId()).add(lt);
    
        // index it by switch dest
        if (!switchLinks.containsKey(lt.getDst().getId()))
            switchLinks.put(lt.getDst().getId(), new HashSet<DaoLinkTuple>());
        switchLinks.get(lt.getDst().getId()).add(lt);
    
        // index both ends by switch:port
        if (!portLinks.containsKey(lt.getSrc()))
            portLinks.put(lt.getSrc(), new HashSet<DaoLinkTuple>());
        portLinks.get(lt.getSrc()).add(lt);
    
        if (!portLinks.containsKey(lt.getDst()))
            portLinks.put(lt.getDst(), new HashSet<DaoLinkTuple>());
        portLinks.get(lt.getDst()).add(lt);

        links.put(lt, timeStamp);
    }

    @Override
    public void updateLink(DaoLinkTuple lt, Long timeStamp) {
        links.put(lt, timeStamp);
    }

    @Override
    public void removeLink(DaoLinkTuple lt) {
        this.switchLinks.get(lt.getSrc().getId()).remove(lt);
        this.switchLinks.get(lt.getDst().getId()).remove(lt);
        if (this.switchLinks.containsKey(lt.getSrc().getId()) &&
                this.switchLinks.get(lt.getSrc().getId()).isEmpty())
            this.switchLinks.remove(lt.getSrc().getId());
        if (this.switchLinks.containsKey(lt.getDst().getId()) &&
                this.switchLinks.get(lt.getDst().getId()).isEmpty())
            this.switchLinks.remove(lt.getDst().getId());

        this.portLinks.get(lt.getSrc()).remove(lt);
        this.portLinks.get(lt.getDst()).remove(lt);
        if (this.portLinks.get(lt.getSrc()).isEmpty())
            this.portLinks.remove(lt.getSrc());
        if (this.portLinks.get(lt.getDst()).isEmpty())
            this.portLinks.remove(lt.getDst());

        this.links.remove(lt);
    }

    @Override
    public Set<DaoLinkTuple> removeLinksBySwitch(Long id) {
        Set<DaoLinkTuple> eraseList = new HashSet<DaoLinkTuple>();
        if (switchLinks.containsKey(id)) {
            // add all tuples with an endpoint on this switch to erase list
            eraseList.addAll(switchLinks.get(id));

            for (DaoLinkTuple lt : switchLinks.get(id)) {
                // cleanup id:port->links map
                // check src
                if (lt.getSrc().getId().equals(id)) {
                    portLinks.remove(lt.getSrc());
                } else {
                    if (portLinks.get(lt.getSrc()) != null) {
                        portLinks.get(lt.getSrc()).remove(lt);
                        if (portLinks.get(lt.getSrc()).isEmpty())
                            portLinks.remove(lt.getSrc());
                    }

                    this.switchLinks.get(lt.getSrc().getId()).remove(lt);
                    if (this.switchLinks.get(lt.getSrc().getId()).isEmpty())
                        this.switchLinks.remove(lt.getSrc().getId());
                }

                // check dst
                if (lt.getDst().getId().equals(id)) {
                    portLinks.remove(lt.getDst());
                } else {
                    if (portLinks.get(lt.getDst()) != null) {
                        portLinks.get(lt.getDst()).remove(lt);
                        if (portLinks.get(lt.getDst()).isEmpty())
                            portLinks.remove(lt.getDst());
                    }

                    this.switchLinks.get(lt.getDst().getId()).remove(lt);
                    if (this.switchLinks.get(lt.getDst().getId()).isEmpty())
                        this.switchLinks.remove(lt.getDst().getId());
                }

                // cleanup link->timeout map
                this.links.remove(lt);
            }
            switchLinks.remove(id);
        }
        return eraseList;
    }

    @Override
    public Set<DaoLinkTuple> removeLinksBySwitchPort(DaoSwitchPortTuple idPort) {
        Set<DaoLinkTuple> eraseList = new HashSet<DaoLinkTuple>();
        if (this.portLinks.containsKey(idPort)) {
            eraseList.addAll(this.portLinks.get(idPort));
            for (DaoLinkTuple lt : this.portLinks.get(idPort)) {
                // cleanup id:port->links map
                // check src
                if (!lt.getSrc().equals(idPort))
                    if (portLinks.get(lt.getSrc()) != null) {
                        portLinks.get(lt.getSrc()).remove(lt);
                        if (portLinks.get(lt.getSrc()).isEmpty())
                            portLinks.remove(lt.getSrc());
                    }
                else
                    if (portLinks.get(lt.getDst()) != null) {
                        portLinks.get(lt.getDst()).remove(lt);
                        if (portLinks.get(lt.getDst()).isEmpty())
                            portLinks.remove(lt.getDst());
                    }

                // cleanup swid->links map
                this.switchLinks.get(idPort.getId()).remove(lt);
                if (this.switchLinks.get(idPort.getId()).isEmpty())
                    this.switchLinks.remove(idPort.getId());

                // cleanup link->timeout map
                this.links.remove(lt);
                
            }
        }
        return eraseList;
    }

    @Override
    public Long getLink(DaoLinkTuple lt) {
        return links.get(lt);
    }

    @Override
    public Set<DaoLinkTuple> getLinksToExpire(Long deadline) {
        Set<DaoLinkTuple> expireList = new HashSet<DaoLinkTuple>();

        Iterator<Entry<DaoLinkTuple, Long>> it = this.links.entrySet().iterator();
        while (it.hasNext()) {
            Entry<DaoLinkTuple, Long> entry = it.next();
            if (entry.getValue() <= deadline) {
                expireList.add(entry.getKey());
            }
        }
        return expireList;
    }

    @Override
    public Set<DaoLinkTuple> getLinks(DaoSwitchPortTuple idPort) {
        return portLinks.get(idPort);
    }

    @Override
    public Set<DaoLinkTuple> getLinks(Long id) {
        return this.switchLinks.get(id);
    }

    @Override
    public void clear() {
        this.links.clear();
        this.portLinks.clear();
        this.switchLinks.clear();
    }
}
