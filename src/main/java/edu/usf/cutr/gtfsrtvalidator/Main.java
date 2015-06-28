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

import edu.usf.cutr.gtfsrtvalidator.db.Database;
import edu.usf.cutr.gtfsrtvalidator.db.Datasource;
import edu.usf.cutr.gtfsrtvalidator.servlets.CountServlet;
import edu.usf.cutr.gtfsrtvalidator.servlets.GTFSDownloaderServlet;
import edu.usf.cutr.gtfsrtvalidator.servlets.RTFeedValidatorServlet;
import edu.usf.cutr.gtfsrtvalidator.servlets.TriggerBackgroundServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    static String BASE_RESOURCE = "./target/classes/webroot";

    public Main() throws SQLException, IOException, PropertyVetoException {
        Datasource ds = Datasource.getInstance();
        Connection connection = ds.getConnection();
    }

    public static void main(String[] args) throws Exception{
        new Main();

        Database.InitializeDB();

        Server server = new Server(8080);

        String[] configFiles = {"etc/jetty.xml"};
        for(String configFile : configFiles) {
            XmlConfiguration configuration = new XmlConfiguration(new File(configFile).toURI().toURL());
            configuration.configure(server);
        }

        WebAppContext context = new WebAppContext();

        context.setContextPath("/");
        context.setResourceBase(BASE_RESOURCE);

        server.setHandler(context);

        context.addServlet(RTFeedValidatorServlet.class, "/validate");
        context.addServlet(GTFSDownloaderServlet.class, "/downloadgtfs");

        context.addServlet(CountServlet.class, "/count");
        context.addServlet(CountServlet.class, "/feedInfo");
        context.addServlet(TriggerBackgroundServlet.class, "/startBackground");


        context.addServlet(DefaultServlet.class, "/");

        server.start();
        server.join();
    }
}
