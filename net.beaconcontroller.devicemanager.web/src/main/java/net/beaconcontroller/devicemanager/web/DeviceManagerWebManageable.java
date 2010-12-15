package net.beaconcontroller.devicemanager.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.web.IWebManageable;
import net.beaconcontroller.web.view.BeaconViewResolver;
import net.beaconcontroller.web.view.Tab;
import net.beaconcontroller.web.view.layout.Layout;
import net.beaconcontroller.web.view.layout.TwoColumnLayout;
import net.beaconcontroller.web.view.section.TableSection;

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
        columnNames.add("Switch");
        columnNames.add("Port");
        cells = new ArrayList<List<String>>();
        for (Device device : deviceManager.getDevices()) {
            List<String> row = new ArrayList<String>();
            row.add(HexString.toHexString(device.getDataLayerAddress()));
            row.add(HexString.toHexString(device.getSw().getId()));
            row.add(device.getSwPort().toString());
            cells.add(row);
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
