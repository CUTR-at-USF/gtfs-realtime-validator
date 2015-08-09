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

package edu.usf.cutr.gtfsrtvalidator.validation;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.Trip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EntityGtfsFeedValidation {

    //Check if all trip ids in the entities provided have matching counterparts in the GTFS feed
    public static boolean checkTripIds(GtfsDaoImpl gtfsFeed, List<GtfsRealtime.FeedEntity> entityList) {
        List<GtfsRealtime.FeedEntity>tripUpdateList = new ArrayList<>();
        Collection<Trip> gtfsTripList = gtfsFeed.getAllTrips();

        List<String> tripList = new ArrayList<>();

        //get a list of trip Ids from the GTFS feed
        for (Trip trip : gtfsTripList) {
            tripList.add(trip.getId().getId());
        }

        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasTripUpdate()) {
                tripUpdateList.add(entity);

                String tripId = entity.getTripUpdate().getTrip().getTripId();
                
                if (tripList.contains(tripId)) {
                    System.out.println("No matching trip_id in the GTFS feed for: " + tripId);
                } else {

                }

            }
        }
        return true;
    }
}
