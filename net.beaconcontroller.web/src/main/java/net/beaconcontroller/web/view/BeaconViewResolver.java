/**
 * 
 */
package net.beaconcontroller.web.view;

import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * @author David Erickson (derickso@cs.stanford.edu)
 *
 */
public class BeaconViewResolver implements Ordered, ViewResolver {
    public static String SIMPLE_JSON_VIEW = "simpleJson";
    public static String SIMPLE_VIEW = "simple";

    protected int order = Ordered.LOWEST_PRECEDENCE;


    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        if ("simpleJson".equalsIgnoreCase(viewName))
            return new SimpleJsonView();
        if ("simple".equalsIgnoreCase(viewName))
            return new SimpleView();
        return null;
    }

    @Override
    public int getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }
}
