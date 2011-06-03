/**
 * 
 */
package net.beaconcontroller.counter.internal;

import java.util.Date;

import net.beaconcontroller.counter.CounterValue;
import net.beaconcontroller.counter.CountSeries;
import net.beaconcontroller.counter.ICounter;


/**
 * This is a simple counter implementation that doesn't support data series.
 * The idea is that beacon only keeps the realtime value for each counter,
 * statd, a statistics collection daemon, samples counters at a user-defined interval
 * and pushes the values to a database, which keeps time-based data series. 
 * @author Kanzhe
 *
 */
public class SimpleCounter implements ICounter {

  protected CounterValue counter;
  protected Date samplingTime;
  protected Date startDate;
  
  /**
   * Factory method to create a new counter instance.  
   * 
   * @param startDate
   * @return
   */
  public static ICounter createCounter(Date startDate, CounterValue.CounterType type) {
    SimpleCounter cc = new SimpleCounter(startDate, type);
    return cc;
    
  }
  
  /**
   * Protected constructor - use createCounter factory method instead
   * @param startDate
   */
  protected SimpleCounter(Date startDate, CounterValue.CounterType type) {
    init(startDate, type);
  }
  
  protected void init(Date startDate, CounterValue.CounterType type) {
    this.startDate = startDate;
    this.samplingTime = new Date();
    this.counter = new CounterValue(type);
  }
  
  /**
   * This is the key method that has to be both fast and very thread-safe.
   */
  @Override
  public void increment() {
    this.increment(new Date(), (long)1);
  }
  
  @Override
  public void increment(Date d, long delta) {
    this.samplingTime = d;
    this.counter.increment(delta);
  }
  
  public void setCounter(Date d, CounterValue value) {
      this.samplingTime = d;
      this.counter = value;
  }
  
  /**
   * This is the method to retrieve the current value.
   */
  @Override
  public CounterValue getCounterValue() {
    return this.counter;
  }

  /**
   * This is the method to retrieve the last sampling time.
   */
  @Override
  public Date getCounterDate() {
    return this.samplingTime;
  }
  
  /**
   * Reset value.
   */
  @Override
  public void reset(Date startDate) {
    init(startDate, this.counter.getType());
  }
  
  @Override
  /**
   * This method only returns the real-time value.
   */
  public CountSeries snapshot(DateSpan dateSpan) {
    long[] values = new long[1];
    values[0] = this.counter.getLong();
    return new CountSeries(this.samplingTime, DateSpan.DAYS, values);
  }

  
  
}
