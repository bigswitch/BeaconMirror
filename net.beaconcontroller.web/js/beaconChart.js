/**
 * This is a light-weight wrapper around the jit library (thejit.org)'s
 * charting package.
 * 
 */

var labelType, useGradients, nativeTextSupport, animate;

/**
 * Borrowed from thejit -- small bit of browser detection magic.
 */
(function() {
  var ua = navigator.userAgent,
      iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i),
      typeOfCanvas = typeof HTMLCanvasElement,
      nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'),
      textSupport = nativeCanvasSupport 
        && (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
  // I'm setting this based on the fact that ExCanvas provides text support for IE
  // and that as of today iPhone/iPad current text support is lame
  labelType = (!nativeCanvasSupport || (textSupport && !iStuff))? 'Native' : 'HTML';
  nativeTextSupport = labelType == 'Native';
  useGradients = nativeCanvasSupport;
  animate = !(iStuff || !nativeCanvasSupport);
})();

/**
 * The JIT library requires a window.Log object
 */
var Log = {
  elem: false,
  write: function(text){
  if(console)
    console.log(text);
    // if (!this.elem)
    // this.elem = document.getElementById('log');
    // this.elem.innerHTML = text;
    // this.elem.style.left = (500 - this.elem.offsetWidth / 2) + 'px';
  }
};

/**
 * This is the hook in to the beacon client-side formatting system (see net.beaconcontroller.web's index.html)
 * 
 */
