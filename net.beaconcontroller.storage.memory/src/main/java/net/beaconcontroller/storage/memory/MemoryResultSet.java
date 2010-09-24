package net.beaconcontroller.storage.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.beaconcontroller.storage.IResultSet;
import net.beaconcontroller.storage.NullValueStorageException;
import net.beaconcontroller.storage.ResultSetIterator;
import net.beaconcontroller.storage.RowOrdering;
import net.beaconcontroller.storage.StorageException;
import net.beaconcontroller.storage.TypeMismatchStorageException;

public class MemoryResultSet implements IResultSet {

    class RowUpdate {
        Object key;
        String column;
        Object value;
        
        RowUpdate(Object key, String column, Object value) {
            this.key = key;
            this.column = column;
            this.value = value;
        }
    }
    
    MemoryTable table;
    String primaryKeyName;
    List<Map<String,Object>> rowList;
    int currentIndex;
    List<RowUpdate> rowUpdateList;
    List<Object> rowDeleteList;
    Iterator<IResultSet> resultSetIterator;
    
    MemoryResultSet(MemoryTable table, String primaryKeyName) {
        this.table = table;
        this.primaryKeyName = primaryKeyName;
        rowList = new ArrayList<Map<String,Object>>();
        currentIndex = -1;
    }
    
    void addRow(Map<String,Object> row) {
        rowList.add(row);
    }
    
    class RowComparator implements Comparator<Map<String,Object>> {
        private RowOrdering rowOrdering;
        
        public RowComparator(RowOrdering rowOrdering) {
            this.rowOrdering = rowOrdering;
        }
        
        public int compare(Map<String,Object> row1, Map<String,Object> row2) {
            if (rowOrdering == null)
                return 0;
            
            for (RowOrdering.Item item: rowOrdering.getItemList()) {
                Comparable key1 = (Comparable)row1.get(item.getColumn());
                Comparable key2 = (Comparable)row2.get(item.getColumn());
                int result = key1.compareTo(key2);
                if (result != 0) {
                    if (item.getDirection() == RowOrdering.Direction.DESCENDING)
                        result = -result;
                    return result;
                }
            }
            
            return 0;
        }
        
        public boolean equals(Object obj) {
            if (!(obj instanceof RowComparator))
                return false;
            RowComparator rc = (RowComparator)obj;
            if (rc.rowOrdering == null)
                return this.rowOrdering == null;
            return rc.rowOrdering.equals(this.rowOrdering);
        }
    }
    
    void sortRows(RowOrdering rowOrdering) {
        Collections.sort(rowList, new RowComparator(rowOrdering));
    }
    
    @Override
    public Map<String,Object> getRow() {
        if ((currentIndex < 0) || (currentIndex >= rowList.size())) {
            throw new StorageException("No current row in result set.");
        }
        
        return rowList.get(currentIndex);
    }
    
    @Override
    public void close() {
        // Don't need to do anything here for this implementation
    }

    @Override
    public boolean next() {
        currentIndex++;
        return currentIndex < rowList.size();
    }

    @Override
    public void save() {
        synchronized (table) {
            if (rowUpdateList != null) {
                for (RowUpdate rowUpdate: rowUpdateList) {
                    Map<String,Object> row = table.getRow(rowUpdate.key);
                    row.put(rowUpdate.column, rowUpdate.value);
                }
                rowUpdateList = null;
            }
            
            if (rowDeleteList != null) {
                for (Object key: rowDeleteList) {
                    table.deleteRow(key);
                }
                rowDeleteList = null;
            }
        }
    }

    Object getObject(String column) {
        Map<String,Object> row = rowList.get(currentIndex);
        if (!row.containsKey(column))
            throw new StorageException("Column \"" + column + "\" does not exist in table \"" + table.getTableName() + "\"");
        Object value = row.get(column);
        return value;
    }
    
    @Override
    public boolean containsColumn(String columnName) {
        return getObject(columnName) != null;
    }

    @Override
    public boolean getBoolean(String columnName) {
        Boolean b = getBooleanObject(columnName);
        if (b == null)
            throw new NullValueStorageException(columnName);
        return b.booleanValue();
    }

