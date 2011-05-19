package net.beaconcontroller.web.view.section;

import java.util.HashMap;
import java.util.Map;

/**
 * This class takes a jsp file and embeds its content within a Section wrapper.
 * 
 * @author David Erickson (derickso@stanford.edu)
 * @author Kyle Forster (kyle.forster@bigswitch.com)
 */
public class JspSection extends Section {
    protected static final String RESOURCE_PATH = "/WEB-INF/jsp/view/section/";

    /**
     * @param jspFileName
     * @param model
     */
    public JspSection(String jspFileName, Map<String, Object> model) {
        if(model == null)
            this.model = new HashMap<String, Object>();
        else
            this.model = model;
        this.addImport(RESOURCE_PATH + jspFileName);
    }
}
