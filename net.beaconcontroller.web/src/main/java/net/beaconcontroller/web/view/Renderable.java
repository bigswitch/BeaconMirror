/**
 * 
 */
package net.beaconcontroller.web.view;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author David Erickson (derickso@cs.stanford.edu)
 *
 */
public interface Renderable {

    /**
     * Renders content out a HttpServletResponse
     * @param request
     * @param response
     * @throws IOException
     */
    public void render(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
