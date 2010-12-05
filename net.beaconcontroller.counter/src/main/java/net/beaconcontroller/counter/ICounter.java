/**
 * Simple interface for a counter whose value can be retrieved in several different
 * time increments (last x seconds, minutes, hours, days)
 */
package net.beaconcontroller.counter;

import java.util.Date;

/**
 * @author kyle
 *
 */
public interface ICounter {
  
  /**
   * Most commonly used method
   */
  public void increment();
  
  /**
   * Used primarily for testing - no performance guarantees
   */
  public void increment(Date d, long delta);
  
  /**
   * Returns a CountSeries that is a snapshot of the counter's values for the given dateSpan.  (Further changes
   * to this counter won't be reflected in the CountSeries that comes  back.)
   * 
   * @param startDate
   * @return
   */
  public CountSeries snapshot(DateSpan dateSpan);
  

  public static enum DateSpan {
    SECONDS,
    MINUTES,
    HOURS,
    DAYS,
    WEEKS
  }


  
}
