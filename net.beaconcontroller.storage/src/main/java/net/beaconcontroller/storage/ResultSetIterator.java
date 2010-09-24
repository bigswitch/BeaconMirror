package net.beaconcontroller.storage;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Iterator wrapper for an IResultSet, useful for iterating through query
 * results in an enhanced for (foreach) loop.
 * 
 * Note that the iterator manipulates the state of the underlying IResultSet.
 */
public class ResultSetIterator implements Iterator<IResultSet> {
    private IResultSet resultSet;
    private boolean hasAnother;
    private boolean peekedAtNext;
    
    public ResultSetIterator(IResultSet resultSet) {
        this.resultSet = resultSet;
        this.peekedAtNext = false;
    }
    
    @Override
    public IResultSet next() {
        if (!peekedAtNext) {
            hasAnother = resultSet.next();
        }
        peekedAtNext = false;
        if (!hasAnother)
            throw new NoSuchElementException();
        return resultSet;
    }
    
    @Override
    public boolean hasNext() {
        if (!peekedAtNext) {
            hasAnother = resultSet.next();
            peekedAtNext = true;
        }
        return hasAnother;
    }
    
    /** Row removal is not supported; use IResultSet.deleteRow instead.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
