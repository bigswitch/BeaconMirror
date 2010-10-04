/**
 * 
 */
package net.beaconcontroller.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.beaconcontroller.core.IBeaconProvider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author David Erickson (derickso@cs.stanford.edu)
 *
 */
@Controller
public class BundleController implements BundleContextAware {
    protected static Logger log = LoggerFactory.getLogger(BundleController.class);
    protected IBeaconProvider beaconProvider;
    protected BundleContext bundleContext;

    /**
     * @param beaconProvider the beaconProvider to set
     */
    @Autowired
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

    @RequestMapping("/bundles")
    public ModelAndView get() {
        ModelAndView mav = new ModelAndView();
        List<Bundle> bundles = Arrays.asList(this.bundleContext.getBundles());
        Collections.sort(bundles, new Comparator<Bundle>() {
            public int compare(Bundle o1, Bundle o2) {
                return o1.getSymbolicName().compareTo(o2.getSymbolicName());
            }
        });
        mav.addObject("bundles", bundles);
        return mav;
    }

    @RequestMapping("/bundle/{bundleId}")
    public String performAction(@PathVariable Long bundleId, @RequestParam String action) {
        Bundle bundle = this.bundleContext.getBundle(bundleId);
        if (action != null) {
            try {
                if ("start".equalsIgnoreCase(action)) {
                        bundle.start();
                } else if ("stop".equalsIgnoreCase(action)) {
                        bundle.stop();
                }
            } catch (BundleException e) {
                log.error("Failure performing action " + action + " on bundle " + bundle.getSymbolicName(), e);
            }
        }
        return "redirect:/bundles.do";
    }

    @Override
    public void setBundleContext(BundleContext context) {
        this.bundleContext = context;
    }
}
