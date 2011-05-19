/**
 * Implements a very simple central store for system counters
 */
package net.beaconcontroller.counter.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import net.beaconcontroller.counter.CounterValue;
import net.beaconcontroller.counter.ICounter;
import net.beaconcontroller.counter.ICounterStoreProvider;
import net.beaconcontroller.counter.ICounterStoreProvider.NetworkLayer;

/**
 * @author kyle
 *
 */
//Note - I can't seem to get the @Component annotation working... falling back to the xml file approach
//@Component("counterStoreProvider")
public class CounterStore implements ICounterStoreProvider {
          
  protected class CounterEntry {
    protected ICounter counter;
    String title;
  }
  
  /**
   * A map of counterName --> Counter
   */
  protected Map<String, CounterEntry> nameToCEIndex = 
      new HashMap<String, CounterEntry>();
  
  protected ICounter heartbeatCounter;
  protected ICounter randomCounter;
  
  /**
   * Counter Categories grouped by network layers
   * NetworkLayer -> CounterToCategories
   */
  Map<NetworkLayer, Map<String, List<String>>> layeredCategories = 
      new HashMap<NetworkLayer, Map<String, List<String>>> ();
      
  
  /* 
   * @see net.beaconcontroller.counter.ICounterStoreProvider#createCounterName(java.lang.String, int, String)
   */
  @Override
  public String createCounterName(String switchID, int portID, String counterName) {
      if (portID < 0) {
          return switchID + TitleDelimitor + counterName;
      } else {
          return switchID + TitleDelimitor + portID + TitleDelimitor + counterName;
      }
  }
  
  /* 
   * @see net.beaconcontroller.counter.ICounterStoreProvider#createCounterName(java.lang.String, 
   *                int, String, String, NetworkLayer)
   */
  @Override
  public String createCounterName(String switchID, int portID, String counterName, String subCategory, NetworkLayer layer) {
      String fullCounterName = "";
      String groupCounterName = "";
      
      if (portID < 0) {
          groupCounterName = switchID + TitleDelimitor + counterName;
          fullCounterName = groupCounterName + TitleDelimitor + subCategory;
      } else {
          groupCounterName = switchID + TitleDelimitor + portID + TitleDelimitor + counterName;
          fullCounterName = groupCounterName + TitleDelimitor + subCategory;
      }
      
      Map<String, List<String>> counterToCategories;      
      if (layeredCategories.containsKey(layer)) {
          counterToCategories = layeredCategories.get(layer);
      } else {
          counterToCategories = new HashMap<String, List<String>> ();
          layeredCategories.put(layer, counterToCategories);
      }
      
      List<String> categories;
      if (counterToCategories.containsKey(groupCounterName)) {
          categories = counterToCategories.get(groupCounterName);
      } else {
          categories = new ArrayList<String>();
          counterToCategories.put(groupCounterName, categories);
      }
      
      if (!categories.contains(subCategory)) {
          categories.add(subCategory);
      }
      return fullCounterName;
  }
  
  /* 
   * @see net.beaconcontroller.counter.ICounterStoreProvider#getAllCategories(java.lang.String, String)
   */
  @Override
  public List<String> getAllCategories(String counterName, NetworkLayer layer) {
      if (layeredCategories.containsKey(layer)) {
          Map<String, List<String>> counterToCategories = layeredCategories.get(layer);
          if (counterToCategories.containsKey(counterName)) {
              return counterToCategories.get(counterName);
          }
      }
      return null;
  }
  /* 
   * @see net.beaconcontroller.counter.ICounterStoreProvider#createCounter(java.lang.String)
   */
  @Override
  public ICounter createCounter(String key, CounterValue.CounterType type) {
    CounterEntry ce;
    ICounter c;
    
    if (!nameToCEIndex.containsKey(key)) {
        c = SimpleCounter.createCounter(new Date(), type);
        ce = new CounterEntry();
        ce.counter = c;
        ce.title = key;
        nameToCEIndex.put(key, ce);
    } else {
        throw new IllegalArgumentException("Title for counters must be unique, and there is already a counter with title " + key);
    }

    return c;
  }
  
  /**
   * Post construction init method to kick off the health check and random (test) counter threads
   */
  @PostConstruct
  public void startUp() {
    this.heartbeatCounter = this.createCounter("CounterStore heartbeat", CounterValue.CounterType.LONG);
    this.randomCounter = this.createCounter("CounterStore random", CounterValue.CounterType.LONG);
    //Set a background thread to flush any liveCounters every 100 milliseconds
    Timer healthCheckTimer = new Timer();
    healthCheckTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() {
          heartbeatCounter.increment();
          randomCounter.increment(new Date(), (long) (Math.random() * 100)); //TODO - pull this in to random timing
        }
    }, 100, 100);
  }
  
  public ICounter getCounter(String key) {
      if (nameToCEIndex.containsKey(key)) {
          return nameToCEIndex.get(key).counter;
      } else {
          return null;
      }
  }

  /**
   * Debug method to get all of the counters currently being tracked in the system
   */
  @Override
  public Map<String, ICounter> getAll() {
    Map<String, ICounter> ret = new HashMap<String, ICounter>();
    for(Map.Entry<String, CounterEntry> counterEntry : this.nameToCEIndex.entrySet()) {
        String key = counterEntry.getKey();
        ICounter counter = counterEntry.getValue().counter;
        ret.put(key, counter);
     }
    return ret;
  }

}
