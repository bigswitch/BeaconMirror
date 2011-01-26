package net.beaconcontroller.storage.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.beaconcontroller.storage.IPredicate;
import net.beaconcontroller.storage.IQuery;
import net.beaconcontroller.storage.IResultSet;
import net.beaconcontroller.storage.IRowMapper;
import net.beaconcontroller.storage.RowOrdering;
import net.beaconcontroller.storage.IStorageSource;
import net.beaconcontroller.storage.OperatorPredicate;
import net.beaconcontroller.storage.CompoundPredicate;
import net.beaconcontroller.storage.StorageException;

public class MemoryStorageSource implements IStorageSource {
    
    interface IMemoryPredicate {
        public boolean matches(Map<String,Object> row);
    }
    
    class MemoryOperatorPredicate implements IMemoryPredicate {
        
        private String columnName;
        private OperatorPredicate.Operator operator;
        private Comparable<?> value;
        
        MemoryOperatorPredicate(String columnName, OperatorPredicate.Operator operator, Comparable<?> value) {
            this.columnName = columnName;
            this.operator = operator;
            this.value = value;
        }
        
        @SuppressWarnings("unchecked")
        public boolean matches(Map<String,Object> row) {
            // With Eclipse Galileo the SuppressWarnings annotation suppressed the
            // warning from casting to Comparable, but this doesn't work with
            // Eclipse Helios anymore. Need to see if there's some alternative way
            // to suppress it. Note that we need to cast to the non-generic version
            // of Comparable in order to call compareTo (or at least I couldn't
            // figure out a way to do it with generics). I'm sure it would be
            // possible to do it with a lot of type checking/casting with instanceof
            // but that would add a lot of code.
            Comparable value = (Comparable) row.get(columnName);
            if (value == null)
                return (this.value == null);
            else if (this.value == null)
                return false;
            
            int result = value.compareTo(this.value);
            switch (operator) {
            case EQ:
                return result == 0;
            case LT:
                return result < 0;
            case LTE:
                return result <= 0;
            case GT:
                return result > 0;
            case GTE:
                return result >= 0;
            }
            return false;
        }
    }
    
    class MemoryCompoundPredicate implements IMemoryPredicate {
        
        private CompoundPredicate.Operator operator;
        private boolean negated;
        private IMemoryPredicate[] predicateList;
        
        MemoryCompoundPredicate(CompoundPredicate.Operator operator, boolean negated, IMemoryPredicate... predicateList) {
            this.operator = operator;
            this.negated = negated;
            this.predicateList = predicateList;
        }
        
        public boolean matches(Map<String,Object> row) {
            boolean matches;
            if (operator == CompoundPredicate.Operator.AND) {
                matches = true;
                for (IMemoryPredicate predicate: predicateList) {
                    if (!predicate.matches(row)) {
                        matches = false;
                    }
                }
            } else {
                matches = false;
                for (IMemoryPredicate predicate: predicateList) {
                    if (predicate.matches(row)) {
                        matches = true;
                    }
                }
            }
            
            if (negated)
                matches = !matches;
            
            return matches;
        }
    }
    
    private Map<String, MemoryTable> tableMap = new HashMap<String,MemoryTable>();
    private Map<String, String> primaryKeyMap = new HashMap<String,String>();
    
    private final static String DEFAULT_PRIMARY_KEY_NAME = "id";
    
    public MemoryStorageSource() {
    }
    
    public void setTablePrimaryKeyName(String tableName, String primaryKeyName) {
        primaryKeyMap.put(tableName, primaryKeyName);
    }
    
    private String getTablePrimaryKeyName(String tableName) {
        String primaryKeyName = primaryKeyMap.get(tableName);
        if (primaryKeyName == null)
            primaryKeyName = DEFAULT_PRIMARY_KEY_NAME;
        return primaryKeyName;
    }
    
    @Override
    synchronized public void createTable(String tableName) {
        if (!tableMap.containsKey(tableName))
            tableMap.put(tableName, new MemoryTable(tableName));
    }
    
    synchronized private MemoryTable getTable(String tableName, boolean create) {
        MemoryTable table = tableMap.get(tableName);
        if (table == null) {
            if (!create)
                throw new StorageException("Table does not exist");
            table = new MemoryTable(tableName);
            tableMap.put(tableName, table);
        }
        return table;
    }
    
    IMemoryPredicate convertPredicate(IPredicate predicate, Map<String,Comparable<?>> parameterMap) {
        // This is probably not the cleanest way to implement this (you could probably do some
        // sort of Visitor pattern to eliminate the type-casting), but this should be fine for now.
        IMemoryPredicate memoryPredicate = null;
        if (predicate != null) {
            if (predicate instanceof CompoundPredicate) {
                CompoundPredicate compoundPredicate = (CompoundPredicate)predicate;
                int predicateListLength = compoundPredicate.getPredicateList().length;
                IMemoryPredicate[] memoryPredicateList = new IMemoryPredicate[predicateListLength];
                for (int i = 0; i < predicateListLength; i++) {
                    memoryPredicateList[i] = convertPredicate(compoundPredicate.getPredicateList()[i], parameterMap);
                }
                memoryPredicate = new MemoryCompoundPredicate(compoundPredicate.getOperator(),
                        compoundPredicate.isNegated(), memoryPredicateList);
            } else if (predicate instanceof OperatorPredicate) {
                OperatorPredicate operatorPredicate = (OperatorPredicate)predicate;
                Comparable<?> predicateValue = operatorPredicate.getValue();
                if (predicateValue instanceof String) {
                    String stringValue = (String) predicateValue;
                    if ((stringValue.charAt(0) == '?') && (stringValue.charAt(stringValue.length()-1) == '?')) {
                        String parameterName = stringValue.substring(1,stringValue.length()-1);
                        predicateValue = parameterMap.get(parameterName);
                    }
                }
                memoryPredicate = new MemoryOperatorPredicate(operatorPredicate.getColumnName(),
                        operatorPredicate.getOperator(), predicateValue);
            } else {
                throw new StorageException("Unknown predicate type");
            }
        }
        return memoryPredicate;
    }
    
