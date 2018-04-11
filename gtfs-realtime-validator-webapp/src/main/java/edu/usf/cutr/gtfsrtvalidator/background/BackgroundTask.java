/*
 * Copyright (C) 2017 Nipuna Gunathilake, University of South Florida
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

package edu.usf.cutr.gtfsrtvalidator.background;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.resource.GtfsFeed;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.helper.DBHelper;
import edu.usf.cutr.gtfsrtvalidator.lib.model.GtfsRtFeedIterationModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.GtfsRtFeedModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.interfaces.FeedEntityValidator;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.rules.*;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils.getElapsedTime;
import static edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils.getElapsedTimeString;

public class BackgroundTask implements Runnable {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(BackgroundTask.class);

    private static Map<Integer, GtfsRealtime.FeedMessage> mGtfsRtFeedMap = new ConcurrentHashMap<>();
    private static Map<Integer, GtfsMetadata> mGtfsMetadata = new ConcurrentHashMap<>();
    private final static List<FeedEntityValidator> mValidationRules = new ArrayList<>();

    private GtfsRtFeedModel mCurrentGtfsRtFeed = null;

    public BackgroundTask(GtfsRtFeedModel gtfsRtFeed) {
        // Accept the gtfs feed id and save entities of the same feed in an array
        mCurrentGtfsRtFeed = gtfsRtFeed;

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
    }

    @Override
    public void run() {
        try {
            long startTimeNanos = System.nanoTime();
            GtfsRealtime.FeedMessage currentFeedMessage;
            GtfsRealtime.FeedMessage previousFeedMessage = null;
            GtfsMutableDao gtfsData;
            GtfsMetadata gtfsMetadata;
            // Holds data needed in the database under each iteration
            GtfsRtFeedIterationModel feedIteration;
            StringBuffer consoleOutput = new StringBuffer();
            
            // Get the GTFS feed from the GtfsDaoMap using the gtfsFeedId of the current feed.
            gtfsData = GtfsFeed.GtfsDaoMap.get(mCurrentGtfsRtFeed.getGtfsFeedModel().getFeedId());
            // Create the GTFS metadata if it doesn't already exist
            // TODO - read ignoreShapes from website checkbox - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/286
            gtfsMetadata = mGtfsMetadata.computeIfAbsent(mCurrentGtfsRtFeed.getGtfsFeedModel().getFeedId(),
                    k -> new GtfsMetadata(mCurrentGtfsRtFeed.getGtfsFeedModel().getGtfsUrl(),
                            TimeZone.getTimeZone(mCurrentGtfsRtFeed.getGtfsFeedModel().getAgency()),
                            gtfsData, mCurrentGtfsRtFeed.getEnableShapes()));

            // Read the GTFS-rt feed from the feed URL
            URL gtfsRtFeedUrl;
            Session session;
            try {
                gtfsRtFeedUrl = new URL(mCurrentGtfsRtFeed.getGtfsRtUrl());
            } catch (MalformedURLException e) {
                _log.error("Malformed Url: " + mCurrentGtfsRtFeed.getGtfsRtUrl(), e);
                e.printStackTrace();
                return;
            }

            try {
                // Get the GTFS-RT feedMessage for this method
                long startHttpRequest = System.nanoTime();
                InputStream in = gtfsRtFeedUrl.openStream();
                consoleOutput.append("\n" + mCurrentGtfsRtFeed.getGtfsRtUrl() + " gtfsRtFeedUrl.openStream() in " + getElapsedTimeString(getElapsedTime(startHttpRequest, System.nanoTime())));
                long startToByteArray = System.nanoTime();
                byte[] gtfsRtProtobuf = IOUtils.toByteArray(in);
                consoleOutput.append("\n" + mCurrentGtfsRtFeed.getGtfsRtUrl() + " IOUtils.toByteArray(in) in " + getElapsedTimeString(getElapsedTime(startToByteArray, System.nanoTime())));

                boolean isUniqueFeed = true;
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] prevFeedDigest = null;
                byte[] currentFeedDigest = md.digest(gtfsRtProtobuf);

                session = GTFSDB.initSessionBeginTrans();
                feedIteration = (GtfsRtFeedIterationModel) session.createQuery("FROM GtfsRtFeedIterationModel"
                        + " WHERE rtFeedId = :gtfsRtId"
                        + " ORDER BY IterationId DESC")
                        .setParameter("gtfsRtId", mCurrentGtfsRtFeed.getGtfsRtId())
                        .setMaxResults(1)
                        .uniqueResult();
                if (feedIteration != null) {
                    prevFeedDigest = feedIteration.getFeedHash();
                }

                if(MessageDigest.isEqual(currentFeedDigest, prevFeedDigest)) {
                    // If previous feed digest and newly fetched/current feed digest are equal means, we received the same feed again.
                    isUniqueFeed = false;
                }

                long startProtobufDecode = System.nanoTime();
                currentFeedMessage = GtfsRealtime.FeedMessage.parseFrom(gtfsRtProtobuf);
                consoleOutput.append("\n" + mCurrentGtfsRtFeed.getGtfsRtUrl() + " protobuf decode in " + getElapsedTimeString(getElapsedTime(startProtobufDecode, System.nanoTime())));
                _log.info(consoleOutput.toString());
                consoleOutput.setLength(0);  // Clear the buffer for the next set of log statements

                long feedTimestamp = TimeUnit.SECONDS.toMillis(currentFeedMessage.getHeader().getTimestamp());

                // Create new feedIteration object and save the iteration to the database
                if(isUniqueFeed) {
                    if (feedIteration != null && feedIteration.getFeedprotobuf() != null) {
                        // Get the previous feed message
                        InputStream previousIs = new ByteArrayInputStream(feedIteration.getFeedprotobuf());
                        previousFeedMessage = GtfsRealtime.FeedMessage.parseFrom(previousIs);
                    }

                    feedIteration = new GtfsRtFeedIterationModel(System.currentTimeMillis(), feedTimestamp, gtfsRtProtobuf, mCurrentGtfsRtFeed, currentFeedDigest);
                } else {
                    feedIteration = new GtfsRtFeedIterationModel(System.currentTimeMillis(), feedTimestamp, null, mCurrentGtfsRtFeed, currentFeedDigest);
                }
                session.save(feedIteration);
                GTFSDB.commitAndCloseSession(session);

                if (!isUniqueFeed) {
                    return;
                }
            } catch (Exception e) {
                _log.error("The URL '" + gtfsRtFeedUrl + "' does not contain valid Gtfs-Rt data", e);
                return;
            }

            // Read all GTFS-rt entities for the current feed
            mGtfsRtFeedMap.put(feedIteration.getGtfsRtFeedModel().getGtfsRtId(), currentFeedMessage);

            session = GTFSDB.initSessionBeginTrans();

            List<GtfsRealtime.FeedEntity> allEntitiesArrayList = new ArrayList<>();

            List<GtfsRtFeedModel> gtfsRtFeedModelList;
            gtfsRtFeedModelList = session.createQuery("FROM GtfsRtFeedModel"
                    + " WHERE gtfsFeedID = :feedID")
                    .setParameter("feedID", mCurrentGtfsRtFeed.getGtfsFeedModel().getFeedId())
                    .list();

            GTFSDB.closeSession(session);

            while (!mGtfsRtFeedMap.keySet().containsAll(gtfsRtFeedModelList.stream().map(GtfsRtFeedModel::getGtfsRtId).collect(Collectors.toSet()))) {
                Thread.sleep(200);
            }
            GtfsRealtime.FeedHeader header = null;

            if (gtfsRtFeedModelList.size() < 1) {
                _log.error("The URL '" + gtfsRtFeedUrl + "' is not stored properly into the database");
                return;
            }

            GtfsRealtime.FeedMessage combinedFeed = null;

            if (gtfsRtFeedModelList.size() == 1) {
                // See if more than one entity type exists in this feed
                GtfsRealtime.FeedMessage message = mGtfsRtFeedMap.get(gtfsRtFeedModelList.get(0).getGtfsRtId());
                if (GtfsUtils.isCombinedFeed(message)) {
                    // Run CrossFeedDescriptorValidator on this message
                    combinedFeed = message;
                }
            }

            if (gtfsRtFeedModelList.size() > 1) {
                // We're monitoring multiple GTFS-rt feeds for the same GTFS data - create a combined feed message include all entities for all of those GTFS-rt feeds
                _log.debug("Creating combined feed message for " + gtfsRtFeedModelList.toString());
                for (GtfsRtFeedModel gtfsRtFeedModel : gtfsRtFeedModelList) {
                    GtfsRealtime.FeedMessage message = mGtfsRtFeedMap.get(gtfsRtFeedModel.getGtfsRtId());
                    if (header == null) {
                        // Save one header to use in our combined feed below
                        header = message.getHeader();
                    } else {
                        if (message.getHeader() != null && message.getHeader().getTimestamp() > header.getTimestamp()) {
                            // Use largest header timestamp with multiple feeds - see #239
                            header = message.getHeader();
                        }
                    }
                    if (message != null) {
                        allEntitiesArrayList.addAll(message.getEntityList());
                    }
                }

                GtfsRealtime.FeedMessage.Builder feedMessageBuilder = GtfsRealtime.FeedMessage.newBuilder();
                feedMessageBuilder.setHeader(header);
                feedMessageBuilder.addAllEntity(allEntitiesArrayList);
                combinedFeed = feedMessageBuilder.build();
            }

            // Use the same current time for all rules for consistency
            long currentTimeMillis = System.currentTimeMillis();
            // Run validation rules
            for (FeedEntityValidator rule : mValidationRules) {
                consoleOutput.append(validateEntity(currentTimeMillis, currentFeedMessage, previousFeedMessage, combinedFeed, gtfsData, gtfsMetadata, feedIteration, rule));
            }
            consoleOutput.append("\nProcessed " + mCurrentGtfsRtFeed.getGtfsRtUrl() + " in " + getElapsedTimeString(getElapsedTime(startTimeNanos, System.nanoTime())));
            consoleOutput.append("\n---------------------");
            _log.info(consoleOutput.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private StringBuffer validateEntity(long currentTimeMillis, GtfsRealtime.FeedMessage currentFeedMessage, GtfsRealtime.FeedMessage previousFeedMessage,
                                        GtfsRealtime.FeedMessage combinedFeedMessage, GtfsMutableDao gtfsData, GtfsMetadata gtfsMetadata,
                                        GtfsRtFeedIterationModel feedIteration, FeedEntityValidator feedEntityValidator) {
        StringBuffer consoleLine = new StringBuffer();
        long startTimeNanos = System.nanoTime();
        List<ErrorListHelperModel> errorLists = feedEntityValidator.validate(currentTimeMillis, gtfsData, gtfsMetadata, currentFeedMessage, previousFeedMessage, combinedFeedMessage);
        consoleLine.append("\n" + feedEntityValidator.getClass().getSimpleName() + " - rule = " + getElapsedTimeString(getElapsedTime(startTimeNanos, System.nanoTime())));
        if (errorLists != null) {
            startTimeNanos = System.nanoTime();
            for (ErrorListHelperModel errorList : errorLists) {
                if (!errorList.getOccurrenceList().isEmpty()) {
                    //Set iteration Id
                    errorList.getErrorMessage().setGtfsRtFeedIterationModel(feedIteration);
                    //Save the captured errors to the database
                    DBHelper.saveError(errorList);
                }
            }
            consoleLine.append(", database = " + getElapsedTimeString(getElapsedTime(startTimeNanos, System.nanoTime())));
        }
        return consoleLine;
    }
}
