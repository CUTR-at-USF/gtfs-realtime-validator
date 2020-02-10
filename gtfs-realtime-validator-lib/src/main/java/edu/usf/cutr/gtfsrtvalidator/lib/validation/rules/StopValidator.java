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

package edu.usf.cutr.gtfsrtvalidator.lib.validation.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.lib.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.util.RuleUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.E011;
import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.E015;

/**
 * Rules:
 *
 * E011 - All stop_ids referenced in GTFS-rt feed must appear in the GTFS feed
 * E015 - All stop_ids referenced in GTFS-rt TripUpdates and VehiclePositions feeds must have the location_type = 0
 */
public class StopValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(StopValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsMutableDao gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
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
                        String prefix = "trip_id " + tripUpdate.getTrip().getTripId() + " stop_id " + stopTimeUpdate.getStopId();
                        if (!gtfsMetadata.getStopIds().contains(stopTimeUpdate.getStopId())) {
                            // E011 - All stop_ids referenced in GTFS-rt feed must appear in the GTFS feed
                            RuleUtils.addOccurrence(E011, prefix, e011List, _log);
                        }
                        Integer locationType = gtfsMetadata.getStopToLocationTypeMap().get(stopTimeUpdate.getStopId());
                        if (locationType != null && locationType != 0) {
                            // E015 - All stop_ids referenced in GTFS-rt feeds must have the location_type = 0
                            RuleUtils.addOccurrence(E015, prefix, e015List, _log);
                        }
                    }
                }
            }
            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition v = entity.getVehicle();
                if (v.hasStopId()) {
                    if (!gtfsMetadata.getStopIds().contains(v.getStopId())) {
                        // E011 - All stop_ids referenced in GTFS-rt feed must appear in the GTFS feed
                        String prefix = (v.hasVehicle() && v.getVehicle().hasId() ? "vehicle_id " + v.getVehicle().getId() + " " : "") + "stop_id " + v.getStopId();
                        RuleUtils.addOccurrence(E011, prefix, e011List, _log);
                    }
                    Integer locationType = gtfsMetadata.getStopToLocationTypeMap().get(v.getStopId());
                    if (locationType != null && locationType != 0) {
                        // E015 - All stop_ids referenced in GTFS-rt feeds must have the location_type = 0
                        String prefix = (v.hasVehicle() && v.getVehicle().hasId() ? "vehicle_id " + v.getVehicle().getId() + " " : "") + "stop_id " + v.getStopId();
                        RuleUtils.addOccurrence(E015, prefix, e015List, _log);
                    }
                }
            }
            if (entity.hasAlert()) {
                List<GtfsRealtime.EntitySelector> informedEntityList = entity.getAlert().getInformedEntityList();
                for (GtfsRealtime.EntitySelector entitySelector : informedEntityList) {
                    if (entitySelector.hasStopId()) {
                        String prefix = "alert entity ID " + entityId + " stop_id " + entitySelector.getStopId();
                        if (!gtfsMetadata.getStopIds().contains(entitySelector.getStopId())) {
                            // E011 - All stop_ids referenced in GTFS-rt feed must appear in the GTFS feed
                            RuleUtils.addOccurrence(E011, prefix, e011List, _log);
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
