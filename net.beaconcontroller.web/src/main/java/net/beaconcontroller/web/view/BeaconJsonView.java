package net.beaconcontroller.web.view;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

/**
 * Extends {@link MappingJacksonJsonView} and adds the capability to set the
 * root object of the serialization to be something other than the default
 * Map. If an object exists in the model with key {@link ROOT_OBJECT_KEY} then
 * it is set as the root of the Json serialization, otherwise the Map is used.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class BeaconJsonView extends MappingJacksonJsonView {
    public static String ROOT_OBJECT_KEY = BeaconJsonView.class.getName() + "_ROOT";

    /**
     * 
     */
    public BeaconJsonView() {
        super();
    }

    @Override
    protected Object filterModel(Map<String, Object> model) {
        Map<String, Object> map = (Map<String, Object>) super.filterModel(model);
        if (map.containsKey(ROOT_OBJECT_KEY))
            return map.get(ROOT_OBJECT_KEY);
        else
            return map;
    }
    
    /**
     * Work around for objectMapper as a private variable
     */
    protected ObjectMapper objectMapper;
    
    @Override
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        super.setObjectMapper(objectMapper);
    }
    
    /**
     * Over-ridden in order to slip in JSONP padding if there is a "callback" parameter in the request
     * 
     * NOTE - is there some better way to do this so subclasses can use this?  A bit awkward given that
     * the set-up is to write to request's output stream directly and the state of that stream (i.e. the stack
     * of stuff getting written out) needs to be tightly managed.
     */
    @Override
    protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Object value = filterModel(model);
        boolean prefixJson = request.getParameter("callback") != null;
        
        if(this.objectMapper == null)
            this.objectMapper = new ObjectMapper();
        
        JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(response.getOutputStream(), JsonEncoding.UTF8);
        
        String callback = request.getParameter("callback");
        
        if (prefixJson)
            generator.writeRaw(callback + "(");
        
        objectMapper.writeValue(generator, value);
        generator.flush();
    
        if (prefixJson) {
            generator.writeRaw(");");
            generator.flush();
        }
    }
}
