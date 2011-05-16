/**
* Implementation of a simple script loader to pull in js files dynamically
* (used only when loading tabs)
*/
var scriptFilesQueued = [];
var scriptFilesLoaded = "";
var scriptOnLoadCallbacks = [];

function loadScript(pathToJs) {
    scriptFilesQueued.push(pathToJs);
};

function loadScripts(callbackWhenDone) {
    var scriptPath = scriptFilesQueued.pop();

    if(!scriptPath) {
        callbackWhenDone();
        return;
    }

    if(scriptFilesLoaded.indexOf(scriptPath) == -1 ) {
        scriptFilesLoaded += "[" + scriptPath + "]";
        $.getScript(scriptPath, function() {
            loadScripts(callbackWhenDone);
        });
    }
    else {
        //if the script is already loaded, just continue recursion
        loadScripts(callbackWhenDone);
    }
};

/**
 * Set this to true only after all libraries have loaded
 */
allLibrariesLoaded = false;
$(window).load(function() {
    window.allLibrariesLoaded = true;
});


/**
* Registers a "decorator" -- this is basically a function that is given a matching dom element as an argument
* every time a tab is loaded (or refreshed).  The string "selector" uses jquery syntax to find the dom element.
* The callback should have the signature callback(element, params) where element is the DOM element matched by
* the selector string and params is a map of key/value strings that came from the back end as html param elements.
* 
* registerDecorator(".column", function(el, params) {
*   $(el).sortable({
*     connectWith: '.column',
*   });
*   $(el).disableSelection();
* });
* 
* Note that if two decorators register for the same selectorString, only one of them will get called
* (i.e. stay out of the global namespace if your decorators are application specific!)
*
*/
var decorators = {};

function registerDecorator(selectorString, callback) {
    decorators[selectorString] = callback;
};

function getDecorators() {
    return decorators;
};

/**
 * Primary method where client-side templating is applied.
 *
 * Processing step that will go through the html used in a freshly loaded application's tab and decorate it
 * with the css classes or jquery functionality to change the nice semantic html in to something
 * with a bit more flash.
 *
 * tab should be a dom element representing the contents of a single tab
 */
function decorateTab(tab) {
    var decorators = getDecorators();
    for(var selectorString in decorators) {
      var decoratorCallback = decorators[selectorString];
      $(tab).find(selectorString).each(function(index, el) {
        var params = {};
        $(this).children("param").each(function(index, el){
          var key = $(el).attr("name");
          var val = $(el).attr("value");
          params[key] = val;
        });
        decoratorCallback(el, params);
      });
    }
};
  
/**
 * Sends an event to all of the elements matching a registered decorator - for advanced decorator use.
 *
 * Example: inside a decorator:
 * var timerId;
 *  $(el).bind("tabLoad", function(){
 *   timerId = setInterval(function () {
 *     $.getJSON(seriesDataPath, function(seriesData){
 *       beaconChart.loadJSON(seriesData);
 *     });
 *   }, 5000);
 * });
 *
 * $(el).bind("tabHide", function(){
 *   clearInterval(timerId);
 * });
 */
function sendBeaconEvent(tab, eventName, extraParams) {
    var decorators = getDecorators();
    for(var selectorString in decorators) {
      //console.log("Sending event to selector string: " + selectorString + " " + eventName);
      $(tab).find(selectorString).each(function(index, el) {
        $(el).trigger(eventName, extraParams);
      });
    }
};
  

/**
 * Decoration defaults
 */
registerDecorator(".column", function(el, params) {
  $(el).sortable({
    connectWith: '.column',
  });
  $(el).disableSelection();
});

registerDecorator(".section", function(el, params) {
  $(el).addClass("ui-widget ui-widget-content ui-helper-clearfix ui-corner-all");
  $(el).find(".section-header")
      .addClass("ui-widget-header ui-corner-all")
      .prepend('<span class="ui-icon ui-icon-minusthick"></span>');
});

registerDecorator(".section-header .ui-icon", function(el, params) {
  $(el).click(function() {
    $(this).toggleClass("ui-icon-minusthick").toggleClass("ui-icon-plusthick");
    $(this).parents(".section:first").find(".section-content").toggle();
  });
});

registerDecorator(".beaconTable", function(el, params) {
  $(el).dataTable({
    bJQueryUI: true,
    bPaginate: false,
    bLengthChange: true,
    bFilter: false,
    bSort: true,
    bInfo: true,
    bRetrieve: true, //if this table already exists, retrieve it rather than creating a new one
  });
  
  //add a hover over rows.
  $(el).find("tr").hover(function () {
    $(this).toggleClass("rowHover");
  });
});


