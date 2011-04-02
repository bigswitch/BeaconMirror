package net.beaconcontroller.counter.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.beaconcontroller.counter.CounterValue;
import net.beaconcontroller.counter.CountSeries;
import net.beaconcontroller.counter.ICounter;
import net.beaconcontroller.counter.ICounter.DateSpan;
import net.beaconcontroller.counter.ICounterStoreProvider;
import net.beaconcontroller.web.IWebManageable;
import net.beaconcontroller.web.view.BeaconJsonView;
import net.beaconcontroller.web.view.BeaconViewResolver;
import net.beaconcontroller.web.view.ModelUtils;
import net.beaconcontroller.web.view.Tab;
import net.beaconcontroller.web.view.layout.Layout;
import net.beaconcontroller.web.view.layout.OneColumnLayout;
import net.beaconcontroller.web.view.layout.TwoColumnLayout;
import net.beaconcontroller.web.view.section.StringSection;
import net.beaconcontroller.web.view.section.TableSection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;

/**
 * This class sets up the web UI component for the structures in
 * net.beaconcontroller.counter -- the counter store.
 * 
 * It uses the net.beaconcontroller.web mgmt framework /
 * 
 * @author Kyle Forster (kyle.forster@bigswitch.com)
 * 
 */
@Controller
@RequestMapping("/counter")
public class CounterWebManageable implements IWebManageable {
    protected static Logger log = LoggerFactory.getLogger(CounterWebManageable.class);
    protected ICounterStoreProvider counterStore;
    protected List<Tab> tabs;

    public CounterWebManageable() {
        tabs = new ArrayList<Tab>();
        tabs.add(new Tab("Overview", "/wm/counter/overview.do"));
    }
    
    
    /**
     * Autowired to set the counter store
     * @param counterStore
     */
    @Autowired
    public void setCounterStoreProvider(ICounterStoreProvider counterStore) {
      this.counterStore = counterStore;
    }
    
    @Override
    public String getName() {
        return "Counters";
    }

    @Override
    public String getDescription() {
        return "View in to the counter store of Beacon.";
    }

    @Override
    public List<Tab> getTabs() {
        return tabs;
    }

    @RequestMapping("/overview")
    public String overview(Map<String, Object> model) {
        Layout layout = new TwoColumnLayout();
        model.put("layout", layout);

        // Description
        layout.addSection(
                new StringSection("Welcome",
                        "Thanks for using the counter store!"),
                TwoColumnLayout.COLUMN1);


        // Counter List Table
        List<String> columnNames = new ArrayList<String>();
        List<List<String>> cells = new ArrayList<List<String>>();
        columnNames = new ArrayList<String>();
        columnNames.add("Counter Name");
        columnNames.add("Chart");
        cells = new ArrayList<List<String>>();
        for (Entry<String, ICounter> entry : this.counterStore.getAll().entrySet()) {
            List<String> row = new ArrayList<String>();
            String counterTitle = entry.getKey().toString();
            row.add(counterTitle);
            String encodedTitle = null;
            try {
              encodedTitle = URLEncoder.encode(counterTitle, "UTF-8");
            } catch (UnsupportedEncodingException e) {
              //Swallow silently if this encoding isn't supported, i.e. the link will be broken
            }
            row.add("<a href=\"/wm/counter/port/" + encodedTitle + "\"" +
                    " class=\"beaconNewRefreshingTab\"" +
                    " name=\"" + counterTitle + "\">Chart</a>");
            //todo - add another interesting column here
            cells.add(row);
        }
        TableSection ts = new TableSection("Counters", columnNames, cells);
        ts.dontEscapeXML("Chart");
        layout.addSection(ts, TwoColumnLayout.COLUMN2);

        return BeaconViewResolver.SIMPLE_VIEW;
    }
    
    @RequestMapping("/port/{counterTitle}")
    public String portCounter(Map<String, Object> model, @PathVariable String counterTitle) {
      OneColumnLayout layout = new OneColumnLayout();
      try {
        counterTitle = URLDecoder.decode(counterTitle, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        //Just leave counterTitle undecoded if there is an issue - fail silently
      }
      model.put("title", "Counter: " + counterTitle);
      model.put("layout", layout);

      ICounter counter = this.counterStore.getCounter(counterTitle);      
      
      List<String> columnNames = Arrays.asList("Date", "Count");
      List<List<String>> cells = new ArrayList<List<String>>();
      Date d = counter.getCounterDate();
      CounterValue v = counter.getCounterValue();
      
      List<String> row = Arrays.asList(d.toString(), "" + v.getLong());
      cells.add(row);
      
      layout.addSection(new TableSection("Counter " + counterTitle, columnNames, cells), null);
      
      return BeaconViewResolver.SIMPLE_VIEW;
  }
    

    @RequestMapping("/port/{counterTitle}/json")
    public View snapshotCounter(Map<String, Object> model, @PathVariable String counterTitle, 
                                                             @RequestParam(required=false) String format) {
        
        ICounter counter = this.counterStore.getCounter(counterTitle); //Do I need to URL-unencode this? -KYLE
        
        Map<String, Object> m = new HashMap<String, Object>();
        m.put(counter.getCounterDate().toString(), counter.getCounterValue().getLong());
        ModelUtils.generateTableModel(model, format, Arrays.asList(m), "dateSpan");

        return new BeaconJsonView();
        
    }
}
