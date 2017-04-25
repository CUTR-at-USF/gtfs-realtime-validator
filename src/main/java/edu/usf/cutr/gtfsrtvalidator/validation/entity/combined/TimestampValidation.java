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
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.isPosix;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;

/**
 * Implement validation rules related to feed entity timestamps:
 *  W001 - Timestamp not populated
 *  E001 - Not in POSIX time
 *  E012 - Header timestamp should be greater than or equal to all other timestamps
 *  E017 - GTFS-rt content changed but has the same timestamp
 *  E018 - GTFS-rt header timestamp decreased between two sequential iterations
 */
public class TimestampValidation implements FeedEntityValidator{

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(TimestampValidation.class);

    private static long MINIMUM_REFRESH_INTERVAL_SECONDS = 35L;

    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        if (feedMessage.equals(previousFeedMessage)) {
            throw new IllegalArgumentException("feedMessage and previousFeedMessage must not be the same");
        }
        List<OccurrenceModel> w001List = new ArrayList<>();
        List<OccurrenceModel> w007List = new ArrayList<>();
        List<OccurrenceModel> e001List = new ArrayList<>();
        List<OccurrenceModel> e012List = new ArrayList<>();
        List<OccurrenceModel> e017List = new ArrayList<>();
        List<OccurrenceModel> e018List = new ArrayList<>();

        /**
         * Validate FeedHeader timestamp - W001 and E001
         */
        long headerTimestamp = feedMessage.getHeader().getTimestamp();
        if (headerTimestamp == 0) {
            OccurrenceModel errorW001 = new OccurrenceModel("header");
            w001List.add(errorW001);
            _log.debug(errorW001.getPrefix() + " " + W001.getOccurrenceSuffix());
        } else {
            if (!isPosix(headerTimestamp)) {
                OccurrenceModel errorE001 = new OccurrenceModel("header.timestamp");
                e001List.add(errorE001);
                _log.debug(errorE001.getPrefix() + " " + E001.getOccurrenceSuffix());
            }
            if (previousFeedMessage != null && previousFeedMessage.getHeader().getTimestamp() != 0) {
                long previousTimestamp = previousFeedMessage.getHeader().getTimestamp();
                long interval = headerTimestamp - previousTimestamp;
                if (headerTimestamp == previousTimestamp) {
                    OccurrenceModel om = new OccurrenceModel("header.timestamp of " + headerTimestamp);
                    e017List.add(om);
                    _log.debug(om.getPrefix() + " " + E017.getOccurrenceSuffix());
                } else if (headerTimestamp < previousTimestamp) {
                    OccurrenceModel om = new OccurrenceModel("header.timestamp of " + headerTimestamp + " is less than the header.timestamp of " + previousFeedMessage.getHeader().getTimestamp());
                    e018List.add(om);
                    _log.debug(om.getPrefix() + " " + E018.getOccurrenceSuffix());
                } else if (interval > MINIMUM_REFRESH_INTERVAL_SECONDS) {
                    OccurrenceModel om = new OccurrenceModel(interval + " second interval between consecutive header.timestamps");
                    w007List.add(om);
                    _log.debug(om.getPrefix() + " " + W007.getOccurrenceSuffix());
                }
            }
        }

        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                long tripUpdateTimestamp = tripUpdate.getTimestamp();

