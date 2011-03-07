package net.beaconcontroller.core.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import net.beaconcontroller.web.view.layout.OneColumnLayout;
import net.beaconcontroller.web.view.layout.TwoColumnLayout;
import net.beaconcontroller.web.view.section.JspSection;
import net.beaconcontroller.web.view.section.StringSection;
import net.beaconcontroller.web.view.section.TableSection;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFTableStatistics;
import org.openflow.protocol.statistics.OFAggregateStatisticsRequest;
import org.openflow.protocol.statistics.OFQueueStatisticsRequest;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
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
    protected PackageAdmin packageAdmin;
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
                        "Thanks for using Beacon!"),
                TwoColumnLayout.COLUMN1);

        // Switch List Table
        model.put("title", "Switches");
        model.put("switches", beaconProvider.getSwitches().values());
        layout.addSection(new JspSection("switches.jsp", model), TwoColumnLayout.COLUMN2);

        // Listener List Table
        List<String> columnNames = new ArrayList<String>();
        List<List<String>> cells = new ArrayList<List<String>>();
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
        layout.addSection(new TableSection("OpenFlow Packet Listeners", columnNames, cells), TwoColumnLayout.COLUMN1);

        return BeaconViewResolver.SIMPLE_VIEW;
    }

    @RequestMapping("/osgi")
    public String osgi(Map<String, Object> model) {
        Layout layout = new OneColumnLayout();
        model.put("layout", layout);

        // Bundle Form
        model.put("title", "Add Bundle");
        layout.addSection(new JspSection("addBundle.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);

        // Bundle List Table
        model.put("bundles", Arrays.asList(this.bundleContext.getBundles()));
        model.put("title", "OSGi Bundles");
        layout.addSection(new JspSection("bundles.jsp", model), TwoColumnLayout.COLUMN1);

        return BeaconViewResolver.SIMPLE_VIEW;
    }

    @RequestMapping("/bundle/{bundleId}/{action}")
    @ResponseBody
    public String osgiAction(@PathVariable Long bundleId, @PathVariable String action) {
        final Bundle bundle = this.bundleContext.getBundle(bundleId);
        if (action != null) {
            try {
                if (BundleAction.START.toString().equalsIgnoreCase(action)) {
                    bundle.start();
                } else if (BundleAction.STOP.toString().equalsIgnoreCase(action)) {
                    bundle.stop();
                } else if (BundleAction.UNINSTALL.toString().equalsIgnoreCase(action)) {
                    bundle.uninstall();
                } else if (BundleAction.REFRESH.toString().equalsIgnoreCase(action)) {
                    packageAdmin.refreshPackages(new Bundle[] {bundle});
                }
            } catch (BundleException e) {
                log.error("Failure performing action " + action + " on bundle " + bundle.getSymbolicName(), e);
            }
        }
        return "";
    }

    @RequestMapping(value = "/bundle/add", method = RequestMethod.POST)
    public View osgiBundleAdd(@RequestParam("file") MultipartFile file, Map<String, Object> model) throws Exception {
        BeaconJsonView view = new BeaconJsonView();

        File tempFile = null;
        Bundle newBundle = null;
        try {
            tempFile = File.createTempFile("beacon", ".jar");
            file.transferTo(tempFile);
            tempFile.deleteOnExit();
            newBundle = bundleContext.installBundle("file:"+tempFile.getCanonicalPath());
            model.put(BeaconJsonView.ROOT_OBJECT_KEY,
                    "Successfully installed: " + newBundle.getSymbolicName()
                            + "_" + newBundle.getVersion());
        } catch (IOException e) {
            log.error("Failure to create temporary file", e);
            model.put(BeaconJsonView.ROOT_OBJECT_KEY, "Failed to install bundle.");
        } catch (BundleException e) {
            log.error("Failure installing bundle", e);
            model.put(BeaconJsonView.ROOT_OBJECT_KEY, "Failed to install bundle.");
        }
        view.setContentType("text/javascript");
        return view;
    }

    protected List<OFStatistics> getSwitchStatistics(String switchId, OFStatisticsType statType) {
        IOFSwitch sw = beaconProvider.getSwitches().get(HexString.toLong(switchId));
        Future<List<OFStatistics>> future;
        List<OFStatistics> values = null;
        if (sw != null) {
            OFStatisticsRequest req = new OFStatisticsRequest();
            req.setStatisticType(statType);
            int requestLength = req.getLengthU();
            if (statType == OFStatisticsType.FLOW) {
                OFFlowStatisticsRequest specificReq = new OFFlowStatisticsRequest();
                OFMatch match = new OFMatch();
                match.setWildcards(0xffffffff);
                specificReq.setMatch(match);
                specificReq.setOutPort(OFPort.OFPP_NONE.getValue());
                specificReq.setTableId((byte) 0xff);
                req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
                requestLength += specificReq.getLength();
            } else if (statType == OFStatisticsType.AGGREGATE) {
                OFAggregateStatisticsRequest specificReq = new OFAggregateStatisticsRequest();
                OFMatch match = new OFMatch();
                match.setWildcards(0xffffffff);
                specificReq.setMatch(match);
                specificReq.setOutPort(OFPort.OFPP_NONE.getValue());
                specificReq.setTableId((byte) 0xff);
                req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
                requestLength += specificReq.getLength();
            } else if (statType == OFStatisticsType.PORT) {
                OFPortStatisticsRequest specificReq = new OFPortStatisticsRequest();
                specificReq.setPortNumber((short)OFPort.OFPP_NONE.getValue());
                req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
                requestLength += specificReq.getLength();
            } else if (statType == OFStatisticsType.QUEUE) {
                OFQueueStatisticsRequest specificReq = new OFQueueStatisticsRequest();
                specificReq.setPortNumber((short)OFPort.OFPP_ALL.getValue());
                // LOOK! openflowj does not define OFPQ_ALL! pulled this from openflow.h
                // note that I haven't seen this work yet though...
                specificReq.setQueueId(0xffffffff);
                req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
                requestLength += specificReq.getLength();
            } else if (statType == OFStatisticsType.DESC ||
                       statType == OFStatisticsType.TABLE) {
                // pass - nothing todo besides set the type above
            }
            req.setLengthU(requestLength);
            try {
                future = sw.getStatistics(req);
                values = future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Failure retrieving statistics from switch {}", sw, e);
            }
        }
        return values;
    }

    @RequestMapping("/switch/{switchId}/flows")
    public String getSwitchFlows(@PathVariable String switchId, Map<String,Object> model) {
        OneColumnLayout layout = new OneColumnLayout();
        model.put("title", "Flows for switch: " + switchId);
        model.put("layout", layout);
        model.put("flows", getSwitchStatistics(switchId, OFStatisticsType.FLOW));
        layout.addSection(new JspSection("flows.jsp", model), null);
        return BeaconViewResolver.SIMPLE_VIEW;
    }
    
    @RequestMapping("/switch/{switchId}/{statType}/json")
    public View getSwitchStatisticsJson(@PathVariable String switchId, @PathVariable String statType, Map<String,Object> model) {
        BeaconJsonView view = new BeaconJsonView();
        List<OFStatistics> values = null;
        if (statType.equals("port")) {
            values = getSwitchStatistics(switchId, OFStatisticsType.PORT);
        } else if (statType.equals("queue")) {
            values = getSwitchStatistics(switchId, OFStatisticsType.QUEUE);
        } else if (statType.equals("flow")) {
            values = getSwitchStatistics(switchId, OFStatisticsType.FLOW);
        } else if (statType.equals("aggregate")) {
            values = getSwitchStatistics(switchId, OFStatisticsType.AGGREGATE);
        } else if (statType.equals("desc")) {
            values = getSwitchStatistics(switchId, OFStatisticsType.DESC);
        } else if (statType.equals("table")) {
            values = getSwitchStatistics(switchId, OFStatisticsType.TABLE);
        } else if (statType.equals("features")) {
            IOFSwitch sw = beaconProvider.getSwitches().get(HexString.toLong(switchId));
            OFFeaturesReply fr = sw.getFeaturesReply();
            model.put(BeaconJsonView.ROOT_OBJECT_KEY, fr);
            return view;
        } else if (statType.equals("hosts")) {
            IOFSwitch sw = beaconProvider.getSwitches().get(HexString.toLong(switchId));
            model.put(BeaconJsonView.ROOT_OBJECT_KEY, sw.getMacVlanToPortMap());
            return view;
        }
        model.put(BeaconJsonView.ROOT_OBJECT_KEY, values);
        return view;
    }
    
    @RequestMapping("/controller/switches/json")
    public View getSwitchesJson(Map<String,Object> model) {
        BeaconJsonView view = new BeaconJsonView();
        List<Map<String,Long>> switchIds = new ArrayList<Map<String,Long>>();
        for (IOFSwitch s:beaconProvider.getSwitches().values()) {
            Map<String, Long> m = new HashMap<String, Long>();
            m.put("dpid", s.getId());
            switchIds.add(m);
           
        }
       model.put(BeaconJsonView.ROOT_OBJECT_KEY, switchIds);
       return view;
    }

    /**
     * @param packageAdmin the packageAdmin to set
     */
    @Autowired
    public void setPackageAdmin(PackageAdmin packageAdmin) {
        this.packageAdmin = packageAdmin;
    }
    
    @RequestMapping("/controller/bundle/json")
    public View getOsgiJson(Map<String,Object>model) {
        BeaconJsonView view = new BeaconJsonView();
        List<Map<String, String>> bundleNames = new ArrayList<Map<String, String>>();
        for (int i = 0; i < this.bundleContext.getBundles().length; i++) {
            Bundle b = this.bundleContext.getBundles()[i];
            Map<String, String> m = new HashMap<String, String>();
            m.put("name", b.getSymbolicName());
            m.put("id", new Long(b.getBundleId()).toString());
            m.put("state", new Integer(b.getState()).toString());
            bundleNames.add(m);
        }
        model.put(BeaconJsonView.ROOT_OBJECT_KEY, bundleNames);
        return view;
    }
}
