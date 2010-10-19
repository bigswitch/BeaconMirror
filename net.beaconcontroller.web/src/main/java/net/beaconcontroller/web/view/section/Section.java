package net.beaconcontroller.web.view.section;

import net.beaconcontroller.web.view.Renderable;

public abstract class Section implements Renderable {
    protected String title;

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
}