    @Override
    public byte getByte(String columnName) {
        Byte b = getByteObject(columnName);
        if (b == null)
            throw new NullValueStorageException(columnName);
        return b.byteValue();
    }

    @Override
    public byte[] getByteArray(String column) {
        byte[] b = null;
        Object obj = getObject(column);
        if (obj != null) {
            if (!(obj instanceof byte[]))
                throw new StorageException("Invalid byte array value");
            b = (byte[])obj;
        }
        return b;
    }

    @Override
    public double getDouble(String columnName) {
        Double d = getDoubleObject(columnName);
        if (d == null)
            throw new NullValueStorageException(columnName);
        return d.doubleValue();
    }

    @Override
    public float getFloat(String columnName) {
        Float f = getFloatObject(columnName);
        if (f == null)
            throw new NullValueStorageException(columnName);
        return f.floatValue();
    }

    @Override
    public int getInt(String columnName) {
        Integer i = getIntegerObject(columnName);
        if (i == null)
            throw new NullValueStorageException(columnName);
        return i.intValue();
    }

    @Override
    public long getLong(String columnName) {
        Long l = getLongObject(columnName);
        if (l == null)
            throw new NullValueStorageException(columnName);
        return l.longValue();
    }

    @Override
    public short getShort(String columnName) {
        Short s = getShortObject(columnName);
        return s.shortValue();
    }

    @Override
    public String getString(String column) {
        Object obj = getObject(column);
        if (obj == null)
            return null;
        return obj.toString();
    }


