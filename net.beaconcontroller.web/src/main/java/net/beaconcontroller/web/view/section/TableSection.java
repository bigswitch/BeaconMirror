package net.beaconcontroller.web.view.section;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class TableSection extends JspSection {
    protected List<String> columnNames;
    protected List<List<String>> cells;

    public TableSection(String title, List<String> columnNames,
            List<List<String>> cells) {
        this.title = title;
        this.columnNames = columnNames;
        this.cells = cells;
        this.jspFileName = "table.jsp";
    }

    @Override
    public void render(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
    	//Make sure to put in a default row if empty, otherwise the front end template may break
    	List<List<String>> c = cells;
    	if(c.size() == 0) {
    		c = new ArrayList<List<String>>();
    		List<String> r = new ArrayList<String>();
    		for(@SuppressWarnings("unused") String col : columnNames) {
    			r.add("...");
    		}
    		c.add(r);
    	}
        request.setAttribute("columnNames", columnNames);
        request.setAttribute("cells", c);
        request.setAttribute("title", title);
        super.render(request, response);
    }
}
