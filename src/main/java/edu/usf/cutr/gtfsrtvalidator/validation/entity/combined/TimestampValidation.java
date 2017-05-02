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
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.getAge;
import static edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils.isPosix;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;

/**
 * Implement validation rules related to feed entity timestamps:
 *  W001 - Timestamp not populated
 *  E001 - Not in POSIX time
 *  E012 - Header timestamp should be greater than or equal to all other timestamps
 *  E017 - GTFS-rt content changed but has the same timestamp
 *  E018 - GTFS-rt header timestamp decreased between two sequential iterations
 *  E022 - trip stop_time_update times are not increasing
 */
public class TimestampValidation implements FeedEntityValidator{

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(TimestampValidation.class);

    private static long MINIMUM_REFRESH_INTERVAL_SECONDS = 35L;
    public static long MAX_AGE_SECONDS = 65L; // Maximum allowed age for GTFS-realtime feed, in seconds (W008)

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        if (feedMessage.equals(previousFeedMessage)) {
            throw new IllegalArgumentException("feedMessage and previousFeedMessage must not be the same");
        }
        List<OccurrenceModel> w001List = new ArrayList<>();
        List<OccurrenceModel> w007List = new ArrayList<>();
        List<OccurrenceModel> w008List = new ArrayList<>();
        List<OccurrenceModel> e001List = new ArrayList<>();
        List<OccurrenceModel> e012List = new ArrayList<>();
        List<OccurrenceModel> e017List = new ArrayList<>();
        List<OccurrenceModel> e018List = new ArrayList<>();
        List<OccurrenceModel> e022List = new ArrayList<>();

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
            } else {
                long age = getAge(currentTimeMillis, headerTimestamp);
                if (age > TimeUnit.SECONDS.toMillis(MAX_AGE_SECONDS)) {
                    // W008
                    long ageMinutes = TimeUnit.MILLISECONDS.toMinutes(age);
                    long ageSeconds = TimeUnit.MILLISECONDS.toSeconds(age);
                    OccurrenceModel om = new OccurrenceModel(String.format("header.timestamp is " + ageMinutes + " min " + ageSeconds % 60 + " sec"));
                    w008List.add(om);
                    _log.debug(om.getPrefix() + " " + W008.getOccurrenceSuffix());
                }
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
                 * Validate TripUpdate StopTimeUpdate times
                 */
                List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdates = tripUpdate.getStopTimeUpdateList();
                if (stopTimeUpdates != null) {

                    Long previousArrivalTime = null;
                    Long previousDepartureTime = null;
                    for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdates) {
                        String stopDescription = stopTimeUpdate.hasStopSequence() ? " stop_sequence " + stopTimeUpdate.getStopSequence() : " stop_id " + stopTimeUpdate.getStopId();
                        Long arrivalTime = null;
                        Long departureTime = null;
                        if (stopTimeUpdate.hasArrival()) {
                            if (stopTimeUpdate.getArrival().hasTime()) {
                                arrivalTime = stopTimeUpdate.getArrival().getTime();

                                if (!isPosix(arrivalTime)) {
                                    // E001
                                    OccurrenceModel errorE001 = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() +
                                            stopDescription + " arrival_time " + arrivalTime);
                                    e001List.add(errorE001);
                                    _log.debug(errorE001.getPrefix() + " " + E001.getOccurrenceSuffix());
                                }
                                if (previousArrivalTime != null && arrivalTime <= previousArrivalTime) {
                                    // E022 - this stop arrival time is <= previous stop arrival time
                                    OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() +
                                            stopDescription + " arrival_time " + arrivalTime + " is less than or equal to previous stop arrival_time " + previousArrivalTime);
                                    e022List.add(om);
                                    _log.debug(om.getPrefix() + " " + E022.getOccurrenceSuffix());
                                }
                                if (previousDepartureTime != null && arrivalTime <= previousDepartureTime) {
                                    // E022 - this stop arrival time is <= previous stop departure time
                                    OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() +
                                            stopDescription + " arrival_time " + arrivalTime + " is less than or equal to previous stop departure_time " + previousDepartureTime);
                                    e022List.add(om);
                                    _log.debug(om.getPrefix() + " " + E022.getOccurrenceSuffix());
                                }
                            }
                        }

                        if (stopTimeUpdate.hasDeparture()) {
                            if (stopTimeUpdate.getDeparture().hasTime()) {
                                departureTime = stopTimeUpdate.getDeparture().getTime();

                                if (!isPosix(departureTime)) {
                                    // E001
                                    OccurrenceModel errorE001 = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() +
                                            stopDescription + " departure_time " + departureTime);
                                    e001List.add(errorE001);
                                    _log.debug(errorE001.getPrefix() + " " + E001.getOccurrenceSuffix());
                                }
                                if (previousDepartureTime != null && departureTime <= previousDepartureTime) {
                                    // E022 - this stop departure time is <= previous stop departure time
                                    OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() +
                                            stopDescription + " departure_time " + departureTime + " is less than or equal to previous stop departure_time " + previousDepartureTime);
                                    e022List.add(om);
                                    _log.debug(om.getPrefix() + " " + E022.getOccurrenceSuffix());
                                }
                                if (previousArrivalTime != null && departureTime <= previousArrivalTime) {
                                    // E022 - this stop departure time is <= previous stop arrival time
                                    OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() +
                                            stopDescription + " departure_time " + departureTime + " is less than or equal to previous stop arrival_time " + previousArrivalTime);
                                    e022List.add(om);
                                    _log.debug(om.getPrefix() + " " + E022.getOccurrenceSuffix());
                                }
                                if (stopTimeUpdate.getArrival().hasTime() && departureTime < stopTimeUpdate.getArrival().getTime()) {
                                    // E022 - this stop departure time is before the same stop arrival time
                                    OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() +
                                            stopDescription + " departure_time " + departureTime + " is less than the same stop arrival_time " + stopTimeUpdate.getArrival().getTime());
                                    e022List.add(om);
                                    _log.debug(om.getPrefix() + " " + E022.getOccurrenceSuffix());
                                }
                            }
                        }
                        if (arrivalTime != null) {
                            previousArrivalTime = arrivalTime;
                        }
                        if (departureTime != null) {
                            previousDepartureTime = departureTime;
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
        if (!w008List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W008), w008List));
        }
        if (!e001List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E001), e001List));
        }
        if (!e012List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E012), e012List));
        }
        if (!e017List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E017), e017List));
        }
        if (!e018List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E018), e018List));
        }
        if (!e022List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E022), e022List));
        }
        return errors;
    }
}
