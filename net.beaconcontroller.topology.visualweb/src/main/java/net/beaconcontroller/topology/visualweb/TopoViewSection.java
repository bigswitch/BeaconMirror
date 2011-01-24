package net.beaconcontroller.topology.visualweb;

import net.beaconcontroller.web.view.section.Section;

public class TopoViewSection extends Section {
    
    public TopoViewSection() {
        this.title = "Topology View";
        this.body = "<div id='topologyCanvas' style='width: 100%; height: 400px'></div>"; //todo - replace this with setTemplate
        this.escapeXml = false;
        this.addJSInclude("/js/jit-yc.js");
        this.addJSInclude("/js/bsnTopologyViewer.js");

    }
}
