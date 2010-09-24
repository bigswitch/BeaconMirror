package net.beaconcontroller.storage;

/** Representation of a database query. For SQL queries this maps to
 * a prepared statement, so it will be more efficient than if you use the
 * methods in IStorageSource that bypass the IQuery. For many NoSQL
 * storage sources there won't be any performance improvement from keeping
 * around the query.
 * 
 * The query interface also supports parameterized queries (i.e. which maps
 * to using ? values in a SQL query). The values of the parameters are set
 * using the setParameter method. In the storage source API the parameters
 * are named rather than positional. The format of the parameterized values
 * in the query predicates is the parameter name bracketed with question marks
 * (e.g. ?MinimumSalary? ).
 * 
 * @author rob
 *
 */
public interface IQuery {

    void setParameter(String name, Object value);
}