    @Override
    public IQuery createQuery(String tableName, String[] columnNameList,
            IPredicate predicate, RowOrdering rowOrdering) {
        return new MemoryQuery(tableName, columnNameList, predicate, rowOrdering);
    }

    private MemoryResultSet executeParameterizedQuery(String tableName, String[] columnNameList,
            IPredicate predicate, RowOrdering rowOrdering, Map<String,Comparable<?>> parameterMap) {
        MemoryTable table = getTable(tableName, false);
        String primaryKeyName = getTablePrimaryKeyName(tableName);
        MemoryResultSet resultSet = new MemoryResultSet(table, primaryKeyName);
        synchronized (table) {
            if (table != null) {
                Collection<Map<String,Object>> allRows = table.getAllRows();
                IMemoryPredicate memoryPredicate = convertPredicate(predicate, parameterMap);
                for (Map<String,Object> row: allRows) {
                    if ((memoryPredicate == null) || memoryPredicate.matches(row)) {
                        Map<String,Object> rowResult;
                        if (columnNameList != null) {
                            rowResult = new HashMap<String,Object>();
                            boolean insertedPrimaryKey = false;
                            for (String columnName: columnNameList) {
                                if (columnName.equals(primaryKeyName))
                                    insertedPrimaryKey = true;
                                Object columnValue = row.get(columnName);
                                rowResult.put(columnName, columnValue);
                            }
                            if (!insertedPrimaryKey) {
                                Object primaryKey = row.get(primaryKeyName);
                                rowResult.put(primaryKeyName, primaryKey);
                            }
                        } else {
                            rowResult = row;
                        }
                        resultSet.addRow(rowResult);
                    }
                }
                resultSet.sortRows(rowOrdering);
            }
        }
        return resultSet;
    }
    
    public IResultSet executeQuery(IQuery query) {
        MemoryQuery memoryQuery = (MemoryQuery) query;
        return executeParameterizedQuery(memoryQuery.getTableName(),
                memoryQuery.getColumnNameList(), memoryQuery.getPredicate(),
                memoryQuery.getRowOrdering(), memoryQuery.getParameterMap());
    }
    
    @Override
    public IResultSet executeQuery(String tableName, String[] columnNameList,
            IPredicate predicate, RowOrdering rowOrdering) {
        return executeParameterizedQuery(tableName, columnNameList, predicate, rowOrdering, null);
    }

    @Override
    public Object[] executeQuery(String tableName, String[] columnNameList,
            IPredicate predicate, RowOrdering rowOrdering, IRowMapper rowMapper) {
        List<Object> objectList = new ArrayList<Object>();
        IResultSet resultSet = executeParameterizedQuery(tableName, columnNameList,
                predicate, rowOrdering, null);
        while (resultSet.next()) {
            Object object = rowMapper.mapRow(resultSet);
            objectList.add(object);
        }
        resultSet.close();
        return objectList.toArray();
    }

    @Override
    public void insertRow(String tableName, Map<String, Object> values) {
        MemoryTable table = getTable(tableName, true);
        String primaryKeyName = getTablePrimaryKeyName(tableName);
        synchronized (table) {
            Object primaryKey = values.get(primaryKeyName);
            if (primaryKey == null) {
                if (primaryKeyName.equals(DEFAULT_PRIMARY_KEY_NAME)) {
                    values = new HashMap<String,Object>(values);
                    primaryKey = table.getNextId();
                    values.put(primaryKeyName, primaryKey);
                }
            }
            table.insertRow(primaryKey, values);
        }
    }

    @Override
    public void updateRows(String tableName, IPredicate predicate, Map<String, Object> values) {
        String[] columnList = {};
        String primaryKeyName = primaryKeyMap.get(tableName);
        MemoryResultSet resultSet = (MemoryResultSet) executeQuery(tableName, columnList, predicate, null);
        while (resultSet.next()) {
            Object key = resultSet.getObject(primaryKeyName);
            updateRow(tableName, key, values);
        }
    }

    @Override
    public void updateRow(String tableName, Object key, Map<String, Object> values) {
        MemoryTable table = getTable(tableName, false);
        synchronized (table) {
            Map<String,Object> row = table.getRow(key);
            if (row == null)
                row = table.newRow(key);
            for (Map.Entry<String,Object> entry: values.entrySet()) {
                row.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void updateRow(String tableName, Map<String, Object> values) {
        String primaryKeyName = primaryKeyMap.get(tableName);
        updateRow(tableName, (String) values.get(primaryKeyName), values);
    }

    @Override
    public void deleteRow(String tableName, Object rowKey) {
        MemoryTable table = tableMap.get(tableName);
        synchronized (table) {
            table.deleteRow(rowKey);
        }
    }

    @Override
    public IResultSet getRow(String tableName, Object rowKey) {
        MemoryTable table = getTable(tableName, false);
        String primaryKeyName = getTablePrimaryKeyName(tableName);
        MemoryResultSet resultSet = new MemoryResultSet(table, primaryKeyName);
        synchronized (table) {
            Map<String, Object> row = table.getRow(rowKey);
            if (row != null) {
                resultSet.addRow(new HashMap<String, Object>(row));
            }
        }
        return resultSet;
    }
}
