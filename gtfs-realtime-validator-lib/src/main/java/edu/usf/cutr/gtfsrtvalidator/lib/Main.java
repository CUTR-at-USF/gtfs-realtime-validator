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

package edu.usf.cutr.gtfsrtvalidator.lib;

import edu.usf.cutr.gtfsrtvalidator.lib.batch.BatchProcessor;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.IterationStatistics;
import org.apache.commons.cli.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Main {
    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(Main.class);

    private final static String GTFS_PATH_AND_FILE = "gtfs";
    private final static String GTFS_RT_PATH = "gtfsRealtimePath";
    private final static String SORT_OPTION = "sort";
    private final static String SORT_OPTION_NAME = "name";
    private final static String SORT_OPTION_DATE = "date";
    private final static String PLAIN_TEXT = "plainText";
    private final static String RETURN_STATS = "stats";
    private final static String IGNORE_SHAPES = "ignoreShapes";

    public static void main(String[] args) throws InterruptedException, ParseException {
        // Parse command line parameters
        Options options = setupCommandLineOptions();

        // Process archived files and then terminate
        String gtfs = getGtfsPathAndFileFromArgs(options, args);
        String gtfsRealtime = getGtfsRealtimePath(options, args);
        if (gtfs == null || gtfsRealtime == null) {
            throw new IllegalArgumentException("For batch mode you must provide a path and file name to GTFS data (e.g., -gtfs /dir/gtfs.zip) and path to directory of all archived GTFS-rt files (e.g., -gtfsRealtimePath /dir/gtfsarchive)");
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
    }

    /**
     * Sets up the command-line options that this application supports
     */
    private static Options setupCommandLineOptions() {
        Options options = new Options();
        Option gtfsOption = Option.builder(GTFS_PATH_AND_FILE)
                .hasArg()
                .desc("The full path (including zip file name) of the GTFS data")
                .build();
        Option gtfsRealtimeOption = Option.builder(GTFS_RT_PATH)
                .hasArg()
                .desc("The full path to the directory containing the archived GTFS Realtime files")
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
                .desc("If the validator should keep tracks of statistics for all validated GTFS Realtime files.")
                .build();
        Option ignoreShapes = Option.builder(IGNORE_SHAPES)
                .hasArg()
                .desc("If the validator should ignore the shapes.txt file of the GTFS feed.")
                .build();

        options.addOption(gtfsOption);
        options.addOption(gtfsRealtimeOption);
        options.addOption(sort);
        options.addOption(plainText);
        options.addOption(saveStats);
        options.addOption(ignoreShapes);
        return options;
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
        String sortBy;
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
