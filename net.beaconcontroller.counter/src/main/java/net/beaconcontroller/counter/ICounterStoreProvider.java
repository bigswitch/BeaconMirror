package net.beaconcontroller.counter;

import java.util.List;
import java.util.Map;

/**
 * Interface in to the CounterStore.  The CounterStore is intended to be a singleton repository of all of the human-facing
 * counters in the system.  The reason for centralization is to make searching, composing and correlation between counters
 * as pain-free as possible for the end user.
 * 
 * 
 * @author kyle
 *
 */
public interface ICounterStoreProvider {
  public final String TitleDelimitor = "_";
    
  /** L2 EtherType subCategories */
  public final String L2ET_IPV4 = "L2_IPv4";
  
  public enum NetworkLayer {
      L2, L3
  }
  
  /**
   * Create a title based on switch ID, portID, vlanID, and counterName
   * If portID is -1, the title represents the given switch only
   * If portID is a non-negative number, the title represents the port on the given switch
   */
  public String createCounterName(String switchID, int portID, String counterName);
  
  /**
   * Create a title based on switch ID, portID, vlanID, counterName, and subCategory
   * If portID is -1, the title represents the given switch only
   * If portID is a non-negative number, the title represents the port on the given switch
   * For example: PacketIns can be further categorized based on L2 etherType or L3 protocol
   */
  public String createCounterName(String switchID, int portID, String counterName, String subCategory, NetworkLayer layer);
  
  /**
   * Retrieve a list of subCategories by counterName.
   * null if nothing.
   */
  public List<String> getAllCategories(String counterName, NetworkLayer layer);
  
  /**
   * Create a new ICounter and set the title.  Note that the title must be unique, otherwise this will
   * throw an IllegalArgumentException.
   * 
   * @param title
   * @return
   */
  public ICounter createCounter(String title, CounterValue.CounterType type);
  
  /**
   * Retrieves a counter with the given title, or null if none can be found.
   */
  public ICounter getCounter(String title);

  
  /**
   * Returns an immutable map of title:counter with all of the counters in the store.
   * 
   * (Note - this method may be slow - primarily for debugging/UI)
   */
  public Map<String, ICounter> getAll();
  
}
