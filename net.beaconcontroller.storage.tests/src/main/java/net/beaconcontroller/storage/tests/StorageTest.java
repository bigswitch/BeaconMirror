package net.beaconcontroller.storage.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.beaconcontroller.storage.CompoundPredicate;
import net.beaconcontroller.storage.IPredicate;
import net.beaconcontroller.storage.IQuery;
import net.beaconcontroller.storage.IResultSet;
import net.beaconcontroller.storage.IRowMapper;
import net.beaconcontroller.storage.IStorageSource;
import net.beaconcontroller.storage.NullValueStorageException;
import net.beaconcontroller.storage.OperatorPredicate;
import net.beaconcontroller.storage.RowOrdering;
import net.beaconcontroller.test.BeaconTestCase;

import org.junit.Test;

public class StorageTest extends BeaconTestCase {
    
    protected IStorageSource storageSource;
    
    protected String PERSON_TABLE_NAME = "Person";
    
    protected String PERSON_SSN = "SSN";
    protected String PERSON_FIRST_NAME = "FirstName";
    protected String PERSON_LAST_NAME = "LastName";
    protected String PERSON_AGE = "Age";
    protected String PERSON_REGISTERED = "Registered";
    
    protected String[] PERSON_COLUMN_LIST = {PERSON_SSN, PERSON_FIRST_NAME, PERSON_LAST_NAME, PERSON_AGE, PERSON_REGISTERED};
    
    class Person {
        private String ssn;
        private String firstName;
        private String lastName;
        int age;
        boolean registered;
        
        public Person(String ssn, String firstName, String lastName, int age, boolean registered) {
            this.ssn = ssn;
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
            this.registered = registered;
        }
        
        public String getSSN() {
            return ssn;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public String getLastName() {
            return lastName;
            
        }
        
        public int getAge() {
            return age;
        }
        
        public boolean isRegistered() {
            return registered;
        }
    }
    
    Object[][] PERSON_INIT_DATA = {
            {"111-11-1111", "John", "Smith", 40, true},
            {"222-22-2222", "Jim", "White", 24, false},
            {"333-33-3333", "Lisa", "Jones", 27, true},
            {"444-44-4444", "Susan", "Jones", 14, false},
            {"555-55-5555", "Jose", "Garcia", 31, true},
            {"666-66-6666", "Abigail", "Johnson", 35, false},
            {"777-77-7777", "Bjorn", "Borg", 55, true},
            {"888-88-8888", "John", "McEnroe", 53, false}
    };

    public void initPersons() {
        for (Object[] row: PERSON_INIT_DATA) {
            Map<String,Object> rowValues = new HashMap<String,Object>();
            for (int i = 0; i < PERSON_COLUMN_LIST.length; i++) {
                rowValues.put(PERSON_COLUMN_LIST[i], row[i]);
            }
            storageSource.insertRow(PERSON_TABLE_NAME, rowValues);
        }
    }
    
    public void setUp() throws Exception {
        super.setUp();
        storageSource.setTablePrimaryKeyName(PERSON_TABLE_NAME, PERSON_SSN);
        initPersons();
    }

    public void checkExpectedResults(IResultSet resultSet, String[] columnNameList, Object[][] expectedRowList) {
        boolean nextResult;
        for (Object[] expectedRow: expectedRowList) {
            nextResult = resultSet.next();
            assertEquals(nextResult,true);
            assertEquals(expectedRow.length, columnNameList.length);
            for (int i = 0; i < expectedRow.length; i++) {
                Object expectedObject = expectedRow[i];
                String columnName = columnNameList[i];
                if (expectedObject instanceof Boolean)
                    assertEquals(((Boolean)expectedObject).booleanValue(), resultSet.getBoolean(columnName));
                else if (expectedObject instanceof Byte)
                    assertEquals(((Byte)expectedObject).byteValue(), resultSet.getByte(columnName));
                else if (expectedObject instanceof Short)
                    assertEquals(((Short)expectedObject).shortValue(), resultSet.getShort(columnName));
                else if (expectedObject instanceof Integer)
                    assertEquals(((Integer)expectedObject).intValue(), resultSet.getInt(columnName));
                else if (expectedObject instanceof Long)
                    assertEquals(((Long)expectedObject).longValue(), resultSet.getLong(columnName));
                else if (expectedObject instanceof Float)
                    assertEquals(((Float)expectedObject).floatValue(), resultSet.getFloat(columnName), 0.00001);
                else if (expectedObject instanceof Double)
                    assertEquals(((Double)expectedObject).doubleValue(), resultSet.getDouble(columnName), 0.00001);
                else if (expectedObject instanceof byte[])
                    assertEquals((byte[])expectedObject, resultSet.getByteArray(columnName));
                else if (expectedObject instanceof String)
                    assertEquals((String)expectedObject, resultSet.getString(columnName));
                else
                    assertTrue("Unexpected column value type", false);
            }
        }
        nextResult = resultSet.next();
        assertEquals(nextResult,false);
        resultSet.close();
    }
    
