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

import edu.usf.cutr.gtfsrtvalidator.batch.BatchProcessor;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.helper.GetFile;
import edu.usf.cutr.gtfsrtvalidator.hibernate.HibernateUtil;
import edu.usf.cutr.gtfsrtvalidator.servlets.GetFeedJSON;
import edu.usf.cutr.gtfsrtvalidator.validation.IterationStatistics;
import org.apache.commons.cli.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Main {
    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(Main.class);

    static String BASE_RESOURCE = Main.class.getResource("/webroot").toExternalForm();
    static String jsonFilePath = new GetFile().getJarLocation().getParentFile() + "/classes" + File.separator + "/webroot";
    private final static String PORT_NUMBER_OPTION = "port";
    private final static String BATCH_OPTION = "batch";
    private final static String GTFS_PATH_AND_FILE = "gtfs";
    private final static String GTFS_RT_PATH = "gtfsrealtimepath";
    private final static String SORT_OPTION = "sort";
    private final static String SORT_OPTION_NAME = "name";
    private final static String SORT_OPTION_DATE = "date";
    private final static String PLAIN_TEXT = "plaintext";
    private final static String RETURN_STATS = "stats";
    private final static String IGNORE_SHAPES = "ignoreshapes";

    public static void main(String[] args) throws InterruptedException, ParseException {
        // Parse command line parameters
        Options options = setupCommandLineOptions();

        boolean batchMode = getBatchFromArgs(options, args);
        if (batchMode) {
            // Process archived files and then terminate
            String gtfs = getGtfsPathAndFileFromArgs(options, args);
            String gtfsRealtime = getGtfsRealtimePath(options, args);
            if (gtfs == null || gtfsRealtime == null) {
                throw new IllegalArgumentException("For batch mode you must provide a path and file name to GTFS data (e.g., -gtfs /dir/gtfs.zip) and path to directory of all archived GTFS-rt files (e.g., -gtfs-realtime-path /dir/gtfsarchive)");
            }
            BatchProcessor.SortBy sortBy = getSortBy(options, args);
            String plainText = getPlainTextFileExtensionfromArgs(options, args);
            boolean returnStats = getReturnStatsFromArgs(options, args);
            boolean ignoreShapes = getIgnoreShapesFromArgs(options, args);
            BatchProcessor.Builder builder = new BatchProcessor.Builder(gtfs, gtfsRealtime)
                    .sortBy(sortBy)
                    .setPlainTextExtension(plainText)
                    .setReturnStatistics(returnStats)
                    .setIgnoreShapes(ignoreShapes);
            BatchProcessor processor = builder.build();
            try {
                List<IterationStatistics> stats = processor.processFeeds();
                if (returnStats) {
                    _log.info("-------------------------");
                    _log.info("  Validation Statistics");
                    _log.info("-------------------------");
                    for (IterationStatistics stat : stats) {
                        _log.info(stat.toString());
                    }
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                _log.error("Error running batch processor: " + e);
            }
            return;
        }

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
        File jsonDirectory = new File(jsonFilePath);
        jsonDirectory.mkdirs();

        /*
         * As we cannot directly add static GTFS feed json output file to jar, we add an other web resource directory 'jsonFilePath'
         *  such that json output file can also be accessed from server.
         * Now there are two paths for web resources; 'BASE_RESOURCE' and 'jsonFilePath'.
         * 'jsonFilePath' as web resource directory is needed when we don't have any build folders. For example, see issue #181
         *  where we only have Travis generated jar file without any build directories.
         */
        ResourceCollection resources = new ResourceCollection(new String[] {
                BASE_RESOURCE,
                jsonFilePath,
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
        Option batchOption = Option.builder(BATCH_OPTION)
                .hasArg()
                .desc("If the validator should run in batch mode on archived files")
                .build();
        Option gtfsOption = Option.builder(GTFS_PATH_AND_FILE)
                .hasArg()
                .desc("The full path (including zip file name) of the GTFS data")
                .build();
        Option gtfsRealtimeOption = Option.builder(GTFS_RT_PATH)
                .hasArg()
                .desc("The full path to the directory containing the archived GTFS-realtime files")
                .build();
        Option sort = Option.builder(SORT_OPTION)
                .hasArg()
                .desc("'name' if the GTFS-rt files should be chronologically sorted based on file name, or 'date' if the file last modified date should be used to sort the files")
                .build();
        Option plainText = Option.builder(PLAIN_TEXT)
                .hasArg()
                .desc("If the validator should write the protocol buffer files as plain text during batch processing.  Provided option will be the file extension for the plain text files")
                .build();
        Option saveStats = Option.builder(RETURN_STATS)
                .hasArg()
                .desc("If the validator should keep tracks of statistics for all validated GTFS-realtime files.")
                .build();
        Option ignoreShapes = Option.builder(IGNORE_SHAPES)
                .hasArg()
                .desc("If the validator should ignore the shapes.txt file of the GTFS feed.")
                .build();

        options.addOption(portOption);
        options.addOption(batchOption);
        options.addOption(gtfsOption);
        options.addOption(gtfsRealtimeOption);
        options.addOption(sort);
        options.addOption(plainText);
        options.addOption(saveStats);
        options.addOption(ignoreShapes);

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

    /**
     * Returns true if the "-batch" parameter is included, false it if is not
     *
     * @param options command line options that this application supports
     * @param args
     * @return true if the "-batch" parameter is included, false it if is not
     */
    private static boolean getBatchFromArgs(Options options, String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        return cmd.hasOption(BATCH_OPTION);
    }

    /**
     * Returns the path of the GTFS zip file (including zip file name) if provided by the user, or null if no path was provided
     *
     * @param options parsed command line options
     * @param args
     * @return the path of the GTFS zip file (including zip file name) if provided by the user, or null if no path was provided
     */
    private static String getGtfsPathAndFileFromArgs(Options options, String[] args) throws ParseException {
        String gtfsPath = null;
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption(GTFS_PATH_AND_FILE)) {
            gtfsPath = cmd.getOptionValue(GTFS_PATH_AND_FILE);
        }
        return gtfsPath;
    }

    /**
     * Returns the path to the archived GTFS-rt files if provided by the user, or null if no path was provided
     *
     * @param options command line options that this application supports
     * @param args
     * @return the path to the archived GTFS-rt files if provided by the user, or null if no path was provided
     */
    private static String getGtfsRealtimePath(Options options, String[] args) throws ParseException {
        String gtfsRealtimePath = null;
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption(GTFS_RT_PATH)) {
            gtfsRealtimePath = cmd.getOptionValue(GTFS_RT_PATH);
        }
        return gtfsRealtimePath;
    }

    /**
     * Returns BatchProcessor.SortBy.DATE_MODIFIED or BatchProcessor.SortBy.NAME based on the command line parameter provided ("-sort date" or "-sort name")
     * by the user, or BatchProcessor.SortBy.DATE_MODIFIED if the user didn't supply a command line parameter or the provided parameter wasn't recognized
     *
     * @param options command line options that this application supports
     * @param args
     * @return BatchProcessor.SortBy.DATE_MODIFIED or BatchProcessor.SortBy.NAME based on the command line parameter provided ("-sort date" or "-sort name")
     * by the user, or BatchProcessor.SortBy.DATE_MODIFIED if the user didn't supply a command line parameter or the provided parameter wasn't recognized
     */
    private static BatchProcessor.SortBy getSortBy(Options options, String[] args) throws ParseException {
        String sortBy = null;
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption(SORT_OPTION)) {
            sortBy = cmd.getOptionValue(SORT_OPTION);
            switch (sortBy) {
                case SORT_OPTION_NAME:
                    return BatchProcessor.SortBy.NAME;
                case SORT_OPTION_DATE:
                    return BatchProcessor.SortBy.DATE_MODIFIED;
                default:
                    // User provided an unsupported parameter
                    return BatchProcessor.SortBy.DATE_MODIFIED;
            }
        } else {
            // No parameter provided by user
            return BatchProcessor.SortBy.DATE_MODIFIED;
        }
    }

    /**
     * Returns the extension that should be used when outputting a plain text version of the file (if provided by the user), or null if a plain text version shouldn't be output (option wasn't provided on command line)
     *
     * @param options parsed command line options
     * @param args
     * @return the extension that should be used when outputting a plain text version of the file (if provided by the user), or null if a plain text version shouldn't be output (option wasn't provided on command line)
     */
    private static String getPlainTextFileExtensionfromArgs(Options options, String[] args) throws ParseException {
        String plainText = null;
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption(PLAIN_TEXT)) {
            plainText = cmd.getOptionValue(PLAIN_TEXT);
        }
        return plainText;
    }

    /**
     * Returns true if the "-stats" parameter is included, false it if is not
     *
     * @param options command line options that this application supports
     * @param args
     * @return true if the "-stats" parameter is included, false it if is not
     */
    private static boolean getReturnStatsFromArgs(Options options, String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        return cmd.hasOption(RETURN_STATS);
    }

    /**
     * Returns true if the "-ignoreshapes" parameter is included, false it if is not
     *
     * @param options command line options that this application supports
     * @param args
     * @return true if the "-ignoreshapes" parameter is included, false it if is not
     */
    private static boolean getIgnoreShapesFromArgs(Options options, String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        return cmd.hasOption(IGNORE_SHAPES);
    }
}
