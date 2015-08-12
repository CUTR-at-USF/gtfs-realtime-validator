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
import edu.usf.cutr.gtfsrtvalidator.validation.EntityGtfsFeedValidation;
import edu.usf.cutr.gtfsrtvalidator.validation.EntityValidation;
import edu.usf.cutr.gtfsrtvalidator.validation.HeaderValidation;
import org.apache.commons.io.IOUtils;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class BackgroundTask implements Runnable {

    //Entity list kept under the gtfsFeed id.
    //Used to check errors with different feeds for the same transit agency.
    HashMap<Integer, List<TimeFeedEntity>> feedEntityList = new HashMap<>();

    private GtfsRtFeedModel currentFeed = null;

    public BackgroundTask(GtfsRtFeedModel gtfsRtFeed) {
        //Accept the gtfs feed id and save entities of the same feed in an array
        currentFeed = gtfsRtFeed;
    }

    @Override
    public void run() {
        GtfsRealtime.FeedMessage feedMessage;
        GtfsRealtime.FeedHeader feedHeader;
        List<GtfsRealtime.FeedEntity> currentFeedEntityList;
        GtfsDaoImpl gtfsData;

        //Holds data needed in the database under each iteration
        GtfsFeedIterationModel feedIteration;

        //Get the GTFS feed from the GtfsDaoMap using the gtfsFeedId of the current feed.
        gtfsData = GtfsFeed.GtfsDaoMap.get(currentFeed.getGtfsId());

        //Parse the URL from the string provided
        URL gtfsRtFeedUrl;
        try {
            gtfsRtFeedUrl = new URL(currentFeed.getGtfsUrl());
        } catch (MalformedURLException e) {
            System.out.println("Malformed Url");
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
            e.printStackTrace();
            return;
        }

        //get the header of the feed
        feedHeader = feedMessage.getHeader();

        //Save all entities under the gtfs-rt ID
        currentFeedEntityList = feedMessage.getEntityList();

        if (feedEntityList.containsKey(currentFeed.getGtfsId())) {
            List<TimeFeedEntity> currentEntityList = feedEntityList.get(currentFeed.getGtfsId());

            //Add entities to existing list
            List<TimeFeedEntity> timeFeedEntityList = getTimeFeedEntities(currentFeedEntityList);
            currentEntityList.addAll(timeFeedEntityList);

            //Remove items that are older than a given timestamp
            List<TimeFeedEntity> cleanedList = cleanList(currentEntityList);
            feedEntityList.put(currentFeed.getGtfsId(), cleanedList);

        } else {
            List<TimeFeedEntity> timeFeedEntityList = getTimeFeedEntities(currentFeedEntityList);

            //Create list and add entities
            feedEntityList.put(currentFeed.getGtfsId(), timeFeedEntityList);
        }

        //ArrayLists to hold all the entities categorized by type
        List<GtfsRealtime.TripUpdate> tripUpdates = new ArrayList<>();
        List<GtfsRealtime.Alert> alerts = new ArrayList<>();
        List<GtfsRealtime.VehiclePosition> vehiclePositions = new ArrayList<>();

        //Take all entities for the GTFS feed and put the entities to correct ArrayList
        for (TimeFeedEntity entity : feedEntityList.get(currentFeed.getGtfsId())) {
            GtfsRealtime.FeedEntity currentFeedEntity = entity.getEntity();

            if (currentFeedEntity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = currentFeedEntity.getTripUpdate();
                tripUpdates.add(tripUpdate);
            }
            if (currentFeedEntity.hasVehicle()) {
                GtfsRealtime.VehiclePosition vehicle = currentFeedEntity.getVehicle();
                vehiclePositions.add(vehicle);

            }
            if (currentFeedEntity.hasAlert()) {
                GtfsRealtime.Alert alert = currentFeedEntity.getAlert();
                alerts.add(alert);
            }
        }

        //region Rules for header errors

        //Validation rules for all headers
        HeaderValidation validateHeaders = new HeaderValidation();
        ErrorListHelperModel headerErrors = validateHeaders.validate(feedIteration.getIterationId(), gtfsData, feedHeader, currentFeedEntityList);
        if (headerErrors != null) {
            //Save the captured errors to the database
            DBHelper.saveError(headerErrors);
        }
        //endregion

        //region Rules for all errors in the current feed
        //---------------------------------------------------------------------------------------
        //Validation rules for entity
        EntityValidation entityValidation = new EntityValidation();
        entityValidation.validate(currentFeedEntityList);

        EntityGtfsFeedValidation.checkTripIds(gtfsData, currentFeedEntityList);
        //---------------------------------------------------------------------------------------
        //endregion

        //region Rules for all errors in all feeds
        //---------------------------------------------------------------------------------------
        //w003: If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values
        // should match between the two feeds.

        //Should be optimized since this would be costly with a higher number of feeds
        if (!tripUpdates.isEmpty() && !vehiclePositions.isEmpty()) {
            for (GtfsRealtime.TripUpdate trip : tripUpdates) {
                boolean matchingTrips = false;
                for (GtfsRealtime.VehiclePosition vehiclePosition : vehiclePositions) {

                    if (Objects.equals(trip.getTrip().getTripId(), vehiclePosition.getTrip().getTripId())) {
                        matchingTrips = true;
                        break;
                    } else if (Objects.equals(trip.getVehicle().getId(), vehiclePosition.getVehicle().getId())) {
                        matchingTrips = true;
                        break;
                    }
                }
                if (!matchingTrips) {
                    System.out.println("No matching entity for" + trip.toString());
                }
            }

            for (GtfsRealtime.VehiclePosition vehiclePosition : vehiclePositions) {
                boolean matchingTrips = false;
                for (GtfsRealtime.TripUpdate trip : tripUpdates) {

                    if (Objects.equals(trip.getTrip().getTripId(), vehiclePosition.getTrip().getTripId())) {
                        matchingTrips = true;
                        break;
                    } else if (Objects.equals(trip.getVehicle().getId(), vehiclePosition.getVehicle().getId())) {
                        matchingTrips = true;
                        break;
                    }
                }
                if (!matchingTrips) {
                    System.out.println("No matching entity for" + vehiclePosition.toString());
                }
            }
        }
        //---------------------------------------------------------------------------------------
        //endregion

    }


    //Check the timestamp differences to avoid comparing older entities
    private List<TimeFeedEntity> cleanList(List<TimeFeedEntity> currentEntityList) {
        List<TimeFeedEntity> cleanedEntityList = new ArrayList<>();
        for (TimeFeedEntity entity : currentEntityList) {
            //Checks if the feed was uploaded within the last 15 seconds
            if (TimeStampHelper.getCurrentTimestamp() - entity.getTimestamp() < 15) {
                cleanedEntityList.add(entity);
            }
        }
        System.out.println("Entities in cleaned feed:\t" + cleanedEntityList.size());
        return cleanedEntityList;
    }

    class TimeFeedEntity {
        public TimeFeedEntity(GtfsRealtime.FeedEntity entity, long timestamp) {
            this.entity = entity;
            this.timestamp = timestamp;
        }

        private GtfsRealtime.FeedEntity entity;
        private long timestamp;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }

        public GtfsRealtime.FeedEntity getEntity() {
            return entity;
        }

        public void setEntity(GtfsRealtime.FeedEntity entity) {
            this.entity = entity;
        }
    }

    private List<TimeFeedEntity> getTimeFeedEntities(List<GtfsRealtime.FeedEntity> entityList) {
        long timestamp = TimeStampHelper.getCurrentTimestamp();
        List<TimeFeedEntity> timeFeedEntityList = new ArrayList<>();
        for (GtfsRealtime.FeedEntity entity : entityList) {
            TimeFeedEntity timeEntity = new TimeFeedEntity(entity, timestamp);
            timeFeedEntityList.add(timeEntity);
        }
        return timeFeedEntityList;
    }
}