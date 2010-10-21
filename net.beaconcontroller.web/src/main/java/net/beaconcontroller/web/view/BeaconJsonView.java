package net.beaconcontroller.web.view;

import java.util.Map;

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
}
