package net.beaconcontroller.storage;

/**
 * Interface for mapping the current row in a result set to an object.
 * This is based on the Spring JDBC support.
 * 
 * @author rob
 */
public interface IRowMapper {

    /** This method must be implemented by the client of the storage API
     * to map the current row in the result set to a Java object.
     * 
     * @param resultSet The result set obtained from a storage source query
     * @return The object created from the data in the result set
     */
    Object mapRow(IResultSet resultSet);
}
