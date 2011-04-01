/**
 * 
 */
package net.beaconcontroller.counter.internal;

import java.util.Date;

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

  protected Date date;
  protected long value;    
  protected Date startDate;
  
  /**
   * Factory method to create a new counter instance.  
   * 
   * @param startDate
   * @return
   */
  public static ICounter createCounter(Date startDate) {
    SimpleCounter cc = new SimpleCounter(startDate);
    return cc;
    
  }
  
  /**
   * Protected constructor - use createCounter factory method instead
   * @param startDate
   */
  protected SimpleCounter(Date startDate) {
    init(startDate);
  }
  
  protected void init(Date startDate) {
    this.startDate = startDate;
    this.value = 0;
    this.date = new Date(0);
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
    this.date = d;
    this.value += delta;
  }
  
  /**
   * This is the method to retrieve the current value.
   */
  @Override
  public long get() {
    return value;
  }

  /**
   * Reset value.
   */
  @Override
  public void reset(Date startDate) {
    init(startDate);
  }
  
  @Override
  /**
   * This method only returns the real-time value.
   */
  public CountSeries snapshot(DateSpan dateSpan) {
    long[] values = new long[1];
    values[0] = this.value;
    return new CountSeries(this.date, DateSpan.DAYS, values);
  }

  
  
}
