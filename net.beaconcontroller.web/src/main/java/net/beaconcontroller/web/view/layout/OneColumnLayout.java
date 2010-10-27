package net.beaconcontroller.web.view.layout;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.beaconcontroller.web.view.section.Section;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OneColumnLayout extends Layout {

    @Override
    public void render(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        for (Section s : sections.keySet())
            s.render(request, response);
    }
}
