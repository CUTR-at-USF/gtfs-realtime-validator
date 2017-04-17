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

package edu.usf.cutr.gtfsrtvalidator.validation.entity;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.Stop;

import java.util.*;

/**
 * ID: e011
 * Description: All stop_ids referenced in GTFS-rt feeds must have the "location_type" = 0
 */
public class LocationTypeReferenceValidator implements FeedEntityValidator {
    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsRealtime.FeedMessage feedMessage) {
        List<OccurrenceModel> e011List = new ArrayList<>();
        List<GtfsRealtime.FeedEntity> allEntities = feedMessage.getEntityList();

        //Get all stops from the GTFS feed
        Collection<Stop> stops = gtfsData.getAllStops();

        //Get a list of stop_ids from the GTFS feeds stops.txt
        Set<String> stopIds = new HashSet<>();
        for (Stop stop : stops) {
            stopIds.add(stop.getId().getId());
        }

        //Checks all of the RT feeds entities and checks if matching stop_ids are available in the GTFS feed
        for (GtfsRealtime.FeedEntity entity : allEntities) {
            String entityId = entity.getId();
            if (entity.hasTripUpdate()) {
                List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdateList = entity.getTripUpdate().getStopTimeUpdateList();
                //TripUpdate>StopTimeUpdate>stop_is
                for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdateList) {
                    if (stopTimeUpdate.hasStopId() && !stopIds.contains(stopTimeUpdate.getStopId())) {
                        OccurrenceModel errorOccurrence = new OccurrenceModel("$.entity["+ entityId +"]", stopTimeUpdate.getStopId());
                        e011List.add(errorOccurrence);
                    }
                }
            }
            if (entity.hasVehicle()) {
                //VehiclePostion>stop_id
                if(entity.getVehicle().hasStopId() && !stopIds.contains(entity.getVehicle().getStopId())){
                    OccurrenceModel errorOccurrence = new OccurrenceModel("$.entity["+ entityId +"]", entity.getVehicle().getStopId());
                    e011List.add(errorOccurrence);
                }
            }
            if (entity.hasAlert()) {
                //Alert>EntitySelector>stop_id(optional)
                List<GtfsRealtime.EntitySelector> informedEntityList = entity.getAlert().getInformedEntityList();
                for (GtfsRealtime.EntitySelector entitySelector : informedEntityList) {
                    if (entitySelector.hasStopId() && !stopIds.contains(entitySelector.getStopId())) {
                        OccurrenceModel errorOccurrence = new OccurrenceModel("$.entity["+ entityId +"]", entitySelector.getStopId());
                        e011List.add(errorOccurrence);
                    }
                }
            }
        }
        return Arrays.asList(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E011), e011List));
    }
}
