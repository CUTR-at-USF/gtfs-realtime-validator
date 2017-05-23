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
import edu.usf.cutr.gtfsrtvalidator.validation.entity.StopTimeSequenceValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.StopValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.VehicleValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.*;
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
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils.logDuration;

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
                mValidationRules.add(new VehicleTripDescriptorValidator());
                mValidationRules.add(new VehicleValidator());
                mValidationRules.add(new TimestampValidator());
                mValidationRules.add(new StopTimeSequenceValidator());
                mValidationRules.add(new TripDescriptorValidator());
                mValidationRules.add(new StopValidator());
                mValidationRules.add(new FrequencyTypeZeroValidator());
                mValidationRules.add(new FrequencyTypeOneValidator());
            }
        }
    }

    @Override
    public void run() {
        try {
            long startTimeNanos = System.nanoTime();
            GtfsRealtime.FeedMessage currentFeedMessage;
            GtfsRealtime.FeedMessage previousFeedMessage = null;
            GtfsDaoImpl gtfsData;
            GtfsMetadata gtfsMetadata;

            // Holds data needed in the database under each iteration
            GtfsRtFeedIterationModel feedIteration;

            // Get the GTFS feed from the GtfsDaoMap using the gtfsFeedId of the current feed.
            gtfsData = GtfsFeed.GtfsDaoMap.get(mCurrentGtfsRtFeed.getGtfsFeedModel().getFeedId());
            // Create the GTFS metadata if it doesn't already exist
            gtfsMetadata = mGtfsMetadata.computeIfAbsent(mCurrentGtfsRtFeed.getGtfsFeedModel().getFeedId(),
                    k -> new GtfsMetadata(mCurrentGtfsRtFeed.getGtfsFeedModel().getGtfsUrl(),
                            TimeZone.getTimeZone(mCurrentGtfsRtFeed.getGtfsFeedModel().getAgency()),
                            gtfsData));

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

                Session session = GTFSDB.initSessionBeginTrans();
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
                currentFeedMessage = GtfsRealtime.FeedMessage.parseFrom(is);

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

            // Use the same current time for all rules for consistency
            long currentTimeMillis = System.currentTimeMillis();

            // Run validation rules
            for (FeedEntityValidator rule : mValidationRules) {
                validateEntity(currentTimeMillis, combinedFeed, previousFeedMessage, gtfsData, gtfsMetadata, feedIteration, rule);
            }

            logDuration(_log, "Processed " + mCurrentGtfsRtFeed.getGtfsUrl() + " in ", startTimeNanos);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void validateEntity(long currentTimeMillis, GtfsRealtime.FeedMessage currentFeedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRtFeedIterationModel feedIteration, FeedEntityValidator feedEntityValidator) {
        long startTimeNanos = System.nanoTime();
        List<ErrorListHelperModel> errorLists = feedEntityValidator.validate(currentTimeMillis, gtfsData, gtfsMetadata, currentFeedMessage, previousFeedMessage);
        logDuration(_log, "Processed " + feedEntityValidator.getClass().getSimpleName() + " in ", startTimeNanos);

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
}