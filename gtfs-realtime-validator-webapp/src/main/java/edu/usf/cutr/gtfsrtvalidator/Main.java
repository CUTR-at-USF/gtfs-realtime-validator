/*
 * Copyright (C) 2015-2017 Nipuna Gunathilake, University of South Florida
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.usf.cutr.gtfsrtvalidator;

import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.hibernate.HibernateUtil;
import edu.usf.cutr.gtfsrtvalidator.servlets.GetFeedJSON;
import edu.usf.cutr.gtfsrtvalidator.util.FileUtil;
import org.apache.commons.cli.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.LoggerFactory;

import java.io.File;

import static edu.usf.cutr.gtfsrtvalidator.util.FileUtil.GTFS_VALIDATOR_OUTPUT_FILE_PATH;

public class Main {
    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(Main.class);

    static String BASE_RESOURCE = Main.class.getResource("/webroot").toExternalForm();

    private final static String PORT_NUMBER_OPTION = "port";

    public static void main(String[] args) throws InterruptedException, ParseException {
        // Parse command line parameters
        Options options = setupCommandLineOptions();

        // Start validator in normal server mode
        int port = getPortFromArgs(options, args);
        HibernateUtil.configureSessionFactory();
        GTFSDB.initializeDB();

        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        /*
         * Create '/classes/webroot' directory if not exists in the same directory where jar is located.
         * '/jar-location-directory/classes/webroot' is where we store static GTFS feed validation json output.
         * 'classes/webroot' is created so that it will be in sync with or without build directories.
         */
        String gtfsValidationOutputDir = FileUtil.getJarLocation(server).getParentFile() + File.separator + GTFS_VALIDATOR_OUTPUT_FILE_PATH;
        File gtfsValidationOutputFile = new File(gtfsValidationOutputDir);
        gtfsValidationOutputFile.mkdirs();

        /*
         * As we cannot directly add static GTFS feed json output file to jar, we add an other web resource directory 'GTFS_VALIDATOR_OUTPUT_FILE_PATH'
         *  such that json output file can also be accessed from server.
         * Now there are two paths for web resources; 'BASE_RESOURCE' and 'GTFS_VALIDATOR_OUTPUT_FILE_PATH'.
         * 'GTFS_VALIDATOR_OUTPUT_FILE_PATH' as web resource directory is needed when we don't have any build folders. For example, see issue #181
         *  where we only have Travis generated jar file without any build directories.
         */
        ResourceCollection resources = new ResourceCollection(new String[] {
                BASE_RESOURCE,
                gtfsValidationOutputDir,
        });
        context.setBaseResource(resources);

        server.setHandler(context);

        context.addServlet(GetFeedJSON.class, "/getFeed");
        context.addServlet(DefaultServlet.class, "/");

        ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/api/*");
        jerseyServlet.setInitOrder(1);
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", "org.glassfish.jersey.moxy.json.MoxyJsonFeature");
        jerseyServlet.setInitParameter("jersey.config.server.provider.packages", "edu.usf.cutr.gtfsrtvalidator.api.resource");

        try {
            server.start();
            _log.info("Go to http://localhost:" + port + " in your browser");
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the command-line options that this application supports
     */
    private static Options setupCommandLineOptions() {
        Options options = new Options();
        Option portOption = Option.builder(PORT_NUMBER_OPTION)
                .hasArg()
                .desc("Port number the server should run on")
                .build();
        options.addOption(portOption);
        return options;
    }

    /**
     * Returns the port to use from command line arguments, or 8080 if no args are provided
     *
     * @param options command line options that this application supports
     * @param args
     * @return the port to use from command line arguments, or 8080 if no args are provided
     */
    private static int getPortFromArgs(Options options, String[] args) throws ParseException {
        int port = 8080;
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption(PORT_NUMBER_OPTION)) {
            port = Integer.valueOf(cmd.getOptionValue(PORT_NUMBER_OPTION));
        }
        return port;
    }
}
