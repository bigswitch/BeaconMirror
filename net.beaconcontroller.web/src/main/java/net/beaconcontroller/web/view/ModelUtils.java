package net.beaconcontroller.web.view;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Collection of utility methods used to create useful models, primarily
 * intended for use with BeaconJsonView.
 * 
 * @author kyle
 * 
 */
public class ModelUtils {

    /**
     * Format for Ext JS Grid Library - see
     * http://dev.sencha.com/deploy/dev/docs/
     */
    public static final String EXT_JS_FORMAT = "EXTJS";

    /**
     * Format for raw data (i.e. a JSON object that just looks like an array of
     * rows
     */
    public static final String RAW_FORMAT = "RAW";
    
    /**
     * Format used by becaon charts
     * Originally from jit charts (see http://thejit.org/static/v20/Jit/Examples/AreaChart/example1.code.html)
     */
    public static final String BEACON_CHART = "BEACONCHART";

    /**
     * Entry point to decorate the model object passed in with a json-ready
     * representation of the rows given.
     * 
     * @param model
     * @param format
     * @param rows
     * @param idProperty
     */
    public static void generateTableModel(Map<String, Object> model,
            String format, List<Map<String, Object>> rows, String idProperty) {
        Object m = null;
        
        if (format == null)
            format = RAW_FORMAT;

        //do something sane if rows is null
        if (rows == null)
            m = new LinkedList<Map<String, Object>>();
        
        if (format.equalsIgnoreCase(EXT_JS_FORMAT))
            m = generateGridExtModel(rows, idProperty);
        else if (format.equalsIgnoreCase(RAW_FORMAT))
            m = rows;
        else
            throw new IllegalArgumentException("Format " + format
                    + " is not a recognized format for table models");

        model.put(BeaconJsonView.ROOT_OBJECT_KEY, m);
    }

    /**
     * Generates a table-ready model from a list of beans, assuming that each bean
     * corresponds to a row in the table.
     * 
     * The columns are taken from propertyNames.
     * 
     * @param model
     * @param format
     * @param beans
     * @param propertyNames
     * @param idProperty
     */
    public static void generateTableModelFromBeans(Map<String, Object> model,
            String format, List<?> beans, List<String> propertyNames,
            String idProperty) {
        List<Map<String, Object>> rows = beansToRows(beans, propertyNames);
        generateTableModel(model, format, rows, idProperty);
    }

    /**
     * Generates a table-ready model from a list of beans, assuming that each bean
     * corresponds to a row in the table.
     * 
     * The columns are generated from the getters of the first bean in the list.
     * 
     * @param model
     * @param format
     * @param beans
     * @param idProperty
     */
    public static void generateTableModelFromBeans(Map<String, Object> model,
            String format, List<?> beans, String idProperty) {
        List<Map<String, Object>> rows = beansToRows(beans);
        generateTableModel(model, format, rows, idProperty);
    }

    /**
     * Generates a model that corresponds to the json format expected by the EXT
     * JS Grid plug-in (http://dev.sencha.com/deploy/dev/docs/) from a List of
     * rows, each represented by a map of column name to cell value.
     * 
     * @param rows
     * @param idProperty
     *            The propertyName that should be used on the front end grid as
     *            the primary key, defaults to "id"
     * @return
     */
    public static Map<String, Object> generateGridExtModel(
            List<Map<String, Object>> rows, String idProperty) {
        if (idProperty == null)
            idProperty = "id";

        Map<String, Object> ret = new HashMap<String, Object>();
        
        if(rows.size() == 0) {
            ret.put("rowCount", "" + 0);
            ret.put("rows", rows);
            return ret;
        }

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("idProperty", idProperty);
        metadata.put("root", "rows");
        metadata.put("totalProperty", "rowCount");
        metadata.put("successProperty", "success");

        // fields
        Map<String, ?> firstRow = rows.get(0);
        List<Map<String, String>> fields = new LinkedList<Map<String, String>>();
        for (Object columnName : firstRow.keySet()) {
            Map<String, String> fieldMetadata = new HashMap<String, String>();
            fieldMetadata.put("name", columnName.toString());
            fields.add(fieldMetadata);
        }
        metadata.put("fields", fields);

        // sortinfo
        Map<String, String> sortInfo = new HashMap<String, String>();
        String firstColumnName = firstRow.keySet().iterator().next().toString();
        sortInfo.put("field", firstColumnName);
        sortInfo.put("direction", "ASC");
        metadata.put("sortInfo", sortInfo);

        ret.put("metaData", metadata);
        ret.put("success", "true");
        ret.put("rowCount", "" + rows.size());
        ret.put("rows", rows);

        return ret;
    }
    