//Wrap up any dialog box-oriented buttons (see SimpleWebManageableTemplate.wrapWithDialogBox for more detail)
registerDecorator(".beaconGetDialog", function(el, params) {
  $(el).click(function() {
    var url = $(this).attr("href");
    $.get(url, function(response) {
      $("#beaconDialogBoxTemplate" ).html(response);
      $("#beaconDialogBoxTemplate" ).dialog({
        height: 140,
        modal: true,
        resizable: false,
        buttons: {
          OK: function() {
            $( this ).dialog( "close" );

            //reload the currently open tab
            var $tabs = $('#center').tabs();
            var selectedIndex = $tabs.tabs('option', 'selected');
            $tabs.tabs('load', selectedIndex);
          }
        }
      });
    });
    return false; //Return false from the click method to stop any native browser processing
  }); // close the click() inner function
}); //close the decorator function

// Wrap a refresh link
registerDecorator(".beaconRefreshTab", function(el, params) {
  $(el).click(function() {
    var url = $(this).attr("href");
    $.get(url, function(response) {
      //reload the currently open tab
      var $tabs = $('#center').tabs();
      var selectedIndex = $tabs.tabs('option', 'selected');
      $tabs.tabs('load', selectedIndex);
    });
    return false; //Return false from the click method to stop any native browser processing
  }); // close the click() inner function
}); //close the each() loop

// Wrap a new refreshing tab link
registerDecorator(".beaconNewRefreshingTab", function(el, params) {
  $(el).click(function() {
    var $tabs = $("#center");

    // add the new tab
    var oldTemplate = $tabs.tabs( "option", "tabTemplate");
    // use a custom template with an x to close the tab
    $tabs.tabs( "option", "tabTemplate", '<li><a href="#{href}">#{label}</a> <span class="ui-icon ui-icon-close">Remove Tab</span></li>');
    var t = $tabs.tabs('add', $(this).attr("href"), $(this).attr("name"));
    $tabs.tabs( "option", "tabTemplate", oldTemplate);
    // select it
    var index = $tabs.tabs("length")-1;
    $tabs.tabs("select", index);
    // refresh every 5 seconds if we are selected
    var timerId = setInterval(function () {
      if (index == $tabs.tabs('option', 'selected')) {
        $tabs.tabs('load', index);
      }
    }, 5000);

    // add the close handler
    $('#center span.ui-icon-close').last().click(function() {
      var index = $('li',$tabs).index($(this).parent());
      $tabs.tabs('remove', index);
      clearInterval(timerId);
    });
    return false; //Return false from the click method to stop any native browser processing
  }); // close the click() inner function
}); //close the each() loop


// Wrap a new  tab link
registerDecorator(".beaconNewTab", function(el, params) {
  $(el).click(function() {
    var $tabs = $("#center");

    // add the new tab
    var oldTemplate = $tabs.tabs( "option", "tabTemplate");
    // use a custom template with an x to close the tab
    $tabs.tabs( "option", "tabTemplate", '<li><a href="#{href}">#{label}</a> <span class="ui-icon ui-icon-close">Remove Tab</span></li>');
    var t = $tabs.tabs('add', $(this).attr("href"), $(this).attr("name"));
    $tabs.tabs( "option", "tabTemplate", oldTemplate);
    // select it
    var index = $tabs.tabs("length")-1;
    $tabs.tabs("select", index);

    // add the close handler
    $('#center span.ui-icon-close').last().click(function() {
      var index = $('li',$tabs).index($(this).parent());
      $tabs.tabs('remove', index);
    });
    return false; //Return false from the click method to stop any native browser processing
  }); // close the click() inner function
}); //close the each() loop

/**
 * Parses the window.location.hash in to an object with two properties -- appName and tabIndex.
 */
function parseLocation() {
      var appName = window.appList ? window.appList[0].name : "";
      var tabIndex = 0;
      
      if(window.location.hash) {
              var s = window.location.hash.split("~", 2);
              appName = s ? s[0].substring(1) : window.appList[0].name; //note - need to split off the leading hash
              tabIndex = s.length > 1 ? parseInt(s[1], 10) : 0; 
      } 

      appName = unescape(appName);
      return {'appName' : appName, 'tabIndex' : tabIndex};
}
  
function setLocation(appName, tabIndex) {
      if (setLocation.ignore) {
          return;
      }

      var defaultName =  window.appList ? window.appList[0].name : "";
      var defaultIndex = 0;

      // default arguments
      if (!appName) appName = defaultName;
      if (!tabIndex) tabIndex = defaultIndex;

      // set hash (if required)
      if ((appName == defaultName) && (tabIndex == defaultIndex)) {
          window.location.hash = "";
      }
      else {
          window.location.hash = appName + "~" + tabIndex;
      }
}
setLocation.ignore = false;

function handleLocation() {
    if(window.location.hash) {
        var s = parseLocation();
        showTab(s.appName, s.tabIndex);
    }
    else {
        if(window.appList) {
            // NOTE: this fires first time when window.appList is null
            showTab(window.appList[0].name, 0);
        }
    }
}

/**
 * Removes all of the tabs from the main panel and replaces them with the tabs referenced
 * here.  Takes an array of tab objects {url: "some URL", title: "some string"}.  The optional tabIndex
 * specifies which tabd to show
 **/
