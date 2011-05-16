package net.beaconcontroller.staticflowentry.web;

/**
 * This class sets up the web UI component for the structures in
 * net.beaconcontroller.counter -- the counter store.
 * 
 * It uses the net.beaconcontroller.web mgmt framework
 * 
 */

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import net.beaconcontroller.staticflowentry.IStaticFlowEntryPusher;
import net.beaconcontroller.web.IWebManageable;
import net.beaconcontroller.web.view.BeaconViewResolver;
import net.beaconcontroller.web.view.Tab;
import net.beaconcontroller.web.view.layout.Layout;
import net.beaconcontroller.web.view.layout.OneColumnLayout;
import net.beaconcontroller.web.view.section.JspSection;

import org.codehaus.jackson.map.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/staticflowentry")
public class StaticFlowEntryWebManageable implements IWebManageable {
    protected static Logger log = LoggerFactory.getLogger(StaticFlowEntryWebManageable.class);
    protected IStaticFlowEntryPusher flowPusher;
    protected List<Tab> tabs;

    /**
     * Autowired to set the flow pusher instance
     * @param flowPusher
     */
    @Autowired
    public void setStaticFlowEntryPusher(IStaticFlowEntryPusher flowPusher) {
      this.flowPusher = flowPusher;
    }
    
    @Override
    public String getName() {
        return "Static Flows";
    }

    @Override
    public String getDescription() {
        return "Push static flows out to switches.";
    }

    @Override
    public List<Tab> getTabs() {
        return tabs;
    }

    public StaticFlowEntryWebManageable() {
        tabs = new ArrayList<Tab>();
        tabs.add(new Tab("Overview", "/wm/staticflowentry/overview.do"));
    }
    
    @RequestMapping("/overview")
    public String overview(Map<String, Object> model) {
        Layout layout = new OneColumnLayout();
        model.put("layout", layout);
        
        String[] flowmod_attr = {
                "active",
                "name",
                "switch",
                "priority",
                "cookie",
                "wildcards",
                "ingress-port",
                "vlan-id",
                "vlan-priority",
                "ether-type",
                "src-mac",
                "dst-mac",
                "protocol",
                "tos-bits",
                "src-ip",
                "dst-ip",
                "src-port",
                "dst-port",
                "actions",
        };

        List<String> flowmods_json = this.flowPusher.getEntryList();
        ArrayList<HashMap<String, String>> flowmods = new ArrayList<HashMap<String, String>>();
        ObjectMapper mapper = new ObjectMapper();
        for (String flowmod_json : flowmods_json) {
                try {
                    @SuppressWarnings("unchecked")
                    HashMap<String, String> flowmod_data = mapper.readValue(flowmod_json, HashMap.class);
                    HashMap<String, String> flowmod = new HashMap<String, String>();
                    for (String name : flowmod_data.keySet()) {
                        flowmod.put(name.replace('-', '_'), flowmod_data.get(name));
                    }
                    flowmod.put("json", flowmod_json);
                    flowmods.add(flowmod);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
        }

        model.put("title", "Static Flow Entries");
        model.put("flowmods", flowmods);
        model.put("attrs", flowmod_attr);
        layout.addSection(
                new JspSection("flowmod.jsp", model),
                null);

        return BeaconViewResolver.SIMPLE_VIEW;
    }
    

    /**
     * Rest Handler
     * 
     * It handles a POST or a GET request that must include variable:
     *  dpid - The switch DPID either as hex string or long
     *  postBody - the requested flow-mods as a JSON array of objects
     *  
     * NOTE: the variable is called postBody, but can be either in the body for the POST
     * or in the url for the GET. This is a crummy/insecure hack to allow easier cross-domain
     * communication for now.
     *  
     * This function will parse json object in to a flow table entry (see StaticFlowEntryPusher)
     * and pushes it out to the switch.
     * 
     * @param model
     * @param dpid
     * @param postBody
     * @param req
     * @return
     */
    @RequestMapping("/pushentry")
    public String pushEntry(Map<String, Object> model, 
                            @RequestParam(required=false) String postBody,
                            HttpServletRequest req) throws IOException {

        if (postBody == null && req.getMethod().equals("POST")) {
            final char[] buffer = new char[0x10000];
            StringBuilder out = new StringBuilder();
            Reader in;
            in = req.getReader();
            int read = 1;
            while(read >= 0) {
              read = in.read(buffer, 0, buffer.length);
              if (read>0)
                out.append(buffer, 0, read); 
            };
            postBody = out.toString();
        }
        
        flowPusher.addEntry(postBody);
        model.put("output", "OK");
        return BeaconViewResolver.SIMPLE_JSON_VIEW;
    }
    
}
