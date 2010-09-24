package net.beaconcontroller.storage.memory;

import java.util.Map;
import java.util.HashMap;

import net.beaconcontroller.storage.IQuery;
import net.beaconcontroller.storage.IPredicate;
import net.beaconcontroller.storage.RowOrdering;

public class MemoryQuery implements IQuery {

    private String tableName;
    private String[] columnNameList;
    private IPredicate predicate;
    private RowOrdering rowOrdering;
    private Map<String,Comparable<?>> parameterMap;
    
    MemoryQuery(String className, String[] columnNameList, IPredicate predicate, RowOrdering rowOrdering) {
        this.tableName = className;
        this.columnNameList = columnNameList;
        this.predicate = predicate;
        this.rowOrdering = rowOrdering;
    }
    
    @Override
    public void setParameter(String name, Object value) {
        if (parameterMap == null)
            parameterMap = new HashMap<String,Comparable<?>>();
        parameterMap.put(name, (Comparable<?>)value);
    }

    String getTableName() {
        return tableName;
    }
    
    String[] getColumnNameList() {
        return columnNameList;
    }
    
    IPredicate getPredicate() {
        return predicate;
    }
    
    RowOrdering getRowOrdering() {
        return rowOrdering;
    }
    
    Comparable<?> getParameter(String name) {
        Comparable<?> value = null;
        if (parameterMap != null) {
            value = parameterMap.get(name);
        }
        return value;
    }
    
    Map<String,Comparable<?>> getParameterMap() {
        return parameterMap;
    }
}
