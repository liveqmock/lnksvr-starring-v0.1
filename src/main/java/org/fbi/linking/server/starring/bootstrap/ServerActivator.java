package org.fbi.linking.server.starring.bootstrap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerActivator implements BundleActivator {

    private static BundleContext context;
    private ServerService serverService;

    public static BundleContext getBundleContext() {
        return context;
    }

    public void start(BundleContext context) {
        ServerActivator.context = context;
        //try {
            this.serverService = new ServerService(getBundleContext());
            this.serverService.start();
            System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " - Starting the linking server bundle....");
        //} catch (Exception e) {
        //    e.printStackTrace();
        //}
    }

    public void stop(BundleContext context) throws Exception {
        this.serverService.stop();
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " - Stopping the linking server bundle...");
    }

}
