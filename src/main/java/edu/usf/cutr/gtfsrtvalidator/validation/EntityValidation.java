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

import com.google.common.collect.Ordering;
import com.google.transit.realtime.GtfsRealtime;

import java.util.ArrayList;
import java.util.List;

public class EntityValidation {

    List<GtfsRealtime.FeedEntity> tripUpdateList = new ArrayList<>();

    public void validate(List<GtfsRealtime.FeedEntity> entityList) {

        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasTripUpdate()) {

                tripUpdateList.add(entity);

                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                //w002: vehicle_id should be populated in trip_update
                if (tripUpdate.getVehicle().getId() == null) {
                    System.out.println("Vehicle id not present");
                    return;
                }
            }
        }

        for (GtfsRealtime.FeedEntity tripUpdateEntity : tripUpdateList) {
            List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdateList = tripUpdateEntity.getTripUpdate().getStopTimeUpdateList();

            //e02 stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
            List<Integer> stopSequanceList = new ArrayList<>();
            for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdateList) {
                stopTimeUpdate.getStopSequence();
                stopSequanceList.add(stopTimeUpdate.getStopSequence());
            }

            boolean sorted = Ordering.natural().isOrdered(stopSequanceList);
            if (sorted) {
                //System.out.println("StopSequenceList is in order");
            }else {
                //System.out.println("StopSequenceList is not in order");
            }
        }

        System.out.println(tripUpdateList.size());
    }
}
