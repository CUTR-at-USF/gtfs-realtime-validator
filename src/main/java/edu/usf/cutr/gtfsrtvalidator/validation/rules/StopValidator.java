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

package edu.usf.cutr.gtfsrtvalidator.validation.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.E011;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.E015;

/**
 * Rules:
 *
 * E011 - All stop_ids referenced in GTFS-rt feed must appear in the GTFS feed
 * E015 - All stop_ids referenced in GTFS-rt feeds must have the location_type = 0
 */
public class StopValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(StopValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        List<OccurrenceModel> e011List = new ArrayList<>();
        List<OccurrenceModel> e015List = new ArrayList<>();
        List<GtfsRealtime.FeedEntity> allEntities = feedMessage.getEntityList();

        // Checks all of the RT feeds entities and checks if matching stop_ids are available in the GTFS feed
        for (GtfsRealtime.FeedEntity entity : allEntities) {
            String entityId = entity.getId();
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdateList = tripUpdate.getStopTimeUpdateList();
                for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdateList) {
                    if (stopTimeUpdate.hasStopId()) {
                        if (!gtfsMetadata.getStopIds().contains(stopTimeUpdate.getStopId())) {
                            OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() + " stop_id " + stopTimeUpdate.getStopId());
                            e011List.add(om);
                            _log.debug(om.getPrefix() + " " + E011.getOccurrenceSuffix());
                        }
                        Integer locationType = gtfsMetadata.getStopToLocationTypeMap().get(stopTimeUpdate.getStopId());
                        if (locationType != null && locationType != 0) {
                            OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() + " stop_id " + stopTimeUpdate.getStopId());
                            e015List.add(om);
                            _log.debug(om.getPrefix() + " " + E015.getOccurrenceSuffix());
                        }

                    }
                }
            }
            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition v = entity.getVehicle();
                if (v.hasStopId()) {
                    if (!gtfsMetadata.getStopIds().contains(v.getStopId())) {
                        OccurrenceModel om = new OccurrenceModel((v.hasVehicle() && v.getVehicle().hasId() ? "vehicle_id " + v.getVehicle().getId() + " " : "") + "stop_id " + v.getStopId());
                        e011List.add(om);
                    }
                    Integer locationType = gtfsMetadata.getStopToLocationTypeMap().get(v.getStopId());
                    if (locationType != null && locationType != 0) {
                        OccurrenceModel om = new OccurrenceModel((v.hasVehicle() && v.getVehicle().hasId() ? "vehicle_id " + v.getVehicle().getId() + " " : "") + "stop_id " + v.getStopId());
                        e015List.add(om);
                        _log.debug(om.getPrefix() + " " + E015.getOccurrenceSuffix());
                    }
                }
            }
            if (entity.hasAlert()) {
                List<GtfsRealtime.EntitySelector> informedEntityList = entity.getAlert().getInformedEntityList();
                for (GtfsRealtime.EntitySelector entitySelector : informedEntityList) {
                    if (entitySelector.hasStopId()) {
                        if (!gtfsMetadata.getStopIds().contains(entitySelector.getStopId())) {
                            OccurrenceModel errorOccurrence = new OccurrenceModel("alert entity ID " + entityId + " stop_id " + entitySelector.getStopId());
                            e011List.add(errorOccurrence);
                        }
                        Integer locationType = gtfsMetadata.getStopToLocationTypeMap().get(entitySelector.getStopId());
                        if (locationType != null && locationType != 0) {
                            OccurrenceModel om = new OccurrenceModel("alert entity ID " + entityId + " stop_id " + entitySelector.getStopId());
                            e015List.add(om);
                            _log.debug(om.getPrefix() + " " + E015.getOccurrenceSuffix());
                        }
                    }
                }
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!e011List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E011), e011List));
        }
        if (!e015List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E015), e015List));
        }
        return errors;
    }
}
