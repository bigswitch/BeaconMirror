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

import net.beaconcontroller.counter.ICounter;
import net.beaconcontroller.counter.ICounterStoreProvider;

/**
 * @author kyle
 *
 */
//Note - I can't seem to get the @Component annotation working... falling back to the xml file approach
//@Component("counterStoreProvider")
public class CounterStore implements ICounterStoreProvider {
        
    protected class Title {
        String switchID;
        int portID;
        String counterName;
    }
    
  protected class CounterEntry {
    protected ICounter counter;
    String title;
  }
  
  /**
   * The is a hierarchical map:
   * <String of SwitchID> --> portID --> Counters
   */
  protected Map<String, Map<Integer, Map<String, CounterEntry>>> nameToCEIndex = 
      new HashMap<String, Map<Integer, Map<String, CounterEntry>>>();

  protected ICounter heartbeatCounter;
  protected ICounter randomCounter;
  
  /**
   * Parse key string into Title class
   * @param key
   * @return
   */
  protected Title getTitleFromString(String key) {
      if (key == null) {
          return null;
      }
      
      String[] fields = key.split(TitleDelimitor);
      Title title = new Title();
      title.switchID = fields[0];
      
      if (fields.length > 1) {
          try {
              title.portID = Integer.parseInt(fields[1]);
          }
          catch (NumberFormatException e){
              title.portID = -1;
          }
      }
      
      return title;
  }
  
  /* 
   * @see net.beaconcontroller.counter.ICounterStoreProvider#createTitle(java.lang.String, int, int, String)
   */
  @Override
  public String createTitle(String switchID, int portID, String counterName) {
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
  public ICounter createCounter(String key) {
    Title title = getTitleFromString(key);
    
    if (title.portID < 0) {
        throw new IllegalArgumentException("Title for counters must be of format switchid_portid_countername, where portid is a non-negative integer.");
    }
    
    Map<Integer, Map<String, CounterEntry>> switchCounters;
    Map<String, CounterEntry> counterEntries;
    CounterEntry ce;
    ICounter c;
    
    if (!nameToCEIndex.containsKey(title.switchID)) {
        switchCounters = new HashMap<Integer, Map<String, CounterEntry>>();
        nameToCEIndex.put(title.switchID, switchCounters); 
    } else {
        switchCounters = nameToCEIndex.get(title.switchID);
    }
    
    if (!switchCounters.containsKey(title.portID)) {
        counterEntries = new HashMap<String, CounterEntry>();
        switchCounters.put(title.portID, counterEntries);
    } else {
        counterEntries = switchCounters.get(title.portID);
    }
    
    if (!counterEntries.containsKey(title.counterName)) {
        c = SimpleCounter.createCounter(new Date());
        ce = new CounterEntry();
        ce.counter = c;
        ce.title = key;
        counterEntries.put(title.counterName, ce);
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
    System.out.println("CounterStore startUp");
    this.heartbeatCounter = this.createCounter("CounterStore heartbeat");
    this.randomCounter = this.createCounter("CounterStore random");
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
    @SuppressWarnings("unchecked")
    Title title = getTitleFromString(key);
    
    if (title.portID < 0) {
        throw new IllegalArgumentException("Title for counters must be of format switchid_portid_countername, where portid is a non-negative integer.");
    }

    return getCounter(title);
}

  protected ICounter getCounter(Title title) {
      if (nameToCEIndex.containsKey(title.switchID)) {
          Map<Integer, Map<String, CounterEntry>> switchCounters = nameToCEIndex.get(title.switchID);
          if (switchCounters.containsKey(title.portID)) {
              Map<String, CounterEntry> counterEntries = switchCounters.get(title.portID);
              if (counterEntries.containsKey(title.counterName)) {
                  CounterEntry ce = counterEntries.get(title.counterName);
                  return ce.counter;
              }
          }
      }
      
      return null;
  }

  /**
   * Debug method to get all of the counters currently being tracked in the system
   */
  @Override
  public Map<String, ICounter> getAll() {
    Map<String, ICounter> ret = new HashMap<String, ICounter>();
    for(Map.Entry<String, Map<Integer, Map<String, CounterEntry>>> swEntry : this.nameToCEIndex.entrySet()) {
        String switchID = swEntry.getKey();
        Map<String, ICounter> counters = getAllInSwitch(switchID);
        ret.putAll(counters);
     }
    return ret;
  }

  /**
   * Debug method to get all of the counters in the same switch.
   */
  @Override
  public Map<String, ICounter> getAllInSwitch(String switchID){
      Map<String, ICounter> ret = new HashMap<String, ICounter>();
      if (this.nameToCEIndex.containsKey(switchID)) {
          Map<Integer, Map<String, CounterEntry>> switchCounters = this.nameToCEIndex.get(switchID);
          for(Map.Entry<Integer, Map<String, CounterEntry>> portEntry : switchCounters.entrySet()) {
              int portID = portEntry.getKey().intValue();
              Map<String, ICounter> counters = getAllInSwitchPort(switchID, portID);
              ret.putAll(counters);
          }
      }
      return ret;
  }
  
  /**
   * Debug method to get all of the counters given a switch and port.
   */
  @Override
  public Map<String, ICounter> getAllInSwitchPort(String switchID, int portID) {
      Map<String, ICounter> ret = new HashMap<String, ICounter>();
      if (this.nameToCEIndex.containsKey(switchID)) {
          Map<Integer, Map<String, CounterEntry>> switchCounters = this.nameToCEIndex.get(switchID);
          if (switchCounters.containsKey(portID)) {
              Map<String, CounterEntry> counterEntries = switchCounters.get(portID);
              for(Map.Entry<String, CounterEntry> counterEntry: counterEntries.entrySet()) {
                  CounterEntry ce = counterEntry.getValue();
                  ret.put(ce.title, ce.counter);
              }
          }
      }
      return ret;
  }


}
