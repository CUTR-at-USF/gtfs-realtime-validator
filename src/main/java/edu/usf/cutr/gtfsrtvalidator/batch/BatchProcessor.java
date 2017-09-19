/*
 * Copyright (C) 2017 University of South Florida
 *
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
package edu.usf.cutr.gtfsrtvalidator.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.util.SortUtils;
import edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.rules.*;
import org.apache.commons.io.IOUtils;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.getElapsedTime;
import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.getElapsedTimeString;

public class BatchProcessor {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(BatchProcessor.class);

    // Validation rules
    private final static List<FeedEntityValidator> mValidationRules = new ArrayList<>();
    public final static String RESULTS_FILE_EXTENSION = ".results.json";
    private SortBy mSortBy = SortBy.DATE_MODIFIED;
    private String mPlainTextExtension = null;

    // GTFS
    private GtfsDaoImpl mGtfsData = new GtfsDaoImpl();
    private GtfsReader mReader = new GtfsReader();
    private File mPathToGtfsFile;
    private GtfsMetadata mGtfsMetadata;

    // GTFS-realtime
    private String mPathToGtfsRealtime;

    /**
     * Creates a new BatchProcessor for the provided GTFS and GTFS-realtime data
     * <p>
     * By default GTFS-realtime files will be processed in order of last modified date (ascending)
     *
     * @param pathToGtfsFile     the path of the GTFS zip file (including zip file name)
     * @param pathToGtfsRealtime the path to the archived GTFS-rt files
     */
    protected BatchProcessor(String pathToGtfsFile, String pathToGtfsRealtime) {
        mPathToGtfsFile = new File(pathToGtfsFile);
        mPathToGtfsRealtime = pathToGtfsRealtime;
    }

    private void setSortBy(SortBy sortBy) {
        mSortBy = sortBy;
    }

    private void setPlainTextExtension(String plainTextExtension) {
        mPlainTextExtension = plainTextExtension;
    }

    public void processFeeds() throws NoSuchAlgorithmException, IOException {
        // Read GTFS data into a GtfsDaoImpl
        _log.info("Starting batch processor...");
        String timeZoneText = null;
        readGtfsData();

        Collection<Agency> agencies = mGtfsData.getAllAgencies();
        for (Agency agency : agencies) {
            timeZoneText = agency.getTimezone();
            break;
        }
        mGtfsMetadata = new GtfsMetadata(mPathToGtfsFile.getAbsolutePath(), TimeZone.getTimeZone(timeZoneText), mGtfsData);

        // Initialize validation rules
        synchronized (mValidationRules) {
            if (mValidationRules.isEmpty()) {
                mValidationRules.add(new CrossFeedDescriptorValidator());
                mValidationRules.add(new VehicleValidator());
                mValidationRules.add(new TimestampValidator());
                mValidationRules.add(new StopTimeUpdateValidator());
                mValidationRules.add(new TripDescriptorValidator());
                mValidationRules.add(new StopValidator());
                mValidationRules.add(new FrequencyTypeZeroValidator());
                mValidationRules.add(new FrequencyTypeOneValidator());
                mValidationRules.add(new HeaderValidator());
            }
        }
        // Configure output
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        _log.info("Sorting GTFS-rt files by " + mSortBy.name() + "...");

        // Read GTFS-rt protobuf files from provided directory
        List<Path> paths = Files.walk(Paths.get(mPathToGtfsRealtime))
                .filter(Files::isRegularFile)
                .sorted((o1, o2) -> {
                    if (mSortBy.equals(SortBy.DATE_MODIFIED)) {
                        try {
                            // Sort by date modified (ascending) (it seems more consistent cross-platform than "date created")
                            return SortUtils.compareByDateModified(o1, o2);
                        } catch (IOException e) {
                            _log.error("Can't sort GTFS-rt files by date - assuming dates are equal: " + e);
                        }
                        // Assume file dates are equal if we get an exception
                        return 0;
                    } else {
                        // Sort by name (ascending)
                        return SortUtils.compareByFileName(o1, o2);
                    }
                })
                .collect(Collectors.toList());

        MessageDigest md = MessageDigest.getInstance("MD5");
        GtfsRealtime.FeedMessage prevMessage = null;
        byte[] prevHash = null;

        for (Path path : paths) {
            long startTimeNanos = System.nanoTime();
            long startToByteArray = System.nanoTime();
            byte[] protobuf;
            try {
                protobuf = IOUtils.toByteArray(Files.newInputStream(path));
            } catch (IOException e) {
                _log.error("Error reading GTFS-rt file to byte array, skipping to next file: " + e);
                continue;
            }
            _log.info("Read " + path.getFileName() + " to byte array in " + getElapsedTimeString(getElapsedTime(startToByteArray, System.nanoTime())));

            byte[] currentHash = md.digest(protobuf);
            if (MessageDigest.isEqual(currentHash, prevHash)) {
                // This feed file is a duplicate of the last one - skip to next file
                continue;
            }

            long timestamp;
            if (mSortBy.equals(SortBy.DATE_MODIFIED)) {
                // Use file last modified date as "current" timestamp
                timestamp = path.toFile().lastModified();
            } else {
                // Use time parsed from file name as "current" timestamp
                try {
                    timestamp = TimestampUtils.getTimestampFromFileName(path.toFile().getName());
                } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
                    _log.error("Couldn't parse timestamp from file name '" + path.toFile().getName() + "' - using date modified instead: " + e);
                    timestamp = path.toFile().lastModified();
                }
            }

            long startProtobufDecode = System.nanoTime();
            GtfsRealtime.FeedMessage message;
            try {
                message = GtfsRealtime.FeedMessage.parseFrom(protobuf);
            } catch (InvalidProtocolBufferException e) {
                _log.error("Error reading GTFS-rt message from byte array, skipping to next file: " + e);
                continue;
            }
            _log.info("Decoded " + path.getFileName() + " protobuf in " + getElapsedTimeString(getElapsedTime(startProtobufDecode, System.nanoTime())));

            GtfsRealtime.FeedMessage combinedMessage = null;
            // See if more than one entity type exists in this feed
            if (GtfsUtils.isCombinedFeed(message)) {
                // Run CrossFeedDescriptorValidator on this message
                combinedMessage = message;
            }

            List<ErrorListHelperModel> allErrorLists = new ArrayList<>();
            StringBuilder consoleOutput = new StringBuilder();
            for (FeedEntityValidator rule : mValidationRules) {
                long startRuleNanos = System.nanoTime();
                List<ErrorListHelperModel> errorLists = rule.validate(timestamp, mGtfsData, mGtfsMetadata, message, prevMessage, combinedMessage);
                allErrorLists.addAll(errorLists);
                consoleOutput.append("\n" + rule.getClass().getSimpleName() + " - rule = " + getElapsedTimeString(getElapsedTime(startRuleNanos, System.nanoTime())));
            }
            consoleOutput.append("\nProcessed " + path.getFileName() + " in " + getElapsedTimeString(getElapsedTime(startTimeNanos, System.nanoTime())));
            consoleOutput.append("\n---------------------");
            _log.info(consoleOutput.toString());

            // Write validation results for this file to JSON
            writeResults(mapper, path, allErrorLists);

            if (mPlainTextExtension != null) {
                // Write plain text version of protocol buffer
                writePlainText(message, mapper, path);
            }

            prevHash = currentHash;
            prevMessage = message;
        }
    }

    private void readGtfsData() throws IOException {
        _log.info("Reading GTFS data from " + mPathToGtfsFile + "...");
        mGtfsData = new GtfsDaoImpl();
        mReader = new GtfsReader();
        mReader.setInputLocation(mPathToGtfsFile);
        mReader.setEntityStore(mGtfsData);
        long startGtfsRead = System.nanoTime();
        mReader.run();
        _log.info(mPathToGtfsFile.getName() + " read in " + getElapsedTimeString(getElapsedTime(startGtfsRead, System.nanoTime())));
    }

    private void writeResults(ObjectMapper mapper, Path path, List<ErrorListHelperModel> allErrorLists) throws IOException {
        mapper.writeValue(new File(path.toAbsolutePath() + RESULTS_FILE_EXTENSION), allErrorLists);
    }

    private void writePlainText(GtfsRealtime.FeedMessage message, ObjectMapper mapper, Path path) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(path.toAbsolutePath() + "." + mPlainTextExtension));
        out.write(TextFormat.printToString(message).getBytes());
        out.close();
    }

    public enum SortBy {
        DATE_MODIFIED, NAME
    }

    public static class Builder {
        private String mPathToGtfsFile, mPathToGtfsRealtime;
        private SortBy mSortBy = null;
        private String mPlainTextExtension = null;

        public Builder(String pathToGtfsFile, String pathToGtfsRealtime) {
            mPathToGtfsFile = pathToGtfsFile;
            mPathToGtfsRealtime = pathToGtfsRealtime;
        }

        /**
         * Sets the way that the GTFS-realtime files will be sorted (default = last modified date, ascending)
         *
         * @param sortBy the way that the GTFS-realtime files will be sorted
         * @return this Builder instance so methods can be chained together
         */
        public Builder sortBy(SortBy sortBy) {
            mSortBy = sortBy;
            return this;
        }

        /**
         * Sets the validator to output a plain text version of each processed file with the provided file extension.
         * For example, if the protocol buffer file has the name "file.pb", and the text "txt" is provided, then the
         * plain text version of this file will be "file.pb.txt".
         *
         * @param plainTextExtension the extension that should be used for the plain text version of all the processed feed files
         * @return this Builder instance so methods can be chained together
         */
        public Builder setPlainTextExtension(String plainTextExtension) {
            mPlainTextExtension = plainTextExtension;
            return this;
        }

        public BatchProcessor build() {
            BatchProcessor bp = new BatchProcessor(mPathToGtfsFile, mPathToGtfsRealtime);
            if (mSortBy != null) {
                bp.setSortBy(mSortBy);
            }
            if (mPlainTextExtension != null) {
                bp.setPlainTextExtension(mPlainTextExtension);
            }
            return bp;
        }
    }
}