                /**
                 * Validate TripUpdate timestamps - W001, E001, E012
                 */
                if (tripUpdateTimestamp == 0) {
                    OccurrenceModel errorW001 = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId());
                    w001List.add(errorW001);
                    _log.debug(errorW001.getPrefix() + " " + W001.getOccurrenceSuffix());
                } else {
                    if (headerTimestamp != 0 && tripUpdateTimestamp > headerTimestamp) {
                        OccurrenceModel errorE012 = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() + " timestamp " + tripUpdateTimestamp);
                        e012List.add(errorE012);
                        _log.debug(errorE012.getPrefix() + " " + E012.getOccurrenceSuffix());
                    }
                    if (!isPosix(tripUpdateTimestamp)) {
                        OccurrenceModel errorE001 = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() + " timestamp " + tripUpdateTimestamp);
                        e001List.add(errorE001);
                        _log.debug(errorE001.getPrefix() + " " + E001.getOccurrenceSuffix());
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
                                    OccurrenceModel errorE001 = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() +
                                            " stop_time_update " + stopTimeUpdate.getStopSequence() +
                                            " arrival time " + stopTimeUpdate.getArrival().getTime());
                                    e001List.add(errorE001);
                                    _log.debug(errorE001.getPrefix() + " " + E001.getOccurrenceSuffix());
                                }
                            }
                        }

                        if (stopTimeUpdate.hasDeparture()) {
                            if (stopTimeUpdate.getDeparture().hasTime()) {
                                if (!isPosix(stopTimeUpdate.getDeparture().getTime())) {
                                    OccurrenceModel errorE001 = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() +
                                            " stop_time_update " + stopTimeUpdate.getStopSequence() +
                                            " arrival time " + stopTimeUpdate.getDeparture().getTime());
                                    e001List.add(errorE001);
                                    _log.debug(errorE001.getPrefix() + " " + E001.getOccurrenceSuffix());
                                }
                            }
                        }
                    }
                }
            }

            /**
             * Validate VehiclePosition timestamps - W001, E001, E012
             */
            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition vehiclePosition = entity.getVehicle();
                long vehicleTimestamp = vehiclePosition.getTimestamp();

                if (vehicleTimestamp == 0) {
                    OccurrenceModel errorW001 = new OccurrenceModel("vehicle_id " + vehiclePosition.getVehicle().getId());
                    w001List.add(errorW001);
                    _log.debug(errorW001.getPrefix() + " " + W001.getOccurrenceSuffix());
                } else {
                    if (headerTimestamp != 0 && vehicleTimestamp > headerTimestamp) {
                        OccurrenceModel errorE012 = new OccurrenceModel("vehicle_id " + vehiclePosition.getVehicle().getId() + " timestamp " + vehicleTimestamp);
                        e012List.add(errorE012);
                        _log.debug(errorE012.getPrefix() + " " + E012.getOccurrenceSuffix());
                    }
                    if (!isPosix(vehicleTimestamp)) {
                        OccurrenceModel errorE001 = new OccurrenceModel("vehicle_id " + vehiclePosition.getVehicle().getId() + " timestamp " + vehicleTimestamp);
                        e001List.add(errorE001);
                        _log.debug(errorE001.getPrefix() + " " + E001.getOccurrenceSuffix());
                    }
                }
            }

            /**
             * Validate Alert time ranges - E001
             */
            if (entity.hasAlert()) {
                GtfsRealtime.Alert alert = entity.getAlert();
                if (alert != null) {
                    List<GtfsRealtime.TimeRange> activePeriods = alert.getActivePeriodList();
                    if (activePeriods != null) {
                        for (GtfsRealtime.TimeRange range : activePeriods) {
                            if (range.hasStart()) {
                                if (!isPosix(range.getStart())) {
                                    OccurrenceModel errorE001 = new OccurrenceModel("alert in entity " + entity + " active_period.start " + range.getStart());
                                    e001List.add(errorE001);
                                    _log.debug(errorE001.getPrefix() + " " + E001.getOccurrenceSuffix());
                                }
                            }
                            if (range.hasEnd()) {
                                if (!isPosix(range.getEnd())) {
                                    OccurrenceModel errorE001 = new OccurrenceModel("alert in entity " + entity + " active_period.end " + range.getEnd());
                                    e001List.add(errorE001);
                                    _log.debug(errorE001.getPrefix() + " " + E001.getOccurrenceSuffix());
                                }
                            }
                        }
                    }
                }
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!w001List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W001), w001List));
        }
        if (!w007List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W007), w007List));
        }
        if (!e001List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E001), e001List));
        }
        if (!e012List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E012), e012List));
        }
        if (!e017List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E017), e017List));
        }
        if (!e018List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E018), e018List));
        }
        return errors;
    }
}
