package net.beaconcontroller.counter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Interface in to the CounterStore.  The CounterStore is intended to be a singleton repository of all of the human-facing
 * counters in the system.  The reason for centralization is to make searching, composing and correlation between counters
 * as pain-free as possible for the end user.
 * 
 * 
 * @author kyle
 *
 */
@Service
public interface ICounterStoreProvider {
  
  /**
   * Create a new ICounter and set the title.  Note that the title must be unique, otherwise this will
   * throw an IllegalArgumentException.
   * 
   * @param title
   * @return
   */
  public ICounter createCounter(String title);
  
  /**
   * Retrieves a counter with the given title, or null if none can be found.
   */
  public ICounter getCounter(String title);
  
  /**
   * Adds search-able (human readable) metadata tag
   * to an instance of a counter.
   * 
   */
  public void addTag(ICounter counter, String tag);
  
  /**
   * Search the store for counters that have the specified tag.
   * 
   * @param key
   * @param value
   */
  public Set<ICounter> search(String tag);
  
  /**
   * Returns an immutable map of title:counter with all of the counters in the store.
   * 
   * (Note - this method may be slow - primarily for debugging/UI)
   */
  public Map<String, ICounter> getAll();
}
