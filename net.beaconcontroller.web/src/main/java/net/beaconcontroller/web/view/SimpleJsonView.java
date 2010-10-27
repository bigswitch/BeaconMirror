package net.beaconcontroller.web.view;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class SimpleJsonView implements View {

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        response.setContentType("application/json");
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache, no-store, max-age=0");
        response.addDateHeader("Expires", 1L);
        // TODO this is bogus right now, have to find a better Json solution
        response.getWriter().write((String)model.get("output"));
    }
}
