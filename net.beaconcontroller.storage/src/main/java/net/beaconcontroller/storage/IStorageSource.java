package net.beaconcontroller.storage;

import java.util.Map;

public interface IStorageSource {

    /** Set the column to be used as the primary key for a table. This should
     * be guaranteed to be unique for all of the rows in the table, although the
     * storage API does not necessarily enforce this requirement. If no primary
     * key name is specified for a table then the storage API assumes there is
     * a column named "id" that is used as the primary key. In this case when
     * a new row is inserted using the storage API and no id is specified
     * explictly in the row data, the storage API automatically generates a
     * unique ID (typically a UUID) for the id column. To work across all
     * possible implementations of the storage API it is safest, though, to
     * specify the primary key column explicitly.
     * FIXME: It's sort of a kludge to have to specify the primary key column
     * here. Ideally there would be some sort of metadata -- perhaps stored
     * directly in the table, at least in the NoSQL case -- that the
     * storage API could query to obtain the primary key info.
     * @param tableName The name of the table for which we're setting the key
     * @param primaryKeyName The name of column to be used as the primary key
     */
    public void setTablePrimaryKeyName(String tableName, String primaryKeyName);

    /** Create a new table if one does not already exist with the given name.
     * 
     * @param tableName The name of the table to create.
     */
    void createTable(String tableName);
    
    /** Create a query object representing the given query parameters. The query
     * object can be passed to executeQuery to actually perform the query and obtain
     * a result set.
     * 
     * @param tableName The name of the table to query.
     * @param columnNames The list of columns to return in the result set.
     * @param predicate The predicate that specifies which rows to return in the result set.
     * @param ordering Specification of order that rows are returned from the result set
     * returned from executing the query. If the ordering is null, then rows are returned
     * in an implementation-specific order.
     * @return Query object to be passed to executeQuery.
     */
    IQuery createQuery(String tableName, String[] columnNames, IPredicate predicate, RowOrdering ordering);
    
    /** Execute a query created with createQuery.
     * 
     * @param query The query to execute
     * @return The result set containing the rows/columns specified in the query.
     */
    IResultSet executeQuery(IQuery query);

    /** Execute a query created with the given query parameters.
     *
     * @param tableName The name of the table to query.
     * @param columnNames The list of columns to return in the result set.
     * @param predicate The predicate that specifies which rows to return in the result set.
     * @param ordering Specification of order that rows are returned from the result set
     * returned from executing the query. If the ordering is null, then rows are returned
     * in an implementation-specific order.
     * @return The result set containing the rows/columns specified in the query.
     */
    IResultSet executeQuery(String tableName, String[] columnNames, IPredicate predicate,
            RowOrdering ordering);
    
    /** Execute a query and call the row mapper to map the results to Java objects.
     * 
     * @param tableName The name of the table to query.
     * @param columnNames The list of columns to return in the result set.
     * @param predicate The predicate that specifies which rows to return in the result set.
     * @param ordering Specification of order that rows are returned from the result set
     * returned from executing the query. If the ordering is null, then rows are returned
     * in an implementation-specific order.
     * @param rowMapper The client-supplied object that maps the data in a row in the result
     * set to a client object.
     * @return The result set containing the rows/columns specified in the query.
     */
    Object[] executeQuery(String tableName, String[] columnNames, IPredicate predicate,
            RowOrdering ordering, IRowMapper rowMapper);
    
    /** Insert a new row in the table with the given column data.
     * The primary key for the row in the table is indicated with the special column
     * name of "id". If there's no "id" values specified in the map of values, then
     * a unique id will be automatically assigned to the row.
     * @param tableName The name of the table to which to add the row
     * @param values The map of column names/values to add to the table.
     */
    void insertRow(String tableName, Map<String,Object> values);
    
    /** Update the rows in the given table. Any rows matching the predicate
     * are updated with the column names/values specified in the values map.
     * (The values map should not contain the special column "id".)
     * @param tableName The table to update
     * @param predicate The predicate to use to select which rows to update
     * @param values The map of column names/values to update the rows.
     */
    void updateRows(String tableName, IPredicate predicate, Map<String,Object> values);
    
    /** Update or insert a row in the table with the given row key (primary
     * key) and column names/values. (If the values map contains the special
     * column "id", its value must match rowId.)
     * @param tableName The table to update or insert into
     * @param rowKey The ID (primary key) of the row to update
     * @param values The map of column names/values to update the rows
     */
    void updateRow(String tableName, Object rowKey, Map<String,Object> values);
    
    /** Update or insert a row in the table with the given column data.
     * The primary key is indicated with the special column name of "id".
     * @param tableName The table to update or insert into
     * @param values The map of column names/values to update the rows
     */
    void updateRow(String tableName, Map<String,Object> values);
    
    /** Delete the row with the given row ID (primary key).
     * 
     * @param tableName The table from which to delete the row
     * @param rowKey The primary key of the row to delete.
     */
    void deleteRow(String tableName, Object rowKey);

    /** Query for a row with the given ID (primary key).
     * 
     * @param tableName The name of the table to query
     * @param rowKey The primary key of the row
     * @return The result set containing the row with the given ID
     */
    IResultSet getRow(String tableName, Object rowKey);
}
