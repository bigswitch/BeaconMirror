package net.beaconcontroller.storage;

/** Predicate class to specify rows by equality or comparison operations
 * of column values. The Storage API uses the special column name of "id"
 * to specify the primary key values for the row.
 * 
 * @author rob
 */
public class OperatorPredicate implements IPredicate {
    
    public enum Operator { EQ, LT, LTE, GT, GTE };
    
    private String columnName;
    private Operator operator;
    private Comparable<?> value;
    
    public OperatorPredicate(String columnName, Operator operator, Comparable<?> value) {
        this.columnName = columnName;
        this.operator = operator;
        this.value = value;
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    public Operator getOperator() {
        return operator;
    }
    
    public Comparable<?> getValue() {
        return value;
    }
}
