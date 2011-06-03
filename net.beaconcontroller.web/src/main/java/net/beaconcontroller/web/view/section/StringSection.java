package net.beaconcontroller.web.view.section;

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