registerDecorator(".beaconAreaChart", function(el, params) {
  //If there is already a chart here, don't create a second one...
  //NOTE - this doesn't help the case for changing from one tab to another
  //as the tabs refresh (rebuild) themselves on every click
  if($(el).data("beaconAreaChart")) {
    console.log("already found a beacon area chart at el");
    return;
  }

  
  $(el).height(200);
  $(el).css('marginBottom', 35);
  var seriesDataPath = params["pathToJson"]; //See ChartSection.java
  var autoReload = params["autoReload"]; //should be a number converted to a string, or null
  
  var beaconChart = new $jit.AreaChart({
    // id of the visualization container
    injectInto: el.id,
    
    animate: true,
  //separation offsets  
    Margin: {  
      top: 5,  
      left: 5,  
      right: 5,  
      bottom: 5  
    },  
    labelOffset: 10,  
    //whether to display sums  
    showAggregates: true,  
    //whether to display labels at all  
    showLabels: true,  
    //could also be 'stacked'  
    type: useGradients? 'stacked:gradient' : 'stacked',  
    //label styling  
    Label: {  
      type: 'HTML', //can be 'Native' or 'HTML'  
      size: 9,  
      family: 'Arial',  
      color: 'black',
      overrideable: true,
    },
    
    //enable tips  
    Tips: {  
      enable: true,  
      onShow: function(tip, elem) {  
        for(var i in elem)
          //console.log("elem: " + i + ", " + elem[i]);
        tip.innerHTML = "<b>" + elem.name + "</b>: " + elem.value;  
      }  
    },  
    

    Events: {
      enable: true,
      onClick: function(node, eventInfo, e) {
        console.log("onClick: " + eventInfo.node.name); //note this is not stored in 'node' but rather in the ST underneath the AreaChart
        window.tsnode = node;
        window.evinfo = eventInfo;
        window.ee = e;
      },
      onDragStart: function(node, eventInfo, e) {
        console.log("on drag start: " + node.name);
      },
      onDragEnd: function(node, eventInfo, e) {
        console.log("ondrag end: " + node.name + " " + eventInfo.getPos().x + ", " + eventInfo.getPos().y );
      }
    },
    //add left and right click handlers  
    //filterOnClick: true,  
    restoreOnRightClick:true  
    
    
  });
  
  /**
   * Do some deep hacking inside the jit library to get at a few of the core
   * functions we want to mess with
   */
  beaconChart.st.events.config.onDragStart = function(node, eventInfo, e) {
    console.log("drag start for node " + node.name);
  };
  
  beaconChart.st.events.config.onDragMove = function(node, eventInfo, e) {
    var pos = eventInfo.getPos();  
    node.pos.setc(pos.x, node.pos.y);  
    beaconChart.st.plot();     
  };

  beaconChart.st.events.config.onDragEnd = function(node, eventInfo, e) {
    console.log("drag start for node " + node.name);
  };
  
  beaconChart.st.events.config.onDragCancel = function(node, eventInfo, e) {
    console.log("drag start for node " + node.name);
  };


  
  /*
  var oldCreateLabel = beaconChart.st.controller.onCreateLabel;
  beaconChart.st.controller.onCreateLabel = function(domElement, node) {
    console.log("in the hijaacked create label for node: " + node.id + (node.id.indexOf('Tue') == -1));
    oldCreateLabel(domElement, node);
    if(node.id.indexOf('Tue') == -1)
      domElement.style.display = 'none';

  };
  */
  var oldPlaceLabel = beaconChart.st.controller.onPlaceLabel;
  var labelsPlaced = 0;
  beaconChart.st.controller.onPlaceLabel = function(domElement, node) {
    //Notes - 
    //node.name will point to the date associated with this node.
    //node.id points to a long that may be random + node.name
    labelsPlaced++;
    //console.log("in the hijaacked place label for node: labelsPlaced: " + labelsPlaced);
    var nodeCount = 0;
    for(var i in beaconChart.st.graph.nodes)
      nodeCount++;
    
    oldPlaceLabel(domElement, node);
    
    //Only make one out of every five labels visible
    if(labelsPlaced % 5 != 0)
      domElement.style.display = 'none';

  };
  
    
  
  //for debugging only
  window.bc = beaconChart;
  /*
   * Sample expected json object:
   * var json2 = {  
      'values': [
        {  
          'label': 'date A',  
          'values': [10, 40, 15, 7]  
        },   
        {  
          'label': 'date B',  
          'values': [30, 40, 45, 9]  
        },
      ]
    };
   */
  /**
   * Method to clip the json object returned to a given start and end date
   */
  clipSeriesData = function(seriesData) {
    var clippedData = {};
    clippedData['label'] = seriesData['label'];
    clippedData['values'] = [];
    
    //Only show some of the x axis labels (for visual prettiness)
    var xAxisLabelsToShow = 5;
    var showLabelNumber = Math.round(seriesData['values'].length / xAxisLabelsToShow);
    
    for(var i in seriesData['values']) {
      var val = seriesData['values'][i];
      var pointDate = new Date(val['label']);
      
      if(i < 40) //TODO - replace this with a date clipping function
        clippedData['values'].push(val);
    }
      
    return clippedData;
  }
  
  console.log('now loading json from ' + seriesDataPath);
  $.getJSON(seriesDataPath, function(seriesData){
    console.log('loaded json from ' + seriesDataPath);   
    var cd = clipSeriesData(seriesData);
    beaconChart.loadJSON(cd);
  });
  
  
  
  
  console.log('setting interval for beaconChart ' + beaconChart.getMaxValue());
  
  //Store off the chart object in the matching div if it needs to be retrieved later
  $(el).data("beaconAreaChart", beaconChart);
  
  /*
  var timerId;
  $(el).bind("tabLoad", function(){
    timerId = setInterval(function () {
      $.getJSON(seriesDataPath, function(seriesData){
        var cd = clipSeriesData(seriesData);
        beaconChart.config.animate = false; //turn off animations for subsequent loads
        beaconChart.loadJSON(cd);
      });
    }, 5000);
  });
  */

  $(el).bind("tabHide", function(){
    clearInterval(timerId);
  });

  
  $(el).bind("tabRemove", function(){
    console.log("beaconChart - I'm being removed! " + beaconChart);
  });
  
  
  
  
  
});




