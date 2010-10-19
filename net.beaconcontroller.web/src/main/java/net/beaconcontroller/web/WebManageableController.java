package net.beaconcontroller.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.beaconcontroller.web.view.BeaconViewResolver;
import net.beaconcontroller.web.view.Tab;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 
 * @author Kyle Forster (kyle.forster@bigswitch.com)
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */

@Controller
public class WebManageableController {
    protected static Logger log = LoggerFactory.getLogger(WebManageableController.class);

    protected List<IWebManageable> webManageables = new ArrayList<IWebManageable>();

    @RequestMapping(value = "wm")
    public String overview(Map<String, Object> model) {
        JSONArray ja = new JSONArray();
        try {
            for (IWebManageable wm : webManageables) {
                JSONObject jo = new JSONObject();
                jo.put("name", wm.getName());
                jo.put("description", wm.getDescription());
                JSONArray tabs = new JSONArray();
                for (Tab tab : wm.getTabs()) {
                    JSONObject jtab = new JSONObject();
                    jtab.put("title", tab.getTitle());
                    jtab.put("url", tab.getUrl());
                    tabs.put(jtab);
                }
                jo.put("tabs", tabs);
                ja.put(jo);
            }
        } catch (JSONException e) {
            log.error("Error encoding webmanageable", e);
        }

        model.put("output", ja.toString());
        return BeaconViewResolver.SIMPLE_JSON_VIEW;
    }

    /**
     * @param webManageables the webManageables to set
     */
    @Autowired
    public void setWebManageables(List<IWebManageable> webManageables) {
        this.webManageables = webManageables;
    }
}