function loadTabs(tabSet) {
    // use static variable currentTabSet - return if already using same tabSet
    if(tabSet == loadTabs.currentTabSet)
        return;
    
    var $tabs = $("#center");
    loadTabs.currentTabSet = tabSet;
    
    //remove all existing tabs
    while($tabs.tabs("length") > 0)
        $tabs.tabs("remove", 0);

    //Add in the tabs that came with the tab set
    for(var i = 0; i < tabSet.length; i++) {
        $tabs.tabs('add', tabSet[i].url, tabSet[i].title);
    }

};
  
/**
 * Show the app specified by appName - note that this can come from either forward/back buttons,
 * from the app accordian control or from the tab control.
 *
 * It can be called multiple times.
 * If tabIndex is null, just show the first tab in the app
 * If appName is null, just show the tabIndex in the currently active app
 */
function showTab(appName, tabIndex) {
    //Use static vars for currentAppName and currentTabIndex - declare here for readability
    showTab.currentAppName;
    showTab.currentTabIndex;        

    var appList = window.appList;   // fix this so appList isn't a global variable  
    if(!appList) {
        // may happen if showApp gets called early
        return;
    }
    if(!appName) appName = showTab.currentAppName;
    if(!tabIndex) tabIndex = 0;

    //Find the underlying app
    var app = null;
    var appIndex = -1;
    for(var i = 0; i < appList.length; i++) {
      if (appList[i].name == appName) {
        app = appList[i];
        appIndex = i;
        break;
      }
    }
    if(!app) {
        //If there is no app with this name, just return
        return;
    }
    
    // update the app accordian if needed
    if($( "#appAccordian" ).accordion( "option", "active" ) != appIndex) {
        $("#appAccordian").accordion( "activate", i );
    }
    
    // load in the tabs if needed (change of app or not yet loaded)
    if(showTab.currentAppName != appName || $("#center").tabs("length") == 0) {
        // Workaround for loading tabs selecting the last tab implicitly (and hence doing a setLocation).
        // Catch exceptions to reset setLocation.ignore in case of unexpected events
        setLocation.ignore = true;
        try { loadTabs(app.tabs); }
        catch (err) { console.log('loadTabs exception: ', err.description); }
        setLocation.ignore = false;
    }
    
    // open the appropriate tab if needed
    var $tabs = $("#center");
    if($tabs.tabs("option", "selected") != tabIndex) {
       $tabs.tabs("select", tabIndex)       
    }
    
    showTab.currentAppName = appName;
    showTab.currentTabIndex = tabIndex;
}
  
$(document).ready(function () {
  // OUTER/PAGE LAYOUT
  var pageLayout = $("body").layout({
      west__size:       250
    , east__size:       .10
    , north__size:      50
    , south__size:      30
    , south__initClosed:    false
    , north__initClosed:    false
    , east__initClosed:   true
    , west__onresize:     function () { $("#accordion-west").accordion("resize"); }
    , togglerLength_open:   50
    , togglerLength_closed: 50
    , spacing_open:     0
    ,   spacing_closed:     0
  });

  pageLayout.sizeContent("east"); // resize pane-content-elements after creating tabs
  
  /**
   * Wire up the ui component that represents the tabs control
   * Hooks up the tab panel decoration code to run when tabs are loaded from remote
   */
  $("#center").tabs({

    load: function(event, ui) {
      var newTab = ui.panel;
      window.loadScripts(function(){
        // set up callback chain to makes ure libs are loaded before decorate
        decorateTab(newTab);
        sendBeaconEvent(newTab, "tabLoad", {});
      });
     },

     remove: function(event, ui) {
       var currentTab = ui.panel;
       sendBeaconEvent(currentTab, "tabRemove", {});
      },

     select: function(event, ui) {
        var loc = parseLocation();
        setLocation(loc.appName, ui.index);
     }
  });
  
  // Wire up forward and back buttons
  // (see http://stackoverflow.com/questions/813601/jquery-ui-tabs-back-button-history)
  $.address.change(handleLocation);
      
  //Retrieve the app list from the server
  var APPLICATIONS_LIST_URI = "wm.do";

  $.get(APPLICATIONS_LIST_URI, function(appList, status) {
    // for now, just shove the appList in to a global variable
    window.appList = appList;

    // Init the left panel nav (copy in values and do a little html munging)
    for(var i = 0; i < appList.length; i++) {
      $("#appAccordianEntryTemplate").values(appList[i], {onlyNest:true});
      $("#appAccordian").append($("#appAccordianEntryTemplate").clone().children()); //work around to make accordian plugin line up with values plug-in
    }
    $("#appAccordian").accordion({ });

    // Wire the accordian change in the left panel nav to changing up the tabs in the center panel
    $("#appAccordian").bind( "accordionchange", function(event, ui) {
      var name = ui.newHeader.find("a").html(); //crumby way to fish the app name back out
      var loc = parseLocation();
      if (loc.appName != name) {
        setLocation(name);
      }

    });
    
    //Wire up for the first time that the window is loaded (note that this has to occur after the app list is loaded)
    handleLocation();
    });

});

