/*
 * Copyright (C) 2011-2017 Nipuna Gunathilake, University of South Florida.
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
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implement validation rules related to feed entity timestamps:
 * * W001 - Timestamp not populated
 * * E012 - Header timestamp should be greater than or equal to all other timestamps
 */
public class TimestampValidation implements FeedEntityValidator{

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(TimestampValidation.class);

    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsRealtime.FeedMessage feedMessage) {
        MessageLogModel modelW001 = new MessageLogModel(ValidationRules.W001);
        OccurrenceModel errorW001;
        List<OccurrenceModel> errorListW001 = new ArrayList<>();

        MessageLogModel modelE012 = new MessageLogModel(ValidationRules.E012);
        OccurrenceModel errorE012;
        List<OccurrenceModel> errorListE012 = new ArrayList<>();

        long headerTimestamp = feedMessage.getHeader().getTimestamp();
        if (headerTimestamp == 0) {
            _log.debug("Timestamp not present in FeedHeader");
            errorW001 = new OccurrenceModel("$.header.timestamp not populated", String.valueOf(headerTimestamp));
            errorListW001.add(errorW001);
        }
        for(GtfsRealtime.FeedEntity entity: feedMessage.getEntityList()) {
            long tripupdateTimestamp = entity.getTripUpdate().getTimestamp();
            long vehicleTimestamp = entity.getVehicle().getTimestamp();
            if (tripupdateTimestamp == 0) {
                _log.debug("Timestamp not present in TripUpdate");
                errorW001 = new OccurrenceModel("$.entity.*.trip_update.timestamp not populated", String.valueOf(tripupdateTimestamp));
                errorListW001.add(errorW001);
            } else {
                if (headerTimestamp != 0 && tripupdateTimestamp > headerTimestamp) {
                    _log.debug("TripUpdate timestamp is greater than Header timestamp");
                    errorE012 = new OccurrenceModel("$.entity.*.trip_update.timestamp is greater than the FeedHeader timestamp", String.valueOf(tripupdateTimestamp));
                    errorListE012.add(errorE012);
                }
            }
            if (vehicleTimestamp == 0) {
                _log.debug("Timestamp not present in VehiclePosition");
                errorW001 = new OccurrenceModel("$.entity.*.vehicle_position.timestamp not populated", String.valueOf(vehicleTimestamp));
                errorListW001.add(errorW001);
            } else {
                if (headerTimestamp != 0 && vehicleTimestamp > headerTimestamp) {
                    _log.debug("VehiclePosition timestamp is greater than Header timestamp");
                    errorE012 = new OccurrenceModel("$.entity.*.vehicle_position.timestamp is greater than the FeedHeader timestamp", String.valueOf(tripupdateTimestamp));
                    errorListE012.add(errorE012);
                }
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorListW001.isEmpty()) {
            errors.add(new ErrorListHelperModel(modelW001, errorListW001));
        }
        if (!errorListE012.isEmpty()) {
            errors.add(new ErrorListHelperModel(modelE012, errorListE012));
        }
        return errors;
    }
}
