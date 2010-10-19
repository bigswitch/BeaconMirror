package net.beaconcontroller.web;

import java.util.List;

import net.beaconcontroller.web.view.Tab;

/**
 * Interface to be implemented by bundles wishing to appear in the web UI.
 * 
 * @author Kyle Forster (kyle.forster@bigswitch.com)
 * @author David Erickson (daviderickson@cs.stanford.edu)
 * 
 */
public interface IWebManageable {

    /**
     * The name assigned to this web manageable (usually the same method as the
     * getName from IOFMessageListener).
     * 
     * @return
     */
    public String getName();

    /**
     * A short description (~100 chars) of the package for UI purposes
     * 
     * @return
     */
    public String getDescription();

    /**
     * Return tabs that will be rendered by the UI
     * 
     * @return
     */
    public List<Tab> getTabs();
}