    /**
     * Generates a model that corresponds to the json format expected by the BeaconChart
     * javascript class.  Most commonly the series argument has only one entry, but a stacked
     * area or stacked bar chart is possible if it has multiple entries.  (Use an ordered map
     * if there is a specific ordering requested.  The first will be on the bottom.)
     * The length of labels and the length of the int[] in series should be the same.
     * 
     * 
     * var json = {  
      'values': [
        {  
          'label': 'date A',  
          'values': [10, 40, 15, 7]  
        }
      ]
    };
     * 
     */
    public static void generateBeaconChartModel(Map<String, Object> model, List<?> labels, String seriesName, long[] series) {
        //Map<String, Object> ret = new HashMap<String, Object>();
        List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        int i = 0;
        for(Object label : labels) {
            Map<String, Object> point = new HashMap<String, Object>();
            point.put("label", label.toString());
            point.put("values", Arrays.asList(series[i]));
            values.add(point);
            i++;
        }
        model.put("label", Arrays.asList(seriesName));
        model.put("values", values);
        
    }


    /**
     * Converts a list of objects to the rows structure used elsewhere by
     * looking at the propertyies in the first bean to create the columns to be
     * used in the grid. (Note - this uses reflection to find methods named
     * "get" + PropertyName (note the change from camel case to title case is
     * done in this method with no args and maps each of those to a column).
     * 
     * @param beans
     * @return
     */
    protected static List<Map<String, Object>> beansToRows(List<?> beans) {
        Object b = beans.get(0);
        Method[] methods = b.getClass().getMethods();
        List<String> propertyNames = new LinkedList<String>(); // actual
                                                               // properties are
                                                               // guaranteed to
                                                               // be shorter

        for (Method m : methods) {
            String s = m.getName();
            if (s.startsWith("get") && m.getParameterTypes().length == 0) {
                String f0 = s.substring("get".length(), "get".length() + 1)
                        .toLowerCase();
                String f1 = s.substring("get".length() + 1);
                String propertyName = f0 + f1;
                if (propertyName.equals("class")) // don't add the getClass
                                                  // method
                    continue;

                propertyNames.add(propertyName);
            }
        }
        return beansToRows(beans, propertyNames);
    }

    /**
     * Converts a list of objects to the rows structure used elsewhere by
     * looking for properties specified in propertyNames in each bean in List
     * beans to create the rows to be used in the grid. (Note - this uses
     * reflection to find methods named "get" + PropertyName (note the change
     * from camel case to title case is done in this method)).
     * 
     * @param beans
     * @param propertyNames
     * @return
     */
    protected static List<Map<String, Object>> beansToRows(List<?> beans,
            List<String> propertyNames) {
        List<Map<String, Object>> rows = new LinkedList<Map<String, Object>>();
        for (Object b : beans) {
            Map<String, Object> row = new HashMap<String, Object>();
            for (String propertyName : propertyNames) {
                String methodName = "get"
                        + propertyName.substring(0, 1).toUpperCase()
                        + propertyName.substring(1);
                try {
                    Method m = b.getClass().getMethod(methodName, new Class[0]);
                    Object propertyValue = m.invoke(b, null);
                    row.put(propertyName, propertyValue);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }

            }
            rows.add(row);
        }
        return rows;
    }

}
