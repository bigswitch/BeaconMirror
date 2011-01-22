package net.beaconcontroller.topology.visualweb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.topology.ITopology;
import net.beaconcontroller.topology.LinkTuple;
import net.beaconcontroller.web.IWebManageable;
import net.beaconcontroller.web.view.BeaconJsonView;
import net.beaconcontroller.web.view.BeaconViewResolver;
import net.beaconcontroller.web.view.Tab;
import net.beaconcontroller.web.view.layout.Layout;
import net.beaconcontroller.web.view.layout.OneColumnLayout;
import net.beaconcontroller.web.view.layout.TwoColumnLayout;
import net.beaconcontroller.web.view.section.JspSection;
import net.beaconcontroller.web.view.section.StringSection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.View;

/**
 * This class sets up the web UI component for the topology bundle,
 * creating a topology viewer.
 * 
 * It uses the net.beaconcontroller.web mgmt framework /
 * 
 * @author Kyle Forster (kyle.forster@bigswitch.com)
 * @author David Erickson (daviderickson@cs.stanford.edu)
 * 
 */
@Controller()
@RequestMapping("/visualtopology")
public class VisualWebManageable implements IWebManageable {
    protected static Logger log = LoggerFactory.getLogger(VisualWebManageable.class);
    protected IBeaconProvider beaconProvider;
    protected ITopology topologyProvider;
    protected List<Tab> tabs;

    protected Map<Long, SwitchView> switchViews;

    public VisualWebManageable() {
        tabs = new ArrayList<Tab>();
        tabs.add(new Tab("Overview", "/wm/visualtopology/overview.do"));
        tabs.add(new Tab("Topology Viewer", "/wm/visualtopology/topoview.do"));
        switchViews = new HashMap<Long, SwitchView>();
    }

    @Autowired
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

    @Autowired
    public void setTopology(ITopology topologyProvider) {
        this.topologyProvider = topologyProvider;
    }

    @Override
    public String getName() {
        return "VisualTopology";
    }

    @Override
    public String getDescription() {
        return "An LLDP-based topology discovery bundle";
    }

    @Override
    public List<Tab> getTabs() {
        return tabs;
    }


    public void linkAdded(IOFSwitch srcSwitch, short srcPort, IOFSwitch dstSwitch, short dstPort) {
        // TODO Auto-generated method stub - perhaps use these later to initialize the SwitchView structure?      
    }

    public void linkRemoved(IOFSwitch srcSwitch, short srcPort, IOFSwitch dstSwitch, short dstPort) {
        // TODO Auto-generated method stub
    }


    /**
     * Get or create a SwitchView structure for the switch corresponding to this switchId
     * @param dpid
     */
    protected SwitchView getSwitchView(Long switchId) {
        SwitchView sv = switchViews.get(switchId);
        if(sv == null) {
            sv = new SwitchView();
            sv.put("$color", "#CCCCCC");
            sv.put("$type", "circle");
            switchViews.put(switchId, sv);
        }
        return sv;
    }

    @RequestMapping("/overview")
    public String overview(Map<String, Object> model) {
        Layout layout = new TwoColumnLayout();
        model.put("layout", layout);

        // Description
        layout.addSection(
                new StringSection("Welcome",
                "Thanks for using the topology viewer!  Lorem ipsum dolor amet"),
                TwoColumnLayout.COLUMN1);

        // Link List Table
        //model.put("title", "Links");
        //model.put("links", topologyProvider.getLinks());
        //layout.addSection(new JspSection("links.jsp", model), TwoColumnLayout.COLUMN2);

        return BeaconViewResolver.SIMPLE_VIEW;
    }

    @RequestMapping("/topoview")
    public String topoview(Map<String, Object> model) {
        Layout layout = new OneColumnLayout();
        model.put("title", "Topology View");
        //Map<Long, Set<LinkTuple>> switchLinks = topologyProvider.getSwitchLinks();
        layout.addSection(new JspSection("topoview.jsp", model), null);
        model.put("layout", layout);
        return BeaconViewResolver.SIMPLE_VIEW;
    }

