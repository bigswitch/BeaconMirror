package net.beaconcontroller.web.view.section;

import java.util.Map;

/**
 * 
 * 
 * @author kyle
 */
public class ChartSection extends Section {
    Map<String, String> chartParams;

    public ChartSection(String title, String pathToJson, Map<String, String> chartParams) {
        super.setTitle(title);
        super.setTemplateClass("beaconAreaChart");
        
        if(chartParams != null)
            super.putJSParams(chartParams);
        
        super.putJSParam("pathToJson", pathToJson);
        super.putJSParam("autoReload", "5000");
        super.addJSInclude("/js/jit.js");
        super.addJSInclude("/js/excanvas.js");
        super.addJSInclude("/js/beaconChart.js");
        
    }
    
}
