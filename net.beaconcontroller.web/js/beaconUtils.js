/**
 * A set of utility functions for beacon
 * 
 * (Note - requires JQuery to be loaded.)
 */

var beaconUtils = new function BeaconUtils() {
  
  /**
   * Return an object that represents an empty flow mod entry
   */
  this.constructFlowMod = function(){
    return {
      name: null,
      'switch': null,
      actions : null,
      priority: null,
      active: true,
      cookie: null,
      ingress-port: null,
      src-mac: null,
      dst-mac: null,
      vlan-id: null,
      vlan-pirority: null,
      ether-type: null,
      tos-bits: null,
      protocol: null,
      src-ip: null
      dst-ip: null,
      src-port: null,
      dst-port: null,
      idle-timeout: null,
      soft-timeout: null,
      /**
       * actionString examples: "output=2", "output=flood", "output=all"
       * A null actionString implies a 'drop'
       * Note - non-output actions not currently supported (TODO - add these in)
       */
      addAction: function(actionString) {
        if(!this.actions)
          this.actions = "";
        else
          this.actions += ",";
        this.actions += actionString;
      },
    };
    
  };
  
  this.flowModToString(flowMod) {
    return JSON.stringify([flowMod]);
  };
};