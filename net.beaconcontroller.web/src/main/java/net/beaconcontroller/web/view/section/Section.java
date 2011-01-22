package net.beaconcontroller.web.view.section;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.beaconcontroller.web.view.Renderable;

public abstract class Section implements Renderable {
    protected String title = "(no title)";
    protected String templateClass = "";
    protected Map<String, String> templateParams = new HashMap<String, String>();
    protected String body = "";
    protected boolean escapeXml = true;
    protected String sectionJspPath = "/WEB-INF/jsp/view/section/section.jsp";
    protected List<String> jsIncludes = new LinkedList<String>();
    protected List<String> imports = new LinkedList<String>();
    protected String id = "Section-" + Math.random();
    protected Map<String, Object> model = new HashMap<String, Object>();
    
    /**
     * @return the title of the section
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * If this section is a recognized javascript (decorator) template, this is set here.
     * 
     * @param templateClass
     */
    public void setTemplateClass(String templateClass) {
        this.templateClass = templateClass;
    }
    
    /**
     * These params are used to pass variables from the java layer here to the javascript decorator
     * set by the setTemplateClass.
     * 
     * Hint - think of this as parameter passing to javascript decorators.
     * 
     * @param key
     * @param value
     */
    public void putJSParam(String key, String value) {
        this.templateParams.put(key, value);
    }
    
    public void putJSParams(Map<String, String> params) {
        this.templateParams.putAll(params);
    }
    
    /**
     * Sets the body of this section (static html approach)
     * 
     * @param body
     */
    public void setBody(String body) {
        this.body = body;
        this.escapeXml = true;
    }
    
    public void setBody(String body, boolean escapeXml) {
        this.body = body;
        this.escapeXml = escapeXml;
    }
    
    /**
     * If this section uses jsp imports, objects can be set using this 'model'
     * that will be available to the jsp at render time.  (Think of these
     * as objects that will be set as request attributes before rendering.)
     * 
     * (Current is a bit dangerous, as this can over-write elements of the framework
     * being stored in the same request attribute mechanism.)
     * 
     * Hint - think of this as parameter passing to jsps.
     * 
     * @return A reference to the underlying model
     */
    public Map<String, Object> getModel() {
        return this.model;
    }
    
    /**
     * Shortcut method to adding a single param in to the model.  Returns
     * an object (or null) that was previously mapped to this key.
     * 
     * @param key
     * @param value
     * @return
     */
    public Object putModelParam(String key, Object value) {
        return this.model.put(key, value);
    }
    
    /**
     * Pulls in javascript files required by this section at this path
     * @param jsFilePath
     */
    public void addJSInclude(String jsFilePath) {
        this.jsIncludes.add(jsFilePath);
    }
    
    /**
     * Imports content from another relative or absolute URL (importPath)
     * and puts it in to the section (after the body content).
     * @param importPath
     */
    public void addImport(String importPath) {
        this.imports.add(importPath);
    }
    
    public void render(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.setAttribute("title", title);
        request.setAttribute("id", id);
        request.setAttribute("templateClass", templateClass);
        request.setAttribute("templateParams", templateParams);
        request.setAttribute("body", body);
        request.setAttribute("escapeXml", escapeXml);
        request.setAttribute("jsIncludes", jsIncludes);
        request.setAttribute("imports", imports);
        
        for (Entry<String,Object> entry : model.entrySet()) {
            //if(request.getAttribute(entry.getKey()) != null)
            //    throw new Exception("Model used for rendering treads on an attribute already used: " + entry.getKey());
            
            request.setAttribute(entry.getKey(), entry.getValue());
        }
        
        request.getRequestDispatcher(sectionJspPath).include(request, response);
    }
}