    @Test
    public void testInsertRows() {
        IResultSet resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, null, null, new RowOrdering(PERSON_SSN));
        checkExpectedResults(resultSet, PERSON_COLUMN_LIST, PERSON_INIT_DATA);
    }
    
    @Test
    public void testOperatorQuery() {
        Object[][] expectedResults = {
                {"John", "Smith", 40},
                {"Jim", "White", 24},
        };
        String[] columnList = {PERSON_FIRST_NAME,PERSON_LAST_NAME,PERSON_AGE};
        IResultSet resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, columnList,
                new OperatorPredicate(PERSON_LAST_NAME, OperatorPredicate.Operator.GTE, "Sm"),
                new RowOrdering(PERSON_SSN));
        checkExpectedResults(resultSet, columnList, expectedResults);
    }
    
    @Test
    public void testAndQuery() {
        String[] columnList = {PERSON_FIRST_NAME,PERSON_LAST_NAME};        
        Object[][] expectedResults = {
                {"Lisa", "Jones"},
                {"Susan", "Jones"},
                {"Jose", "Garcia"},
                {"Abigail", "Johnson"},
                {"John", "McEnroe"}
        };
        IResultSet resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, columnList,
                new CompoundPredicate(CompoundPredicate.Operator.AND, false,
                        new OperatorPredicate(PERSON_LAST_NAME, OperatorPredicate.Operator.GTE, "G"),
                        new OperatorPredicate(PERSON_LAST_NAME, OperatorPredicate.Operator.LT, "N")
                ),
                new RowOrdering(PERSON_SSN));
        checkExpectedResults(resultSet, columnList, expectedResults);
    }
    
    @Test
    public void testOrQuery() {
        String[] columnList = {PERSON_FIRST_NAME,PERSON_LAST_NAME, PERSON_AGE};        
        Object[][] expectedResults = {
                {"John", "Smith", 40},
                {"Lisa", "Jones", 27},
                {"Abigail", "Johnson", 35},
                {"Bjorn", "Borg", 55},
                {"John", "McEnroe", 53}
        };
        IResultSet resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, columnList,
                new CompoundPredicate(CompoundPredicate.Operator.OR, false,
                        new OperatorPredicate(PERSON_AGE, OperatorPredicate.Operator.GTE, 35),
                        new OperatorPredicate(PERSON_FIRST_NAME, OperatorPredicate.Operator.EQ, "Lisa")
                ),
                new RowOrdering(PERSON_SSN));
        checkExpectedResults(resultSet, columnList, expectedResults);
}
    
    @Test
    public void testCreateQuery() {
        String[] columnList = {PERSON_FIRST_NAME,PERSON_LAST_NAME};
        Object[][] expectedResults = {
                {"Lisa", "Jones"},
                {"Susan", "Jones"}
        };
        IPredicate predicate = new OperatorPredicate(PERSON_LAST_NAME, OperatorPredicate.Operator.EQ, "Jones");
        IQuery query = storageSource.createQuery(PERSON_TABLE_NAME, columnList, predicate, new RowOrdering(PERSON_SSN));
        IResultSet resultSet = storageSource.executeQuery(query);
        checkExpectedResults(resultSet, columnList, expectedResults);
    }
    
    @Test
    public void testQueryParameters() {
        String[] columnList = {PERSON_FIRST_NAME,PERSON_LAST_NAME, PERSON_AGE};        
        Object[][] expectedResults = {
                {"John", "Smith", 40},
                {"Bjorn", "Borg", 55},
                {"John", "McEnroe", 53}
        };
        IPredicate predicate = new OperatorPredicate(PERSON_AGE, OperatorPredicate.Operator.GTE, "?MinimumAge?");
        IQuery query = storageSource.createQuery(PERSON_TABLE_NAME, columnList, predicate, new RowOrdering(PERSON_SSN));
        query.setParameter("MinimumAge", 40);
        IResultSet resultSet = storageSource.executeQuery(query);
        checkExpectedResults(resultSet, columnList, expectedResults);
    }
    
    private void checkPerson(Person person, Object[] expectedValues) {
        assertEquals(person.getSSN(), expectedValues[0]);
        assertEquals(person.getFirstName(), expectedValues[1]);
        assertEquals(person.getLastName(), expectedValues[2]);
        assertEquals(person.getAge(), expectedValues[3]);
        assertEquals(person.isRegistered(), expectedValues[4]);
    }
    
    @Test
    public void testRowMapper() {
        Object[][] expectedResults = {
                PERSON_INIT_DATA[2],
                PERSON_INIT_DATA[3]
        };
        IPredicate predicate = new OperatorPredicate(PERSON_LAST_NAME, OperatorPredicate.Operator.EQ, "Jones");
        IRowMapper rowMapper = new IRowMapper() {
            public Object mapRow(IResultSet resultSet) {
                String ssn = resultSet.getString(PERSON_SSN);
                String firstName = resultSet.getString(PERSON_FIRST_NAME);
                String lastName = resultSet.getString(PERSON_LAST_NAME);
                int age = resultSet.getInt(PERSON_AGE);
                boolean registered = resultSet.getBoolean(PERSON_REGISTERED);
                return new Person(ssn, firstName, lastName, age, registered);
            }
        };
        Object[] personList = storageSource.executeQuery(PERSON_TABLE_NAME, null, predicate, new RowOrdering(PERSON_SSN), rowMapper);
        assertEquals(personList.length, 2);
        for (int i = 0; i < personList.length; i++)
            checkPerson((Person)personList[i], expectedResults[i]);
    }
    
    @Test
    public void testDeleteRowsDirect() {
        
        storageSource.deleteRow(PERSON_TABLE_NAME, "111-11-1111");
        storageSource.deleteRow(PERSON_TABLE_NAME, "222-22-2222");
        storageSource.deleteRow(PERSON_TABLE_NAME, "333-33-3333");
        storageSource.deleteRow(PERSON_TABLE_NAME, "444-44-4444");
        
        Object[][] expectedResults = {
                {"555-55-5555", "Jose", "Garcia", 31, true},
                {"666-66-6666", "Abigail", "Johnson", 35, false},
                {"777-77-7777", "Bjorn", "Borg", 55, true},
                {"888-88-8888", "John", "McEnroe", 53, false}
        };
        IResultSet resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, PERSON_COLUMN_LIST, null, new RowOrdering(PERSON_SSN));
        checkExpectedResults(resultSet, PERSON_COLUMN_LIST, expectedResults);
    }
    
    @Test
    public void testDeleteRowsFromResultSet() {
        Object[][] expectedResults = {
                {"555-55-5555", "Jose", "Garcia", 31, true},
                {"666-66-6666", "Abigail", "Johnson", 35, false},
                {"777-77-7777", "Bjorn", "Borg", 55, true},
                {"888-88-8888", "John", "McEnroe", 53, false}
        };
        
        // Query once to delete the rows
        IResultSet resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, PERSON_COLUMN_LIST, null, new RowOrdering(PERSON_SSN));
        for (int i = 0; i < 4; i++) {
            resultSet.next();
            resultSet.deleteRow();
        }
        resultSet.save();
        resultSet.close();
        
        // Now query again to verify that the rows were deleted
        resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, PERSON_COLUMN_LIST, null, new RowOrdering(PERSON_SSN));
        checkExpectedResults(resultSet, PERSON_COLUMN_LIST, expectedResults);
    }
    
    @Test
    public void testUpdateRowsDirect() {
        
        Object[][] expectedResults = {
                {"777-77-7777", "Tennis", "Borg", 60, true},
                {"888-88-8888", "Tennis", "McEnroe", 60, false}
        };
        Map<String,Object> updateValues = new HashMap<String,Object>();
        updateValues.put(PERSON_FIRST_NAME, "Tennis");
        updateValues.put(PERSON_AGE, 60);
        
        IPredicate predicate = new OperatorPredicate(PERSON_AGE, OperatorPredicate.Operator.GT, 50);
        IResultSet resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, null, predicate, new RowOrdering(PERSON_SSN));
        while (resultSet.next()) {
            String key = resultSet.getString(PERSON_SSN);
            storageSource.updateRow(PERSON_TABLE_NAME, key, updateValues);
        }
        resultSet.close();
        
        resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, PERSON_COLUMN_LIST, predicate, new RowOrdering(PERSON_SSN));
        checkExpectedResults(resultSet, PERSON_COLUMN_LIST, expectedResults);
    }
    
    @Test
    public void testUpdateRowsFromResultSet() {
        
        Object[][] expectedResults = {
                {"777-77-7777", "Tennis", "Borg", 60, true},
                {"888-88-8888", "Tennis", "McEnroe", 60, false}
        };
        
        IPredicate predicate = new OperatorPredicate(PERSON_AGE, OperatorPredicate.Operator.GT, 50);
        IResultSet resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, null, predicate, null);
        while (resultSet.next()) {
            resultSet.setString(PERSON_FIRST_NAME, "Tennis");
            resultSet.setInt(PERSON_AGE, 60);
        }
        resultSet.save();
        resultSet.close();
        
        resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, PERSON_COLUMN_LIST, predicate, new RowOrdering(PERSON_SSN));
        checkExpectedResults(resultSet, PERSON_COLUMN_LIST, expectedResults);
    }
    
    @Test
    public void testNullValues() {
        
        IPredicate predicate = new OperatorPredicate(PERSON_LAST_NAME, OperatorPredicate.Operator.EQ, "Jones");
        IResultSet resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, null, predicate, new RowOrdering(PERSON_SSN));
        while (resultSet.next()) {
            resultSet.setNull(PERSON_FIRST_NAME);
            resultSet.setIntegerObject(PERSON_AGE, null);
        }
        resultSet.save();
        resultSet.close();

        resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, null, predicate, new RowOrdering(PERSON_SSN));
        int count = 0;
        while (resultSet.next()) {
            boolean checkNull = resultSet.isNull(PERSON_FIRST_NAME);
            assertTrue(checkNull);
            String s = resultSet.getString(PERSON_FIRST_NAME);
            assertEquals(s, null);
            checkNull = resultSet.isNull(PERSON_AGE);
            assertTrue(checkNull);
            Integer intObj = resultSet.getIntegerObject(PERSON_AGE);
            assertEquals(intObj, null);
            Short shortObj = resultSet.getShortObject(PERSON_AGE);
            assertEquals(shortObj, null);
            boolean excThrown = false;
            try {
                resultSet.getInt(PERSON_AGE);
            }
            catch (NullValueStorageException exc) {
                excThrown = true;
            }
            assertTrue(excThrown);
            count++;
        }
        resultSet.close();
        assertEquals(count, 2);
        
        predicate = new OperatorPredicate(PERSON_FIRST_NAME, OperatorPredicate.Operator.EQ, null);
        resultSet = storageSource.executeQuery(PERSON_TABLE_NAME, null, predicate, new RowOrdering(PERSON_SSN));
        count = 0;
        while (resultSet.next()) {
            boolean checkNull = resultSet.isNull(PERSON_FIRST_NAME);
            assertTrue(checkNull);
            count++;
            checkNull = resultSet.isNull(PERSON_AGE);
            assertTrue(checkNull);
        }
        resultSet.close();
        assertEquals(count, 2);
    }
}
