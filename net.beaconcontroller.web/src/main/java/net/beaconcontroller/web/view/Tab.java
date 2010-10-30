package net.beaconcontroller.web.view;

/**
 *
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 * @author Kyle Forster (kyle.forster@bigswitch.com)
 */
public class Tab {
    protected String title;
    protected String url;

    /**
     * 
     */
    public Tab() {
    }

    /**
     * @param title
     * @param url
     */
    public Tab(String title, String url) {
        this.title = title;
        this.url = url;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
