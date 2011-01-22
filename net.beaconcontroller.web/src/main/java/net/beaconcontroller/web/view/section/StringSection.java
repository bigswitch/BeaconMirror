package net.beaconcontroller.web.view.section;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author David Erickson (derickso@stanford.edu)
 */
public class StringSection extends Section {
    protected String content;

    public StringSection(String title, String body) {
        this.title = title;
        this.body = body;
    }
}
