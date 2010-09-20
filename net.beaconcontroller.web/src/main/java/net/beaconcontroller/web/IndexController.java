/**
 * 
 */
package net.beaconcontroller.web;

import net.beaconcontroller.core.IBeaconProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author David Erickson (derickso@cs.stanford.edu)
 *
 */
@Controller
public class IndexController {
    protected IBeaconProvider beaconProvider;

    /**
     * @param beaconProvider the beaconProvider to set
     */
    @Autowired
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

    @RequestMapping("/index")
    public ModelAndView get() {
        ModelAndView mav = new ModelAndView();
        mav.addObject("switches", beaconProvider.getSwitches().values());
        mav.addObject("listeners", beaconProvider.getListeners());
        return mav;
    }
}
