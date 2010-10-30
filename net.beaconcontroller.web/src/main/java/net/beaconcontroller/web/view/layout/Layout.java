package net.beaconcontroller.web.view.layout;

import java.util.LinkedHashMap;
import java.util.Map;

import net.beaconcontroller.web.view.Renderable;
import net.beaconcontroller.web.view.section.Section;


/**
 * 
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public abstract class Layout implements Renderable {
    protected static String RESOURCE_PATH = "/WEB-INF/jsp/view/layout/";

    protected Map<Section, Object> sections = new LinkedHashMap<Section, Object>();

    /**
     * @return the sections
     */
    public Map<Section, Object> getSections() {
        return sections;
    }

    /**
     * @param sections the sections to set
     */
    public void setSections(Map<Section, Object> sections) {
        this.sections = sections;
    }

    /**
     * 
     * @param section
     * @param hint
     */
    public void addSection(Section section, Object hint) {
        this.sections.put(section, hint);
    }
}
