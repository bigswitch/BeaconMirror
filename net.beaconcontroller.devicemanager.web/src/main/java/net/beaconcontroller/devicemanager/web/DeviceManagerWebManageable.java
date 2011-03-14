package net.beaconcontroller.devicemanager.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.packet.IPv4;
import net.beaconcontroller.web.IWebManageable;
import net.beaconcontroller.web.view.BeaconViewResolver;
import net.beaconcontroller.web.view.Tab;
import net.beaconcontroller.web.view.layout.Layout;
import net.beaconcontroller.web.view.layout.TwoColumnLayout;
import net.beaconcontroller.web.view.section.TableSection;
import net.beaconcontroller.topology.SwitchPortTuple;

import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * This class sets up the web UI component for the structures in
 * net.beaconcontroller.devicemanager
 * 
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
@Controller
@RequestMapping("/devicemanager")
public class DeviceManagerWebManageable implements IWebManageable {
    protected static Logger log = LoggerFactory.getLogger(DeviceManagerWebManageable.class);
    protected List<Tab> tabs;
    protected IDeviceManager deviceManager;

    public DeviceManagerWebManageable() {
        tabs = new ArrayList<Tab>();
        tabs.add(new Tab("Overview", "/wm/devicemanager/overview.do"));
    }

    @Override
    public String getName() {
        return "Device Manager";
    }

    @Override
    public String getDescription() {
        return "View devices.";
    }

    @Override
    public List<Tab> getTabs() {
        return tabs;
    }

    @RequestMapping("/overview")
    public String overview(Map<String, Object> model) {
        Layout layout = new TwoColumnLayout();
        model.put("layout", layout);

        // Listener List Table
        List<String> columnNames = new ArrayList<String>();
        List<List<String>> cells = new ArrayList<List<String>>();
        columnNames = new ArrayList<String>();
        columnNames.add("MAC");
        columnNames.add("IP");
        columnNames.add("Switch");
        columnNames.add("Port");
        
        cells = new ArrayList<List<String>>();
        for (Device device : deviceManager.getDevices()) {
            Queue<SwitchPortTuple> swp_tuple = device.getSwPorts();
            if (swp_tuple != null && swp_tuple.size() > 0) {
                for (SwitchPortTuple swp : swp_tuple) {
                    List<String> row = new ArrayList<String>();
                    row.add(HexString.toHexString(device.getDataLayerAddress()));
                    StringBuffer sb = new StringBuffer();
                    for (Integer nw : device.getNetworkAddresses()) {
                        if (sb.length() > 0)
                            sb.append(" ");
                        sb.append(IPv4.fromIPv4Address(nw) + " ");
                    }
                    row.add(sb.toString());
                    row.add(HexString.toHexString(swp.getSw().getId()));
                    row.add(swp.getPort().toString());
                    cells.add(row);
                }
            }
            else {
                List<String> row = new ArrayList<String>();
                row.add(HexString.toHexString(device.getDataLayerAddress()));
                StringBuffer sb = new StringBuffer();
                for (Integer nw : device.getNetworkAddresses()) {
                    if (sb.length() > 0)
                        sb.append(" ");
                    sb.append(IPv4.fromIPv4Address(nw) + " ");
                }
                row.add(sb.toString());
                row.add("");
                row.add("");
                cells.add(row);
            }
        }
        
        layout.addSection(new TableSection("Devices", columnNames, cells), TwoColumnLayout.COLUMN1);
        return BeaconViewResolver.SIMPLE_VIEW;
    }

    /**
     * @param deviceManager the deviceManager to set
     */
    @Autowired
    public void setDeviceManager(IDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }
}