    @Override
    public Date getDate(String column) {
        Date d;
        Object obj = getObject(column);
        if (obj == null) {
            d = null;
        } else if (obj instanceof Date) {
            d = (Date) obj;
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                d = dateFormat.parse(obj.toString());
            }
            catch (ParseException exc) {
                throw new TypeMismatchStorageException(Date.class.getName(), obj.getClass().getName(), column);
            }
        }
        return d;
    }


    @Override
    public Short getShortObject(String columnName)
    {
        Short s;
        Object obj = getObject(columnName);
        if (obj instanceof Short) {
            s = (Short)obj;
        } else if (obj != null) {
            try {
                s = Short.parseShort(obj.toString());
            }
            catch (NumberFormatException exc) {
                throw new TypeMismatchStorageException(Short.class.getName(), obj.getClass().getName(), columnName);
            }
        } else {
            s = null;
        }
        return s;
    }
    
    @Override
    public Integer getIntegerObject(String columnName)
    {
        Integer i;
        Object obj = getObject(columnName);
        if (obj instanceof Integer) {
            i = (Integer)obj;
        } else if (obj != null) {
            try {
                i = Integer.parseInt(obj.toString());
            }
            catch (NumberFormatException exc) {
                throw new TypeMismatchStorageException(Integer.class.getName(), obj.getClass().getName(), columnName);
            }
        } else {
            i = null;
        }
        return i;
    }

    @Override
    public Long getLongObject(String columnName)
    {
        Long l;
        Object obj = getObject(columnName);
        if (obj instanceof Long) {
            l = (Long)obj;
        } else if (obj != null) {
            try {
                l = Long.parseLong(obj.toString());
            }
            catch (NumberFormatException exc) {
                throw new TypeMismatchStorageException(Long.class.getName(), obj.getClass().getName(), columnName);
            }
        } else {
            l = null;
        }
        return l;
    }

    @Override
    public Float getFloatObject(String columnName)
    {
        Float f;
        Object obj = getObject(columnName);
        if (obj instanceof Float) {
            f = (Float)obj;
        } else if (obj != null) {
            try {
                f = Float.parseFloat(obj.toString());
            }
            catch (NumberFormatException exc) {
                throw new TypeMismatchStorageException(Float.class.getName(), obj.getClass().getName(), columnName);
            }
        } else {
            f = null;
        }
        return f;
    }

    @Override
    public Double getDoubleObject(String columnName)
    {
        Double d;
        Object obj = getObject(columnName);
        if (obj instanceof Double) {
            d = (Double)obj;
        } else if (obj != null) {
            try {
                d = Double.parseDouble(obj.toString());
            }
            catch (NumberFormatException exc) {
                throw new TypeMismatchStorageException(Double.class.getName(), obj.getClass().getName(), columnName);
            }
        } else {
            d = null;
        }
        return d;
    }

    @Override
    public Boolean getBooleanObject(String columnName)
    {
        Boolean b;
        Object obj = getObject(columnName);
        if (obj instanceof Boolean) {
            b = (Boolean)obj;
        } else if (obj != null) {
            try {
                b = Boolean.parseBoolean(obj.toString());
            }
            catch (NumberFormatException exc) {
                throw new TypeMismatchStorageException(Boolean.class.getName(), obj.getClass().getName(), columnName);
            }
        } else {
            b = null;
        }
        return b;
    }

    @Override
    public Byte getByteObject(String columnName)
    {
        Byte b;
        Object obj = getObject(columnName);
        if (obj instanceof Byte) {
            b = (Byte)obj;
        } else if (obj != null) {
            try {
                b = Byte.parseByte(obj.toString());
            }
            catch (NumberFormatException exc) {
                throw new TypeMismatchStorageException(Byte.class.getName(), obj.getClass().getName(), columnName);
            }
        } else {
            b = null;
        }
        return b;
    }

    
    @Override
    public boolean isNull(String columnName)
    {
        Object obj = getObject(columnName);
        return (obj == null);
    }

    private void addRowUpdate(String column, Object value) {
        Object key = rowList.get(currentIndex).get(primaryKeyName);
        RowUpdate rowUpdate = new RowUpdate(key, column, value);
        if (rowUpdateList == null)
            rowUpdateList = new ArrayList<RowUpdate>();
        rowUpdateList.add(rowUpdate);
    }
    
    @Override
    public void setBoolean(String column, boolean value) {
        addRowUpdate(column, value);
    }

    @Override
    public void setByte(String column, byte value) {
        addRowUpdate(column, value);
    }

    @Override
    public void setByteArray(String column, byte[] byteArray) {
        addRowUpdate(column, byteArray);
    }

    @Override
    public void setDouble(String column, double value) {
        addRowUpdate(column, value);
    }

    @Override
    public void setFloat(String column, float value) {
        addRowUpdate(column, value);
    }

    @Override
    public void setInt(String column, int value) {
        addRowUpdate(column, value);
    }

    @Override
    public void setLong(String column, long value) {
        addRowUpdate(column, value);
    }

    @Override
    public void setShort(String column, short value) {
        addRowUpdate(column, value);
    }

    @Override
    public void setString(String column, String value) {
        addRowUpdate(column, value);
    }

    @Override
    public void setDate(String column, Date value) {
        addRowUpdate(column, value);
    }

    @Override
    public void setShortObject(String column, Short value)
    {
        addRowUpdate(column, value);
    }
    
    @Override
    public void setIntegerObject(String column, Integer value)
    {
        addRowUpdate(column, value);
    }

    @Override
    public void setLongObject(String column, Long value)
    {
        addRowUpdate(column, value);
    }

    @Override
    public void setFloatObject(String column, Float value)
    {
        addRowUpdate(column, value);
    }

    @Override
    public void setDoubleObject(String column, Double value)
    {
        addRowUpdate(column, value);
    }

    @Override
    public void setBooleanObject(String column, Boolean value)
    {
        addRowUpdate(column, value);
    }

    @Override
    public void setByteObject(String column, Byte value)
    {
        addRowUpdate(column, value);
    }

    
    public void setNull(String column)
    {
        addRowUpdate(column, null);
    }


    @Override
    public void deleteRow() {
        Object key = rowList.get(currentIndex).get(primaryKeyName);
        if (rowDeleteList == null)
            rowDeleteList = new ArrayList<Object>();
        rowDeleteList.add(key);
    }
    
    @Override
    public Iterator<IResultSet> iterator() {
        if (resultSetIterator == null)
            resultSetIterator = new ResultSetIterator(this);
        return resultSetIterator;
    }
}
