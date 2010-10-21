package net.beaconcontroller.core.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.util.BundleAction;
import net.beaconcontroller.web.IWebManageable;
import net.beaconcontroller.web.view.BeaconJsonView;
import net.beaconcontroller.web.view.BeaconViewResolver;
import net.beaconcontroller.web.view.Tab;
import net.beaconcontroller.web.view.layout.Layout;
import net.beaconcontroller.web.view.layout.TwoColumnLayout;
import net.beaconcontroller.web.view.section.JspSection;
import net.beaconcontroller.web.view.section.StringSection;
import net.beaconcontroller.web.view.section.TableSection;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;

/**
 * This class sets up the web UI component for the structures in
 * net.beaconcontroller.core and related "platform" related input/output.
 * 
 * It uses the net.beaconcontroller.web mgmt framework /
 * 
 * @author Kyle Forster (kyle.forster@bigswitch.com)
 * @author David Erickson (daviderickson@cs.stanford.edu)
 * 
 */
@Controller
@RequestMapping("/core")
public class CoreWebManageable implements BundleContextAware, IWebManageable {
    protected static Logger log = LoggerFactory.getLogger(CoreWebManageable.class);
    protected IBeaconProvider beaconProvider;
    protected BundleContext bundleContext;
    protected List<Tab> tabs;

    public CoreWebManageable() {
        tabs = new ArrayList<Tab>();
        tabs.add(new Tab("Overview", "/wm/core/overview.do"));
        tabs.add(new Tab("OSGi", "/wm/core/osgi.do"));
    }

    /**
     * The bundleContext to set (platform level stuff)
     */
    @Autowired
    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * 
     */
    @Autowired
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

    @Override
    public String getName() {
        return "Core";
    }

    @Override
    public String getDescription() {
        return "Controls the core components of Beacon.";
    }

    @Override
    public List<Tab> getTabs() {
        return tabs;
    }

    @RequestMapping("/overview")
    public String overview(Map<String, Object> model) {
        Layout layout = new TwoColumnLayout();
        model.put("layout", layout);

        // Description
        layout.addSection(
                new StringSection("Welcome",
                        "Thanks for using Beacon!  We hope you find it as amazing as we do!"),
                TwoColumnLayout.COLUMN1);

        // Switch List Table
        List<String> columnNames = new ArrayList<String>();
        columnNames.add("Switch ID");
        columnNames.add("Connect Time");
        List<List<String>> cells = new ArrayList<List<String>>();
        for (IOFSwitch sw : beaconProvider.getSwitches().values()) {
            List<String> row = new ArrayList<String>();
            row.add(HexString.toHexString(sw.getId()));
            row.add(sw.getConnectedSince().toString());
            cells.add(row);
        }
        layout.addSection(new TableSection("Switches", columnNames, cells), TwoColumnLayout.COLUMN2);

        // Listener List Table
        columnNames = new ArrayList<String>();
        columnNames.add("OpenFlow Packet Type");
        columnNames.add("Listeners");
        cells = new ArrayList<List<String>>();
        for (Entry<OFType, List<IOFMessageListener>> entry : beaconProvider.getListeners().entrySet()) {
            List<String> row = new ArrayList<String>();
            row.add(entry.getKey().toString());
            StringBuffer sb = new StringBuffer();
            for (IOFMessageListener listener : entry.getValue()) {
                sb.append(listener.getName() + " ");
            }
            row.add(sb.toString());
            cells.add(row);
        }
        layout.addSection(new TableSection("OpenFlow Packet Listeners", columnNames, cells), TwoColumnLayout.COLUMN2);

        return BeaconViewResolver.SIMPLE_VIEW;
    }

    @RequestMapping("/osgi")
    public String osgi(Map<String, Object> model) {
        Layout layout = new TwoColumnLayout();
        model.put("layout", layout);

        // Bundle List Table
        model.put("bundles", Arrays.asList(this.bundleContext.getBundles()));
        model.put("title", "OSGi Bundles");
        layout.addSection(new JspSection("bundles.jsp", model), TwoColumnLayout.COLUMN1);

        return BeaconViewResolver.SIMPLE_VIEW;
    }

    @RequestMapping("/bundle/{bundleId}/{action}")
    @ResponseBody
    public String osgiAction(@PathVariable Long bundleId, @PathVariable String action) {
        Bundle bundle = this.bundleContext.getBundle(bundleId);
        if (action != null) {
            try {
                if (BundleAction.START.toString().equals(action)) {
                        bundle.start();
                } else if (BundleAction.STOP.toString().equals(action)) {
                        bundle.stop();
                }
            } catch (BundleException e) {
                log.error("Failure performing action " + action + " on bundle " + bundle.getSymbolicName(), e);
            }
        }
        return "";
    }

    @RequestMapping("/switch/{switchId}/flows/")
    public View getSwitchFlows(@PathVariable String switchId, Map<String,Object> model) {
        IOFSwitch sw = beaconProvider.getSwitches().get(HexString.toLong(switchId));
        BeaconJsonView view = new BeaconJsonView();
        if (sw != null) {
            Future<List<OFStatistics>> future;
            OFStatisticsRequest req = new OFStatisticsRequest();
            OFFlowStatisticsRequest fsr = new OFFlowStatisticsRequest();
            OFMatch match = new OFMatch();
            match.setWildcards(0xffffffff);
            fsr.setMatch(match);
            fsr.setOutPort(OFPort.OFPP_NONE.getValue());
            fsr.setTableId((byte) 0xff);
            req.setStatisticType(OFStatisticsType.FLOW);
            req.setStatistics(Collections.singletonList((OFStatistics)fsr));
            req.setLengthU(req.getLengthU() + fsr.getLength());
            try {
                future = sw.getStatistics(req);
                List<OFStatistics> values = future.get(10, TimeUnit.SECONDS);
                model.put(BeaconJsonView.ROOT_OBJECT_KEY, values);
            } catch (Exception e) {
                log.error("Failure retrieving flows", e);
            }
        }

        return view;
    }
}
