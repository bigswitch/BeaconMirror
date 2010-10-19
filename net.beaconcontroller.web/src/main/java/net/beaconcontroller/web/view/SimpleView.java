/**
 * 
 */
package net.beaconcontroller.web.view;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.beaconcontroller.web.view.layout.Layout;

import org.springframework.web.servlet.View;

/**
 * @author David Erickson (derickso@cs.stanford.edu)
 *
 */
public class SimpleView implements View {

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        for (Object o : model.values()) {
            if (o instanceof Layout) {
                ((Layout)o).render(request, response);
            }
        }
    }
}