    /**
     * Returns the topology object as a json object in the jit format:
     * 
     * [
     * {
      "adjacencies": [
        {
          "nodeTo": "graphnode1", 
          "nodeFrom": "graphnode0", 
          "data": {}
        }, 
        {
          "nodeTo": "graphnode3", 
          "nodeFrom": "graphnode0", 
          "data": {}
        }], 
      "data": {
        "$color": "#83548B", 
        "$type": "circle"
       }, 
      "id": "graphnode0", 
      "name": "graphnode0
      },...]
     * 
     * @param model
     * @return
     */
    @RequestMapping("/topoview/links/json")
    public View getTopologyJson(Map<String,Object> model) {
        BeaconJsonView view = new BeaconJsonView();
        List<Map<String,?>> nodes = new LinkedList<Map<String,?>>();
        Map<IOFSwitch, Set<LinkTuple>> switchLinks = topologyProvider.getSwitchLinks();
        for(Entry<IOFSwitch, Set<LinkTuple>> e : switchLinks.entrySet()) {
            Map<String, Object> node = new HashMap<String, Object>();
            Long dpid = e.getKey().getId();
            Set<LinkTuple> links = e.getValue();
            SwitchView sv = this.getSwitchView(dpid);
            node.put("id", dpid);
            node.put("name", dpid);

            if(sv != null)
                node.put("data", sv.getAsMap());
            else
                log.warn("Timing error - switch is available via topologyProvider.getSwitchLinks but the linkAdded call " +
                "has not yet been triggered"); //this should never happen

            List<Map<String, String>> adjacencies = new LinkedList<Map<String, String>>();
            for(LinkTuple lt : links) {
                Map<String, String> adjacency = new HashMap<String, String>();
                String srcID = "" + lt.getSrc().getSw().getId();
                String dstID = "" + lt.getDst().getSw().getId();
                adjacency.put("nodeFrom", srcID);
                adjacency.put("nodeTo", dstID);
                adjacencies.add(adjacency);
            }
            node.put("adjacencies", adjacencies);
            nodes.add(node);
        }
        model.put(BeaconJsonView.ROOT_OBJECT_KEY, nodes);
        return view;
    }

    /**
     * Called when posting up new view information about a switch (e.g. position).  Note that if multiple values are found under
     * the same key, only the first will be saved while the rest will ignored.
     * 
     * Example:
     * Assuming you have jquery already imported in to a browser, the following javascript will augment the SwitchView corresponding
     * to switchId with key1/value1 and key2/value2:
     * $.post('/wm/topology/topoview/switch/<switchId>/switchView', {key1:'value1', key2:'value2'});
     * 
     * @param model
     * @param switchId
     * @param body
     * @return a BeaconJsonView with the updated SwitchView structure
     */
    @RequestMapping(value = "/topoview/switch/{switchId}/switchView", method = RequestMethod.POST)
    public View switchView(Map<String,Object> model, @PathVariable Long switchId, @RequestBody MultiValueMap<String, String> body) {
        BeaconJsonView view = new BeaconJsonView();
        SwitchView sv =  this.getSwitchView(switchId);
        for(Map.Entry<String, List<String>> e : body.entrySet()) {
            String key = e.getKey();
            String val = e.getValue().get(0);
            sv.put(key, val);
        }
        log.debug("switchView (post) for " + switchId + ": " + sv + ", request body: " + body);
        model.put(BeaconJsonView.ROOT_OBJECT_KEY, sv.getAsMap());
        return view;
    }

    /**
     * Simple internal data structure used to store bits and pieces about a switch used for
     * viewing.  Currently just a simple wrapper around a Map<String, String>, but may add typing
     * and defaults later.
     * 
     * (Note this should roughly correspond to the jit node.data structure.)
     */
    protected class SwitchView {
        Map<String, String> viewData = new HashMap<String, String>();

        protected String get(String paramName) {
            return viewData.get(paramName);
        }
        protected String put(String paramName, String value) {
            return viewData.put(paramName, value);
        }
        protected Set<Map.Entry<String, String>> entrySet() {
            return viewData.entrySet();
        }

        /**
         * Short term hack used for serialization
         * @return
         */
        protected Map<String, String> getAsMap() {
            return Collections.unmodifiableMap(viewData);
        }

        public String toString() {
            return viewData.toString();
        }

    }
}

