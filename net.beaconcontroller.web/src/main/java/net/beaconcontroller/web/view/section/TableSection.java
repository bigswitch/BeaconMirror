package net.beaconcontroller.web.view.section;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A java wrapper around a useful table class
 * 
 * @author David Erickson (daviderickson@cs.stanford.edu)
 * @author Kyle Forster (kyle.forster@bigswitch.com)
 */
public class TableSection extends JspSection {
    protected List<String> columnNames;
    protected List<List<String>> cells;
    protected Map<String, Map<String, Object>> columnMetadata;

    /**
     * Create a simple table.  In this implementation, cells and columNames are carried
     * through by reference, so the contents of the table can change after this constructor
     * has been called.
     * 
     * @param title
     * @param columnNames
     * @param cells
     */
    public TableSection(String title, List<String> columnNames,
            List<List<String>> cells) {
        super("table.jsp", null);
        
        this.title = title;
        this.columnMetadata = new HashMap<String, Map<String, Object>>();
        
        for (String s : columnNames) {
            this.columnMetadata.put(s, new HashMap<String, Object>());
            this.setDefaultColumnAttributes(s);
        }
        
        // Make sure to put in a default row if empty, otherwise the front end
        // template may break
        List<List<String>> c = cells;
        if (c.size() == 0) {
            c = new ArrayList<List<String>>();
            List<String> r = new ArrayList<String>();
            for (int i = 0; i < columnNames.size(); i++) {
                r.add("...");
            }
            c.add(r);
        }
        this.putModelParam("columnNames", columnNames);
        this.putModelParam("columnMetadata", columnMetadata);
        this.putModelParam("cells", c);
        
    }
    

    public void setDefaultColumnAttributes(String columnName) {
        setColumnAttribute(columnName, "escapeXML", true);
        // more go here
    }

    public void dontEscapeXML(String columnName) {
        this.setColumnAttribute(columnName, "escapeXML", false);
    }

    public void setColumnAttribute(String columnName, String attributeName,
            Object attributeValue) {
        columnMetadata.get(columnName).put(attributeName, attributeValue);
    }
    
}
