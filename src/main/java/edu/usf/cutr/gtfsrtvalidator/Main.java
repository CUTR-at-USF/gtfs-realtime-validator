package edu.usf.cutr.gtfsrtvalidator;

import edu.usf.cutr.gtfsrtvalidator.background.BackgroundTask;
import edu.usf.cutr.gtfsrtvalidator.db.Database;
import edu.usf.cutr.gtfsrtvalidator.servlets.CountServlet;
import edu.usf.cutr.gtfsrtvalidator.servlets.GTFSDownloaderServlet;
import edu.usf.cutr.gtfsrtvalidator.servlets.RTFeedValidatorServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletContextListener;

public class Main {
    static String BASE_RESOURCE = "./target/classes/webroot";

    public static void main(String[] args) throws Exception{
        Database.InitializeDB();

        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase(BASE_RESOURCE);

        ServletContextListener myListener = new BackgroundTask();
        context.addEventListener(myListener);

        server.setHandler(context);

        context.addServlet(RTFeedValidatorServlet.class, "/validate");
        context.addServlet(GTFSDownloaderServlet.class, "/downloadgtfs");

        context.addServlet(CountServlet.class, "/count");

        context.addServlet(DefaultServlet.class, "/");

        server.start();
        server.join();
    }
}
