/*
 ***********************************************************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***********************************************************************************************************************
*/

package edu.usf.cutr.gtfsrtvalidator;

import edu.usf.cutr.gtfsrtvalidator.background.BackgroundTask;
import edu.usf.cutr.gtfsrtvalidator.db.Database;
import edu.usf.cutr.gtfsrtvalidator.servlets.*;
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
        //context.addEventListener(myListener);

        server.setHandler(context);

        context.addServlet(RTFeedValidatorServlet.class, "/validate");
        context.addServlet(GTFSDownloaderServlet.class, "/downloadgtfs");

        context.addServlet(CountServlet.class, "/count");
        context.addServlet(FeedInfoServlet.class, "/feedInfo");
        context.addServlet(TriggerBackgroundServlet.class, "/startBackground");


        context.addServlet(DefaultServlet.class, "/");

        server.start();
        server.join();
    }
}
