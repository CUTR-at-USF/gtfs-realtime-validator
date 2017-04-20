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
import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsRtFeedIterationModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsRtFeedModel;
import edu.usf.cutr.gtfsrtvalidator.api.resource.GtfsFeed;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.helper.DBHelper;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.helper.TimeStampHelper;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.StopTimeSequenceValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.StopValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.VehicleIdValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.CheckRouteAndTripIds;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.FrequencyTypeZero;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.TimestampValidation;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.VehicleTripDescriptorValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BackgroundTask implements Runnable {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(BackgroundTask.class);

    private static Map<Integer, Map<Integer, GtfsRealtime.FeedMessage>> mFeedEntityList = new ConcurrentHashMap<>();
    private static Map<Integer, GtfsRealtime.FeedMessage> mGtfsRtFeedMap = new ConcurrentHashMap<>();
    private static Map<Integer, edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata> mGtfsMetadata = new ConcurrentHashMap<>();
    private final static List<FeedEntityValidator> mValidationRules = new ArrayList<>();

    private GtfsRtFeedModel mCurrentGtfsRtFeed = null;

    public BackgroundTask(GtfsRtFeedModel gtfsRtFeed) {
        // Accept the gtfs feed id and save entities of the same feed in an array
        mCurrentGtfsRtFeed = gtfsRtFeed;

        // Initialize validation rules
        synchronized (mValidationRules) {
            if (mValidationRules.isEmpty()) {
                mValidationRules.add(new VehicleTripDescriptorValidator()); // W001, E001, E012
                mValidationRules.add(new VehicleIdValidator()); // W002
                mValidationRules.add(new TimestampValidation()); // W003
                mValidationRules.add(new StopTimeSequenceValidator()); // E002
                mValidationRules.add(new CheckRouteAndTripIds()); // E003, E004
                mValidationRules.add(new StopValidator()); // E011
                mValidationRules.add(new FrequencyTypeZero()); // E013
            }
        }
    }

    @Override
    public void run() {
        try {
            long startTimeNanos = System.nanoTime();
            GtfsRealtime.FeedMessage feedMessage;
            GtfsDaoImpl gtfsData;
            GtfsMetadata gtfsMetadata;

            // Holds data needed in the database under each iteration
            GtfsRtFeedIterationModel feedIteration;

            // Get the GTFS feed from the GtfsDaoMap using the gtfsFeedId of the current feed.
            gtfsData = GtfsFeed.GtfsDaoMap.get(mCurrentGtfsRtFeed.getGtfsFeedModel().getFeedId());
            // Create the GTFS metadata if it doesn't already exist
            gtfsMetadata = mGtfsMetadata.computeIfAbsent(mCurrentGtfsRtFeed.getGtfsFeedModel().getFeedId(),
                    k -> new GtfsMetadata(gtfsData));

            // Read the GTFS-rt feed from the feed URL
            URL gtfsRtFeedUrl;
            try {
                gtfsRtFeedUrl = new URL(mCurrentGtfsRtFeed.getGtfsUrl());
            } catch (MalformedURLException e) {
                _log.error("Malformed Url: " + mCurrentGtfsRtFeed.getGtfsUrl(), e);
                e.printStackTrace();
                return;
            }

            try {
                // Get the GTFS-RT feedMessage for this method
                InputStream in = gtfsRtFeedUrl.openStream();
                byte[] gtfsRtProtobuf = IOUtils.toByteArray(in);

                boolean isUniqueFeed = true;
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] prevFeedDigest = null;
                byte[] currentFeedDigest = md.digest(gtfsRtProtobuf);

                Session session = GTFSDB.InitSessionBeginTrans();
                feedIteration = (GtfsRtFeedIterationModel) session.createQuery("FROM GtfsRtFeedIterationModel"
                        + " WHERE rtFeedId = " + mCurrentGtfsRtFeed.getGtfsRtId()
                            + " ORDER BY IterationId DESC").setMaxResults(1).uniqueResult();
                if (feedIteration != null) {
                    prevFeedDigest = feedIteration.getFeedHash();
                }

                if(MessageDigest.isEqual(currentFeedDigest, prevFeedDigest)) {
                    // If previous feed digest and newly fetched/current feed digest are equal means, we received the same feed again.
                    isUniqueFeed = false;
                }

                InputStream is = new ByteArrayInputStream(gtfsRtProtobuf);
                feedMessage = GtfsRealtime.FeedMessage.parseFrom(is);

                // Create new feedIteration object and save the iteration to the database
                if(isUniqueFeed) {
                    feedIteration = new GtfsRtFeedIterationModel(TimeStampHelper.getCurrentTimestamp(), gtfsRtProtobuf, mCurrentGtfsRtFeed, currentFeedDigest);
                } else {
                    feedIteration = new GtfsRtFeedIterationModel(TimeStampHelper.getCurrentTimestamp(), null, mCurrentGtfsRtFeed, currentFeedDigest);
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

            // Read all GTFS-rt entities
            mGtfsRtFeedMap.put(feedIteration.getGtfsRtFeedModel().getGtfsRtId(), feedMessage);
            mFeedEntityList.put(mCurrentGtfsRtFeed.getGtfsFeedModel().getFeedId(), mGtfsRtFeedMap);

            Map<Integer, GtfsRealtime.FeedMessage> feedEntityInstance = mFeedEntityList.get(mCurrentGtfsRtFeed.getGtfsFeedModel().getFeedId());

            List<GtfsRealtime.FeedEntity> allEntitiesArrayList = new ArrayList<>();

            GtfsRealtime.FeedHeader header = null;

            for (Map.Entry<Integer, GtfsRealtime.FeedMessage> allFeeds : feedEntityInstance.entrySet()) {
                int key = allFeeds.getKey();
                GtfsRealtime.FeedMessage message = feedEntityInstance.get(key);
                if (header == null) {
                    // Save one header to use in our combined feed below
                    header = feedEntityInstance.get(key).getHeader();
                }
                allEntitiesArrayList.addAll(message.getEntityList());
            }

            GtfsRealtime.FeedMessage.Builder feedMessageBuilder = GtfsRealtime.FeedMessage.newBuilder();
            feedMessageBuilder.setHeader(header);
            feedMessageBuilder.addAllEntity(allEntitiesArrayList);

            GtfsRealtime.FeedMessage combinedFeed = feedMessageBuilder.build();

            // Run validation rules
            for (FeedEntityValidator rule : mValidationRules) {
                validateEntity(combinedFeed, gtfsData, gtfsMetadata, feedIteration, rule);
            }

            logDuration(startTimeNanos);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void validateEntity(GtfsRealtime.FeedMessage feedMessage, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRtFeedIterationModel feedIteration, FeedEntityValidator feedEntityValidator) {
        List<ErrorListHelperModel> errorLists = feedEntityValidator.validate(gtfsData, gtfsMetadata, feedMessage);

        if (errorLists != null) {
            for (ErrorListHelperModel errorList : errorLists) {
                if (!errorList.getOccurrenceList().isEmpty()) {
                    //Set iteration Id
                    errorList.getErrorMessage().setGtfsRtFeedIterationModel(feedIteration);
                    //Save the captured errors to the database
                    DBHelper.saveError(errorList);
                }
            }
        }
    }

    /**
     * Logs the amount of time that this validation iteration took
     *
     * @param startTimeNanos the starting time of this iteration, in nanoseconds (e.g., System.nanoTime())
     */
    private void logDuration(long startTimeNanos) {
        long durationNanos = System.nanoTime() - startTimeNanos;
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos);
        long durationSeconds = TimeUnit.NANOSECONDS.toSeconds(durationNanos);

        _log.debug("Processed " + mCurrentGtfsRtFeed.getGtfsUrl() + " in " + durationSeconds + "." + durationMillis + " seconds");
    }
}