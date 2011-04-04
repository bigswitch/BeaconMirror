/**
 * Implements a very simple central store for system counters
 */
package net.beaconcontroller.counter.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import net.beaconcontroller.counter.CounterValue;
import net.beaconcontroller.counter.ICounter;
import net.beaconcontroller.counter.ICounterStoreProvider;

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
  
  /* 
   * @see net.beaconcontroller.counter.ICounterStoreProvider#createCounterName(java.lang.String, int, int, String)
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
