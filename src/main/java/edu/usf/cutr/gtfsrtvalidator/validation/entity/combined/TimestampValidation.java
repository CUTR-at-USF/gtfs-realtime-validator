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

package edu.usf.cutr.gtfsrtvalidator.validation.entity.combined;

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

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.isPosix;

/**
 * Implement validation rules related to feed entity timestamps:
 *  * W001 - Timestamp not populated
 *  * E001 - Not in POSIX time
 *  * E012 - Header timestamp should be greater than or equal to all other timestamps
 */
public class TimestampValidation implements FeedEntityValidator{

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(TimestampValidation.class);

    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsRealtime.FeedMessage feedMessage) {
        MessageLogModel modelW001 = new MessageLogModel(ValidationRules.W001);
        OccurrenceModel errorW001;
        List<OccurrenceModel> errorListW001 = new ArrayList<>();

        MessageLogModel modelE001 = new MessageLogModel(ValidationRules.E001);
        OccurrenceModel errorE001;
        List<OccurrenceModel> errorListE001 = new ArrayList<>();

        MessageLogModel modelE012 = new MessageLogModel(ValidationRules.E012);
        OccurrenceModel errorE012;
        List<OccurrenceModel> errorListE012 = new ArrayList<>();

        /**
         * Validate FeedHeader timestamp - W001 and E001
         */

        long headerTimestamp = feedMessage.getHeader().getTimestamp();
        if (headerTimestamp == 0) {
            _log.debug("Timestamp not present in FeedHeader");
            errorW001 = new OccurrenceModel("$.header.timestamp not populated", String.valueOf(headerTimestamp));
            errorListW001.add(errorW001);
        } else {
            if (!isPosix(headerTimestamp)) {
                _log.debug("FeedHeader timestamp is not POSIX time");
                errorE001 = new OccurrenceModel("$.header.timestamp is not POSIX time", String.valueOf(headerTimestamp));
                errorListE001.add(errorE001);
            }
        }
        for(GtfsRealtime.FeedEntity entity: feedMessage.getEntityList()) {
            GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
            long tripUpdateTimestamp = tripUpdate.getTimestamp();

            long vehicleTimestamp = entity.getVehicle().getTimestamp();

            /**
             * Validate VehiclePosition and TripUpdate timestamps - W001, E001, E012
             */
            if (tripUpdateTimestamp == 0) {
                _log.debug("Timestamp not present in TripUpdate");
                errorW001 = new OccurrenceModel("$.entity.*.trip_update.timestamp not populated", String.valueOf(tripUpdateTimestamp));
                errorListW001.add(errorW001);
            } else {
                if (headerTimestamp != 0 && tripUpdateTimestamp > headerTimestamp) {
                    _log.debug("TripUpdate timestamp is greater than Header timestamp");
                    errorE012 = new OccurrenceModel("$.entity.*.trip_update.timestamp is greater than the FeedHeader timestamp", String.valueOf(tripUpdateTimestamp));
                    errorListE012.add(errorE012);
                }
                if (!isPosix(tripUpdateTimestamp)) {
                    _log.debug("TripUpdate timestamp is not POSIX time");
                    errorE001 = new OccurrenceModel("$.entity.*.trip_update.timestamp is not POSIX time", String.valueOf(tripUpdateTimestamp));
                    errorListE001.add(errorE001);
                }
            }
            if (vehicleTimestamp == 0) {
                _log.debug("Timestamp not present in VehiclePosition");
                errorW001 = new OccurrenceModel("$.entity.*.vehicle_position.timestamp not populated", String.valueOf(vehicleTimestamp));
                errorListW001.add(errorW001);
            } else {
                if (headerTimestamp != 0 && vehicleTimestamp > headerTimestamp) {
                    _log.debug("VehiclePosition timestamp is greater than Header timestamp");
                    errorE012 = new OccurrenceModel("$.entity.*.vehicle_position.timestamp is greater than the FeedHeader timestamp", String.valueOf(tripUpdateTimestamp));
                    errorListE012.add(errorE012);
                }
                if (!isPosix(vehicleTimestamp)) {
                    _log.debug("VehiclePosition timestamp is not POSIX time");
                    errorE001 = new OccurrenceModel("$.entity.*.vehicle_position.timestamp is not POSIX time", String.valueOf(vehicleTimestamp));
                    errorListE001.add(errorE001);
                }
            }

            /**
             * Validate TripUpdate StopTimeUpdate times - E001
             */
            List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdates = tripUpdate.getStopTimeUpdateList();
            if (stopTimeUpdates != null) {
                for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdates) {
                    if (stopTimeUpdate.hasArrival()) {
                        if (stopTimeUpdate.getArrival().hasTime()) {
                            if (!isPosix(stopTimeUpdate.getArrival().getTime())) {
                                _log.debug("StopTimeUpdate arrival time is not POSIX time");
                                errorE001 = new OccurrenceModel("$.entity.*.trip_update.stop_time_update.arrival.time is not POSIX time", String.valueOf(vehicleTimestamp));
                                errorListE001.add(errorE001);
                            }
                        }
                    }

                    if (stopTimeUpdate.hasDeparture()) {
                        if (stopTimeUpdate.getDeparture().hasTime()) {
                            if (!isPosix(stopTimeUpdate.getDeparture().getTime())) {
                                _log.debug("StopTimeUpdate departure time is not POSIX time");
                                errorE001 = new OccurrenceModel("$.entity.*.trip_update.stop_time_update.departure.time is not POSIX time", String.valueOf(vehicleTimestamp));
                                errorListE001.add(errorE001);
                            }
                        }
                    }
                }
            }

            /**
             * Validate Alert time ranges - E001
             */
            GtfsRealtime.Alert alert = entity.getAlert();
            if (alert != null) {
                List<GtfsRealtime.TimeRange> activePeriods = alert.getActivePeriodList();
                if (activePeriods != null) {
                    for (GtfsRealtime.TimeRange range : activePeriods) {
                        if (range.hasStart()) {
                            if (!isPosix(range.getStart())) {
                                _log.debug("Alert starting time range time is not POSIX time");
                                errorE001 = new OccurrenceModel("$.entity.*.alert.active_period.start is not POSIX time", String.valueOf(vehicleTimestamp));
                                errorListE001.add(errorE001);
                            }
                        }
                        if (range.hasEnd()) {
                            if (!isPosix(range.getEnd())) {
                                _log.debug("Alert ending time range time is not POSIX time");
                                errorE001 = new OccurrenceModel("$.entity.*.alert.active_period.end is not POSIX time", String.valueOf(vehicleTimestamp));
                                errorListE001.add(errorE001);
                            }
                        }
                    }
                }
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorListW001.isEmpty()) {
            errors.add(new ErrorListHelperModel(modelW001, errorListW001));
        }
        if (!errorListE001.isEmpty()) {
            errors.add(new ErrorListHelperModel(modelE001, errorListE001));
        }
        if (!errorListE012.isEmpty()) {
            errors.add(new ErrorListHelperModel(modelE012, errorListE012));
        }
        return errors;
    }
}
