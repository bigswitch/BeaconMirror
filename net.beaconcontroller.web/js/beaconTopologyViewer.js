/**
 * This is a light-weight wrapper around the jit library (thejit.org)'s
 * implementation of a force-directed graph.  (Requires jit.js or jit-yc.js to be loaded.)
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


function constructFD(topology, divID){
  
	// init ForceDirected
  var fd = new $jit.ForceDirected({
    // id of the visualization container
    injectInto: divID,
    // Enable zooming and panning
    // with scrolling and DnD
    Navigation: {
      enable: true,
      // Enable panning events only if we're dragging the empty
      // canvas (and not a node).
      panning: 'avoid nodes',
      zooming: 10 // zoom speed. higher is more sensible
    },
    // Change node and edge styles such as
    // color and width.
    // These properties are also set per node
    // with dollar prefixed data-properties in the
    // JSON structure.
    Node: {
      overridable: true,
      dim: 7
    },
    Edge: {
      overridable: true,
      color: '#F70C28',
      lineWidth: 0.4
    },
    // Add node events
    Events: {
      enable: true,
      type: 'Native',
      // Change cursor style when hovering a node
      onMouseEnter: function() {
        fd.canvas.getElement().style.cursor = 'move';
      },
      onMouseLeave: function() {
        fd.canvas.getElement().style.cursor = '';
      },
      // Update node positions when dragged
      onDragMove: function(node, eventInfo, e) {
        var pos = eventInfo.getPos();
        node.pos.setc(pos.x, pos.y);
        fd.plot();
      },
      // Implement the same handler for touchscreens
      onTouchMove: function(node, eventInfo, e) {
        $jit.util.event.stop(e); // stop default touchmove event
        this.onDragMove(node, eventInfo, e);
      },
      onDragEnd: function(node, eventInfo, e) {
    	  var pos = eventInfo.getPos();
    	  var dpid = node.id;
    	  node.setData("saved-x", pos.x);
    	  node.setData("saved-y", pos.y);
    	  bsnTopologyViewer.save(dpid);
    	  // TODO - post this info up to the server
    	  
    	  console.log("TODO - saving off position for dpid " + dpid + " now at position " + pos.x + ", " + pos.y);
      }
    },
    // Number of iterations for the FD algorithm
    iterations: 200,
    // Edge length
    levelDistance: 130,
    // This method is only triggered
    // on label creation and only for DOM labels (not native canvas ones).
    onCreateLabel: function(domElement, node){
      // Create a 'name' and 'close' buttons and add them
      // to the main node label
      var nameContainer = document.createElement('span'),
          closeButton = document.createElement('span'),
          style = nameContainer.style;
      nameContainer.className = 'name';
      nameContainer.innerHTML = node.name;
      closeButton.className = 'close';
      closeButton.innerHTML = 'x';
      domElement.appendChild(nameContainer);
      domElement.appendChild(closeButton);
      style.fontSize = "0.8em";
      style.color = "#ddd";
      // Fade the node and its connections when
      // clicking the close button
      closeButton.onclick = function() {
        node.setData('alpha', 0, 'end');
        node.eachAdjacency(function(adj) {
          adj.setData('alpha', 0, 'end');
        });
        fd.fx.animate({
          modes: ['node-property:alpha',
                  'edge-property:alpha'],
          duration: 500
        });
      };
      // Toggle a node selection when clicking
      // its name. This is done by animating some
      // node styles like its dimension and the color
      // and lineWidth of its adjacencies.
      nameContainer.onclick = function() {
        // set final styles
        fd.graph.eachNode(function(n) {
          if(n.id != node.id) delete n.selected;
          n.setData('dim', 7, 'end');
          n.eachAdjacency(function(adj) {
            adj.setDataset('end', {
              lineWidth: 0.4,
              color: '#F70C28'
            });
          });
        });
        if(!node.selected) {
          node.selected = true;
          node.setData('dim', 17, 'end');
          node.eachAdjacency(function(adj) {
            adj.setDataset('end', {
              lineWidth: 3,
              color: '#36acfb'
            });
          });
        } else {
          delete node.selected;
        }
        // trigger animation to final styles
        fd.fx.animate({
          modes: ['node-property:dim',
                  'edge-property:lineWidth:color'],
          duration: 500
        });
        // Build the right column relations list.
        // This is done by traversing the clicked node connections.
        var html = "<h4>" + node.name + "</h4><b> connections:</b><ul><li>",
            list = [];
        node.eachAdjacency(function(adj){
          if(adj.getData('alpha')) list.push(adj.nodeTo.name);
        });
        // append connections information
        // $jit.id('inner-details').innerHTML = html + list.join("</li><li>") +
		// "</li></ul>";
      };
    },
    // Change node styles when DOM labels are placed
    // or moved.
    onPlaceLabel: function(domElement, node){
      var style = domElement.style;
      var left = parseInt(style.left);
      var top = parseInt(style.top);
      var w = domElement.offsetWidth;
      style.left = (left - w / 2) + 'px';
      style.top = (top + 10) + 'px';
      style.display = '';
    }
  });
  // load JSON data.
  fd.loadJSON(topology);
  // compute positions incrementally and animate.
  fd.computeIncremental({
    iter: 40,
    property: 'end',
    onStep: function(perc){
      console.log(perc + '% loaded...');
    },
    onComplete: function(){
      // Overwrite any computed positions with any saved positions
      for (var i in fd.graph.nodes) {
    	  var nd = fd.graph.nodes[i];
    	  if(nd.getData("saved-x") && nd.getData("saved-y")) {
    		  var x = nd.getData("saved-x");
    		  var y = nd.getData("saved-y");
    		  nd.setPos(new $jit.Complex(x, y), "end");
    	  }
      }
      Log.write('done');
      fd.animate({
        modes: ['linear'],
        transition: $jit.Trans.Elastic.easeOut,
        duration: 2500
      });
    }
  });
  return fd;
};


var beaconTopologyViewer = {
  fd : {},
  
  init: function(topology, divID) {
	  fd = constructFD(topology, divID);
  },
  
  getSwitches: function() {
	  return fd.graph.nodes;
  },
  
  getSwitch: function(switchId) {
	  return fd.graph.nodes[switchId];
  },
  
  /**
   * Persists whatever data has been attached to this node to the back-end (see
   * node.setData(), node.getData())
   */
  save: function(switchId) {
    var sw = this.getSwitch(switchId);
    // Dependent on a very specific version of the topology app - not yet
    // checked in
    // $.post('/wm/topology/topoview/switch/' + switchId + '/switchView',
    // sw.data);
  },
  
};

/**
 * Initializes the topology viewer. (Note this probably should go elsewhere - this is just
 * test code for now.)
 */
$.get('/wm/topology/topoview/links/json', function(topology, status) {
    beaconTopologyViewer.init(topology, 'topologyCanvas');
});

