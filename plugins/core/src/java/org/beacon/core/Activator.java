package org.beacon.core;

import org.beacon.core.internal.Controller;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
  protected ServiceRegistration beaconProviderService;

  @Override
  public void start(BundleContext context) throws Exception {
    Controller cc = new Controller(6633);
    cc.run();
    beaconProviderService = context.registerService(
        "org.beacon.core.IBeaconProvider", cc, null);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    if (beaconProviderService != null)
      beaconProviderService.unregister();
  }
}
