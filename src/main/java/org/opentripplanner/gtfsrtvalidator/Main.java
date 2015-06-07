package org.opentripplanner.gtfsrtvalidator;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.opentripplanner.gtfsrtvalidator.servlets.GTFSDownloaderServlet;
import org.opentripplanner.gtfsrtvalidator.servlets.RTFeedValidatorServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class Main {
    static String BASE_RESOURCE = "./target/classes/webroot";

    public static void main(String[] args) throws Exception{
        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase(BASE_RESOURCE);

        server.setHandler(context);

        context.addServlet(RTFeedValidatorServlet.class, "/validate");
        context.addServlet(GTFSDownloaderServlet.class, "/downloadgtfs");
        context.addServlet(DefaultServlet.class, "/");

        server.start();
        server.join();
    }
}
