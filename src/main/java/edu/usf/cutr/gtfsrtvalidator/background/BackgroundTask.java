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
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.helper.DBHelper;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.helper.TimeStampHelper;
import edu.usf.cutr.gtfsrtvalidator.validation.EntityValidation;
import edu.usf.cutr.gtfsrtvalidator.validation.HeaderValidation;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
        URL gtfsRtFeedUrl = null;

        try {
            gtfsRtFeedUrl = new URL(currentFeed.getGtfsUrl());
        } catch (Exception e) {
            System.out.println("Malformed Url");
            e.printStackTrace();
        }

        GtfsRealtime.FeedMessage feedMessage = null;
        byte[] gtfsRtProtobuf = null;

        try {
            assert gtfsRtFeedUrl != null;
            InputStream in = gtfsRtFeedUrl.openStream();
            gtfsRtProtobuf = IOUtils.toByteArray(in);
            InputStream is = new ByteArrayInputStream(gtfsRtProtobuf);
            feedMessage = GtfsRealtime.FeedMessage.parseFrom(is);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        GtfsFeedIterationModel feedIteration = new GtfsFeedIterationModel();
        feedIteration.setFeedprotobuf(gtfsRtProtobuf);
        feedIteration.setTimeStamp(TimeStampHelper.getCurrentTimestamp());
        feedIteration.setRtFeedId(currentFeed.getGtfsRtId());

        //Return id from feed iteration
        int iterationId = GTFSDB.createRtFeedInfo(feedIteration);


        //get the header of the feed
        assert feedMessage != null;
        GtfsRealtime.FeedHeader header = feedMessage.getHeader();

        //Validation rules for all headers
        ErrorListHelperModel headerErrors = HeaderValidation.validate(header);
        if (headerErrors != null) {
            //Use returned id to save errors to database
            headerErrors.getErrorMessage().setIterationId(iterationId);

            //Save the captured errors to the database
            DBHelper.saveError(headerErrors);
        }

        List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();

        //Validation rules for entity
        EntityValidation entityValidation = new EntityValidation();
        entityValidation.validate(entityList);

        System.out.println("Entities in current feed:\t" + entityList.size());

        //Save all entities under the gtfs-rt ID
        int gtfsId = currentFeed.getGtfsId();
        if (feedEntityList.containsKey(gtfsId)) {
            List<TimeFeedEntity> currentEntityList = feedEntityList.get(currentFeed.getGtfsId());
            //Add entities to existing list
            List<TimeFeedEntity> timeFeedEntityList = getTimeFeedEntities(entityList);
            currentEntityList.addAll(timeFeedEntityList);

            //Clean the entities to a given timestamp
            List<TimeFeedEntity> cleanedList = cleanList(currentEntityList);
            feedEntityList.put(currentFeed.getGtfsId(), cleanedList);

        } else {
            List<TimeFeedEntity> timeFeedEntityList = getTimeFeedEntities(entityList);

            //Create list and add entities
            feedEntityList.put(currentFeed.getGtfsId(), timeFeedEntityList);
        }

        //ArrayLists to hold all the entities categorized by type
        List<GtfsRealtime.TripUpdate> tripUpdates = new ArrayList<>();
        List<GtfsRealtime.Alert> alerts = new ArrayList<>();
        List<GtfsRealtime.VehiclePosition> vehiclePositions = new ArrayList<>();

        //Loop through all the entities in the feeds check for associating errors
        for (TimeFeedEntity entity : feedEntityList.get(currentFeed.getGtfsId())) {
            GtfsRealtime.FeedEntity currentFeedEntity = entity.getEntity();

            //Run checks that compare entities of same GTFS-feed but from differed rt-feeds

            if(currentFeedEntity.hasTripUpdate()){
                GtfsRealtime.TripUpdate tripUpdate = currentFeedEntity.getTripUpdate();
                tripUpdates.add(tripUpdate);
            }if(currentFeedEntity.hasVehicle()){
                GtfsRealtime.VehiclePosition vehicle = currentFeedEntity.getVehicle();
                vehiclePositions.add(vehicle);

            }if(currentFeedEntity.hasAlert()){
                GtfsRealtime.Alert alert = currentFeedEntity.getAlert();
                alerts.add(alert);
            }
        }

        //w003: If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values
        // should match between the two feeds.

        //Should be optimized since this would be costly with a higher number of feeds
        if(!tripUpdates.isEmpty() && !vehiclePositions.isEmpty()) {
            for (GtfsRealtime.TripUpdate trip : tripUpdates) {
                boolean matchingTrips = false;
                for (GtfsRealtime.VehiclePosition vehiclePosition : vehiclePositions) {

                    if (Objects.equals(trip.getTrip().getTripId(), vehiclePosition.getTrip().getTripId())) {
                        matchingTrips = true;
                        break;
                    }else if (Objects.equals(trip.getVehicle().getId(), vehiclePosition.getVehicle().getId())) {
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
                    }else if (Objects.equals(trip.getVehicle().getId(), vehiclePosition.getVehicle().getId())) {
                        matchingTrips = true;
                        break;
                    }
                }
                if (!matchingTrips) {
                    System.out.println("No matching entity for" + vehiclePosition.toString());
                }
            }
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

}