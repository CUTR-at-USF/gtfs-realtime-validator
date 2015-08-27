/*
 * Copyright (C) 2011 Nipuna Gunathilake.
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
import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsFeedIterationModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsRtFeedModel;
import edu.usf.cutr.gtfsrtvalidator.api.resource.GtfsFeed;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.helper.DBHelper;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.helper.TimeStampHelper;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.LocationTypeReferenceValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.CheckTripId;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.StopTimeSequanceValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.VehicleIdValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.entity.combined.VehicleTripDescriptorValidator;
import edu.usf.cutr.gtfsrtvalidator.validation.header.HeaderValidation;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.apache.commons.io.IOUtils;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeLibrary;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundTask implements Runnable {
    //Entity list kept under the gtfsRtFeed id.
    //Used to check errors with different feeds for the same transit agency.

    //HashMap<Integer, List<TimeFeedEntity>> feedEntityList = new HashMap<>();
    static HashMap<Integer, HashMap<Integer, GtfsRealtime.FeedMessage>> feedEntityList = new HashMap<>();
    static HashMap<Integer, GtfsRealtime.FeedMessage> gtfsFeedHash = new HashMap<>();

    private GtfsRtFeedModel currentFeed = null;

    public BackgroundTask(GtfsRtFeedModel gtfsRtFeed) {
        //Accept the gtfs feed id and save entities of the same feed in an array
        currentFeed = gtfsRtFeed;
    }

    @Override
    public void run() {

        try {
            GtfsRealtime.FeedMessage feedMessage;
            GtfsDaoImpl gtfsData;

            //Holds data needed in the database under each iteration
            GtfsFeedIterationModel feedIteration;

            //Get the GTFS feed from the GtfsDaoMap using the gtfsFeedId of the current feed.
            gtfsData = GtfsFeed.GtfsDaoMap.get(currentFeed.getGtfsId());

            //region Get gtfsRtFeed Iteration
            //---------------------------------------------------------------------------------------
            //Parse the URL from the string provided
            URL gtfsRtFeedUrl;
            try {
                gtfsRtFeedUrl = new URL(currentFeed.getGtfsUrl());
            } catch (MalformedURLException e) {
                System.out.println("Malformed Url: " + currentFeed.getGtfsUrl());
                e.printStackTrace();
                return;
            }

            try {
                //Get the GTFS-RT feedMessage for this method
                InputStream in = gtfsRtFeedUrl.openStream();
                byte[] gtfsRtProtobuf = IOUtils.toByteArray(in);
                InputStream is = new ByteArrayInputStream(gtfsRtProtobuf);
                feedMessage = GtfsRealtime.FeedMessage.parseFrom(is);

                //Create new feedIteration object and save the iteration to the database
                feedIteration = new GtfsFeedIterationModel(TimeStampHelper.getCurrentTimestamp(), gtfsRtProtobuf, currentFeed.getGtfsRtId());
                int iterationId = GTFSDB.createRtFeedInfo(feedIteration);
                feedIteration.setIterationId(iterationId);
            } catch (Exception e) {
                System.out.println("The URL: " + gtfsRtFeedUrl + " does not contain valid Gtfs-Rt data");
                //e.printStackTrace();
                return;
            }
            //---------------------------------------------------------------------------------------
            //endregion

            //region Get all entities for a GTFS feed
            //---------------------------------------------------------------------------------------
            gtfsFeedHash.put(feedIteration.getRtFeedId(), feedMessage);
            feedEntityList.put(currentFeed.getGtfsId(), gtfsFeedHash);

            HashMap<Integer, GtfsRealtime.FeedMessage> feedEntityInstance = feedEntityList.get(currentFeed.getGtfsId());

            List<GtfsRealtime.FeedEntity> allEntitiesArrayList = new ArrayList<>();

            for (Map.Entry<Integer, GtfsRealtime.FeedMessage> allFeeds : feedEntityInstance.entrySet()) {
                int key = allFeeds.getKey();
                GtfsRealtime.FeedMessage message = feedEntityInstance.get(key);
                allEntitiesArrayList.addAll(message.getEntityList());
            }

            GtfsRealtime.FeedMessage.Builder feedMessageBuilder = GtfsRealtimeLibrary.createFeedMessageBuilder();
            feedMessageBuilder.addAllEntity(allEntitiesArrayList);

            GtfsRealtime.FeedMessage combinedFeed = feedMessageBuilder.build();
            //---------------------------------------------------------------------------------------
            //endregion

            //region Rules for all errors in all RT feeds in for One GTFS feed
            //---------------------------------------------------------------------------------------
            VehicleTripDescriptorValidator vehicleTripDescriptorValidator = new VehicleTripDescriptorValidator();
            validateEntity(combinedFeed, gtfsData, feedIteration, vehicleTripDescriptorValidator);
            //---------------------------------------------------------------------------------------
            //endregion

            //region Rules for header errors
            //---------------------------------------------------------------------------------------
            //Validation rules for all headers
            HeaderValidation validateHeaders = new HeaderValidation();
            validateEntity(feedMessage, gtfsData, feedIteration, validateHeaders);
            //---------------------------------------------------------------------------------------
            //endregion

            //region Rules for all errors in the current feed
            //---------------------------------------------------------------------------------------
            FeedEntityValidator vehicleIdValidator = new VehicleIdValidator();
            validateEntity(feedMessage, gtfsData, feedIteration, vehicleIdValidator);

            FeedEntityValidator stopTimeSequenceValidator = new StopTimeSequanceValidator();
            validateEntity(feedMessage, gtfsData, feedIteration, stopTimeSequenceValidator);
            //---------------------------------------------------------------------------------------
            //endregion

            //region Rules for all errors in the current feed + GTFS feed
            //---------------------------------------------------------------------------------------
            FeedEntityValidator checkTripId = new CheckTripId();
            validateEntity(feedMessage, gtfsData, feedIteration, checkTripId);

            //e010
            FeedEntityValidator locationTypeReferenceValidator = new LocationTypeReferenceValidator();
            validateEntity(feedMessage, gtfsData, feedIteration, locationTypeReferenceValidator);
            //---------------------------------------------------------------------------------------
            //endregion
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void validateEntity(GtfsRealtime.FeedMessage feedMessage, GtfsDaoImpl gtfsData, GtfsFeedIterationModel feedIteration, FeedEntityValidator feedEntityValidator) {
        ErrorListHelperModel errorList = feedEntityValidator.validate(gtfsData, feedMessage);

        if (errorList != null && !errorList.getOccurrenceList().isEmpty()) {
            //Set iteration Id
            errorList.getErrorMessage().setIterationId(feedIteration.getIterationId());
            //Save the captured errors to the database
            DBHelper.saveError(errorList);
        }
    }

}