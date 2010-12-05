/**
 * Implements a very simple central store for system counters
 */
package net.beaconcontroller.counter.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import net.beaconcontroller.counter.ICounter;
import net.beaconcontroller.counter.ICounterStoreProvider;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.collections.MultiMap;

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
    protected Set<String> tags = new HashSet<String>();
  }
  
  protected Set<CounterEntry> allCounters = new HashSet<CounterEntry>();
  protected MultiMap tagToCEIndex = MultiValueMap.decorate(new HashMap<String, CounterEntry>(), HashSet.class);
  protected Map<ICounter, CounterEntry> counterToCEIndex = new HashMap<ICounter, CounterEntry>();
  protected ICounter heartbeatCounter;
  protected ICounter randomCounter;
  
  /* 
   * @see net.beaconcontroller.counter.ICounterStoreProvider#createCounter(java.lang.String)
   */
  @Override
  public ICounter createCounter(String title) {
    ICounter c = this.getCounter(title);
    if(c != null)
      throw new IllegalArgumentException("Title for counters must be unique, and there is already a counter with title " + title);
      
    c = ConcurrentCounter.createCounter(new Date());
    CounterEntry ce = new CounterEntry();
    ce.counter = c;
    ce.title = title;
    
    allCounters.add(ce);
    this.counterToCEIndex.put(c, ce);

    this.addTag(c, title);
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
  
  public ICounter getCounter(String title) {
    @SuppressWarnings("unchecked")
    Set<CounterEntry> ces = (Set<CounterEntry>)tagToCEIndex.get(title);
    if(ces == null)
      return null;
    
    for(CounterEntry ce : ces) {
      if(ce.title.equals(title))
        return ce.counter;
    }
    return null;
  }

  /* 
   * Throws an IllegalArgumentException if the counter didn't come from this store (would be very odd - all counters should come from this
   * store).
   * 
   * (Synchronized to avoid corruption of the index structures underneath, but note that thread-safetiness wrt to the get
   * and search methods is not guaranteed.)
   * 
   * @see net.beaconcontroller.counter.ICounterStoreProvider#addMetadata(net.beaconcontroller.counter.ICounter, java.lang.String, java.lang.String)
   */
  @Override
  public synchronized void addTag(ICounter counter, String tag) {
    CounterEntry ce = this.counterToCEIndex.get(counter);
    if(ce == null)
      throw new IllegalArgumentException("Couldn't find counter " + counter + " in the CounterStore...");
    
    ce.tags.add(tag);
    this.tagToCEIndex.put(tag, ce);
  }

  /* 
   * Currently supports only exact matches.  TODO - support inexact matches.
   * 
   * @see net.beaconcontroller.counter.ICounterStoreProvider#search(java.lang.String, java.lang.String)
   */
  @Override
  public Set<ICounter> search(String tag) {
    @SuppressWarnings("unchecked")
    Set<CounterEntry> ces = (Set<CounterEntry>)tagToCEIndex.get(tag);
    HashSet<ICounter> ret = new HashSet<ICounter>();
    for(CounterEntry ce : ces) {
      ret.add(ce.counter);
    }
    return ret;
  }

  /**
   * Debug method to get all of the counters currently being tracked in the system
   */
  @Override
  public Map<String, ICounter> getAll() {
    Map<String, ICounter> ret = new HashMap<String, ICounter>();
    for(CounterEntry ce : this.allCounters)
      ret.put(ce.title, ce.counter);
    return ret;
  }


}
