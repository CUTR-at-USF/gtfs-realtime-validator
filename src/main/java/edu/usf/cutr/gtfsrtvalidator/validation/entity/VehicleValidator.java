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
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.hsqldb.lib.StringUtil;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.W002;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.W004;


/**
 * W002 - vehicle_id should be populated for TripUpdates and VehiclePositions
 * W004 - VehiclePosition has unrealistic speed
 */

public class VehicleValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(VehicleValidator.class);

    public static final float MAX_REALISTIC_SPEED_METERS_PER_SECOND = 26.0f;  // Approx. 60 miles per hour

    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage) {
        List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();
        List<OccurrenceModel> w002List = new ArrayList<>();
        List<OccurrenceModel> w004List = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();

                // W002: vehicle_id should be populated in trip_update
                if (StringUtil.isEmpty(tripUpdate.getVehicle().getId())) {
                    OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId());
                    w002List.add(om);
                    _log.debug(om.getPrefix() + " " + W002.getOccurrenceSuffix());
                }
            }
            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition v = entity.getVehicle();

                // W002: vehicle_id should be populated in VehiclePosition
                if (StringUtil.isEmpty(v.getVehicle().getId())) {
                    OccurrenceModel om = new OccurrenceModel("entity ID " + entity.getId());
                    w002List.add(om);
                    _log.debug(om.getPrefix() + " " + W002.getOccurrenceSuffix());
                }

                // W004: VehiclePosition has unrealistic speed
                if (v.hasPosition() && v.getPosition().hasSpeed()) {
                    if (v.getPosition().getSpeed() > MAX_REALISTIC_SPEED_METERS_PER_SECOND ||
                            v.getPosition().getSpeed() < 0f) {
                        OccurrenceModel om = new OccurrenceModel(v.getVehicle().hasId() ? "vehicle_id " + v.getVehicle().getId() : "entity ID " + entity.getId() + " speed of " + v.getPosition().getSpeed() + " m/s");
                        w004List.add(om);
                        _log.debug(om.getPrefix() + " " + W004.getOccurrenceSuffix());
                    }
                }
            }
        }

        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!w002List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W002), w002List));
        }
        if (!w004List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W004), w004List));
        }
        return errors;
    }
}
