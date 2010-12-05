package net.beaconcontroller.web.view.section;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    protected Map<String, Map<String, Object>> columnMetadata;

    public TableSection(String title, List<String> columnNames,
            List<List<String>> cells) {
        this.title = title;
        this.columnNames = columnNames;
        this.cells = cells;
        this.jspFileName = "table.jsp";
        this.columnMetadata = new HashMap<String, Map<String, Object>>();
        for(String s : columnNames) {
          this.columnMetadata.put(s, new HashMap<String, Object>());
          this.setDefaultColumnAttributes(s);
        }
        
    }
    
    public void setDefaultColumnAttributes(String columnName) {
      setColumnAttribute(columnName, "escapeXML", true);
      //more go here
    }
    
    public void dontEscapeXML(String columnName) {
      this.setColumnAttribute(columnName, "escapeXML", false);
    }
    
    public void setColumnAttribute(String columnName, String attributeName, Object attributeValue) {
      columnMetadata.get(columnName).put(attributeName, attributeValue);
    }

    @Override
    public void render(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        request.setAttribute("columnNames", columnNames);
        request.setAttribute("columnMetadata", columnMetadata);

        request.setAttribute("cells", cells);
        request.setAttribute("title", title);
        super.render(request, response);
    }
}
