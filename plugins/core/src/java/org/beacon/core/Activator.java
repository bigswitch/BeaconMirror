package org.beacon.core;

import org.beacon.core.internal.Controller;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
  protected ServiceRegistration beaconProviderService;
  protected Controller controller;

  @Override
  public void start(BundleContext context) throws Exception {
    controller = new Controller();
    controller.startup();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
      controller.shutdown();
  }
}
