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

package edu.usf.cutr.gtfsrtvalidator.lib.validation.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.lib.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.util.RuleUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils.getAge;
import static edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils.isPosix;
import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.*;

/**
 * Implement validation rules related to feed entity timestamps:
 *
 *  W001 - Timestamp not populated
 *  W007 - Refresh interval is more than 35 seconds
 *  W008 - Header timestamp is older than 65 seconds
 *  E001 - Not in POSIX time
 *  E012 - Header timestamp should be greater than or equal to all other timestamps
 *  E017 - GTFS-rt content changed but has the same timestamp
 *  E018 - GTFS-rt header timestamp decreased between two sequential iterations
 *  E022 - trip stop_time_update times are not increasing
 *  E025 - stop_time_update departure time is before arrival time
 *  E048 - header` `timestamp` not populated
 *  E050 - `timestamp` is in the future
 */
public class TimestampValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(TimestampValidator.class);

    private final static long MINIMUM_REFRESH_INTERVAL_SECONDS = 35L;
    private final static long MAX_AGE_SECONDS = 65L; // Maximum allowed age for GTFS-realtime feed, in seconds (W008)
    private final static long IN_FUTURE_TOLERANCE_SECONDS = 60L; // Maximum allowed amount of time for a timetamp to be in the future, in seconds (E050)

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsMutableDao gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
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
        List<OccurrenceModel> e025List = new ArrayList<>();
        List<OccurrenceModel> e048List = new ArrayList<>();
        List<OccurrenceModel> e050List = new ArrayList<>();

        String currentTimeText = TimestampUtils.posixToClock(TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis), gtfsMetadata.getTimeZone());

        /**
         * Validate FeedHeader timestamp
         */
        long headerTimestamp = feedMessage.getHeader().getTimestamp();
        if (headerTimestamp == 0) {
            boolean isV2orHigher = true;
            try {
                isV2orHigher = GtfsUtils.isV2orHigher(feedMessage.getHeader());
            } catch (Exception e) {
                _log.error("Error checking header version for E048/W001, logging as E048: " + e);
            }
            if (isV2orHigher) {
                // E048 - header timestamp not populated
                RuleUtils.addOccurrence(E048, "", e048List, _log);
            } else {
                // W001 - Timestamp not populated
                RuleUtils.addOccurrence(W001, "header", w001List, _log);
            }
        } else {
            if (!isPosix(headerTimestamp)) {
                // E001 - Not in POSIX time
                RuleUtils.addOccurrence(E001, "header.timestamp", e001List, _log);
            } else {
                long ageMillis = getAge(currentTimeMillis, headerTimestamp);
                long ageMinutes = TimeUnit.MILLISECONDS.toMinutes(ageMillis);
                long ageSeconds = TimeUnit.MILLISECONDS.toSeconds(ageMillis);
                if (ageMillis > TimeUnit.SECONDS.toMillis(MAX_AGE_SECONDS)) {
                    // W008 - Header timestamp is older than 65 seconds
                    RuleUtils.addOccurrence(W008, "header.timestamp is " + ageMinutes + " min " + ageSeconds % 60 + " sec", w008List, _log);
                }
                if (TimestampUtils.isInFuture(currentTimeMillis, headerTimestamp, IN_FUTURE_TOLERANCE_SECONDS)) {
                    // E050 - timestamp is in the future
                    String headerTimestampText = TimestampUtils.posixToClock(headerTimestamp, gtfsMetadata.getTimeZone());
                    RuleUtils.addOccurrence(E050, "header.timestamp " + headerTimestampText + " (" + headerTimestamp + ") is " + Math.abs(ageMinutes) + " min " + Math.abs(ageSeconds) % 60 + " sec greater than " + currentTimeText + " (" + currentTimeMillis + ")", e050List, _log);
                }
            }

            if (previousFeedMessage != null && previousFeedMessage.getHeader().getTimestamp() != 0) {
                long previousTimestamp = previousFeedMessage.getHeader().getTimestamp();
                long interval = headerTimestamp - previousTimestamp;
                if (headerTimestamp == previousTimestamp) {
                    // E017 - GTFS-rt content changed but has the same timestamp
                    RuleUtils.addOccurrence(E017, "header.timestamp of " + headerTimestamp, e017List, _log);
                } else if (headerTimestamp < previousTimestamp) {
                    // E018 - GTFS-rt header timestamp decreased between two sequential iterations
                    String prefix = "header.timestamp of " + headerTimestamp + " is less than the header.timestamp of " + previousFeedMessage.getHeader().getTimestamp();
                    RuleUtils.addOccurrence(E018, prefix, e018List, _log);
                } else if (interval > MINIMUM_REFRESH_INTERVAL_SECONDS) {
                    // W007 - Refresh interval is more than 35 seconds
                    RuleUtils.addOccurrence(W007, interval + " second interval between consecutive header.timestamps", w007List, _log);
                }
            }
        }

        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                long tripUpdateTimestamp = tripUpdate.getTimestamp();

                /**
                 * Validate TripUpdate timestamps
                 */
                String id = GtfsUtils.getTripId(entity, tripUpdate);
                if (tripUpdateTimestamp == 0) {
                    // W001 - Timestamp not populated
                    RuleUtils.addOccurrence(W001, id, w001List, _log);
                } else {
                    if (headerTimestamp != 0 && tripUpdateTimestamp > headerTimestamp) {
                        // E012 - Header timestamp should be greater than or equal to all other timestamps
                        RuleUtils.addOccurrence(E012, id + " timestamp " + tripUpdateTimestamp, e012List, _log);
                    }
                    if (!isPosix(tripUpdateTimestamp)) {
                        // E001 - Not in POSIX time
                        RuleUtils.addOccurrence(E001, id + " timestamp " + tripUpdateTimestamp, e001List, _log);
                    } else {
                        if (TimestampUtils.isInFuture(currentTimeMillis, tripUpdateTimestamp, IN_FUTURE_TOLERANCE_SECONDS)) {
                            // E050 - timestamp is in the future
                            long ageMillis = getAge(currentTimeMillis, tripUpdateTimestamp);
                            long ageMinutes = Math.abs(TimeUnit.MILLISECONDS.toMinutes(ageMillis));
                            long ageSeconds = Math.abs(TimeUnit.MILLISECONDS.toSeconds(ageMillis));
                            String tripUpdateTimestampText = TimestampUtils.posixToClock(tripUpdateTimestamp, gtfsMetadata.getTimeZone());
                            RuleUtils.addOccurrence(E050, id + " timestamp " + tripUpdateTimestampText + " (" + tripUpdateTimestamp + ") is " + ageMinutes + " min " + ageSeconds % 60 + " sec greater than " + currentTimeText + " (" + currentTimeMillis + ")", e050List, _log);
                        }
                    }
                }

                /**
                 * Validate TripUpdate StopTimeUpdate times
                 */
                List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdates = tripUpdate.getStopTimeUpdateList();
                if (stopTimeUpdates != null) {
                    Long previousArrivalTime = null;
                    String previousArrivalTimeText = null;
                    Long previousDepartureTime = null;
                    String previousDepartureTimeText = null;
                    for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdates) {
                        String stopDescription = stopTimeUpdate.hasStopSequence() ? " stop_sequence " + stopTimeUpdate.getStopSequence() : " stop_id " + stopTimeUpdate.getStopId();
                        Long arrivalTime = null;
                        String arrivalTimeText;
                        Long departureTime = null;
                        String departureTimeText;
                        if (stopTimeUpdate.hasArrival()) {
                            if (stopTimeUpdate.getArrival().hasTime()) {
                                arrivalTime = stopTimeUpdate.getArrival().getTime();
                                arrivalTimeText = TimestampUtils.posixToClock(arrivalTime, gtfsMetadata.getTimeZone());

                                if (!isPosix(arrivalTime)) {
                                    // E001 - Not in POSIX time
                                    RuleUtils.addOccurrence(E001, id + stopDescription + " arrival_time " + arrivalTime, e001List, _log);
                                }
                                if (previousArrivalTime != null && arrivalTime < previousArrivalTime) {
                                    // E022 - this stop arrival time is < previous stop arrival time
                                    String prefix = id + stopDescription +
                                            " arrival_time " + arrivalTimeText + " (" + arrivalTime + ") is less than previous stop arrival_time " + previousArrivalTimeText + " (" + previousArrivalTime + ")";
                                    RuleUtils.addOccurrence(E022, prefix, e022List, _log);
                                }
                                if (previousArrivalTime != null && Objects.equals(arrivalTime, previousArrivalTime)) {
                                    // E022 - this stop arrival time is == previous stop arrival time
                                    String prefix = id + stopDescription + " arrival_time " + arrivalTimeText + " (" + arrivalTime + ") is equal to previous stop arrival_time " + previousArrivalTimeText + " (" + previousArrivalTime + ")";
                                    RuleUtils.addOccurrence(E022, prefix, e022List, _log);
                                }
                                if (previousDepartureTime != null && arrivalTime < previousDepartureTime) {
                                    // E022 - this stop arrival time is < previous stop departure time
                                    String prefix = id + stopDescription + " arrival_time " + arrivalTimeText + " (" + arrivalTime + ") is less than previous stop departure_time " + previousDepartureTimeText + " (" + previousDepartureTime + ")";
                                    RuleUtils.addOccurrence(E022, prefix, e022List, _log);
                                }
                                if (previousDepartureTime != null && Objects.equals(arrivalTime, previousDepartureTime)) {
                                    // E022 - this stop arrival time is == previous stop departure time
                                    String prefix = id + stopDescription + " arrival_time " + arrivalTimeText + " (" + arrivalTime + ") is equal to previous stop departure_time " + previousDepartureTimeText + " (" + previousDepartureTime + ")";
                                    RuleUtils.addOccurrence(E022, prefix, e022List, _log);
                                }
                            }
                        }

                        if (stopTimeUpdate.hasDeparture()) {
                            if (stopTimeUpdate.getDeparture().hasTime()) {
                                departureTime = stopTimeUpdate.getDeparture().getTime();
                                departureTimeText = TimestampUtils.posixToClock(departureTime, gtfsMetadata.getTimeZone());

                                if (!isPosix(departureTime)) {
                                    // E001 - Not in POSIX time
                                    RuleUtils.addOccurrence(E001, id + stopDescription + " departure_time " + departureTime, e001List, _log);
                                }
                                if (previousDepartureTime != null && departureTime < previousDepartureTime) {
                                    // E022 - this stop departure time is < previous stop departure time
                                    String prefix = id + stopDescription + " departure_time " + departureTimeText + " (" + departureTime + ") is less than previous stop departure_time " + previousDepartureTimeText + " (" + previousDepartureTime + ")";
                                    RuleUtils.addOccurrence(E022, prefix, e022List, _log);
                                }
                                if (previousDepartureTime != null && Objects.equals(departureTime, previousDepartureTime)) {
                                    // E022 - this stop departure time is == previous stop departure time
                                    String prefix = id + stopDescription + " departure_time " + departureTimeText + " (" + departureTime + ") is equal to previous stop departure_time " + previousDepartureTimeText + " (" + previousDepartureTime + ")";
                                    RuleUtils.addOccurrence(E022, prefix, e022List, _log);
                                }
                                if (previousArrivalTime != null && departureTime < previousArrivalTime) {
                                    // E022 - this stop departure time is < previous stop arrival time
                                    String prefix = id + stopDescription + " departure_time " + departureTimeText + " (" + departureTime + ") is less than previous stop arrival_time " + previousArrivalTimeText + " (" + previousArrivalTime + ")";
                                    RuleUtils.addOccurrence(E022, prefix, e022List, _log);
                                }
                                if (previousArrivalTime != null && Objects.equals(departureTime, previousArrivalTime)) {
                                    // E022 - this stop departure time is == previous stop arrival time
                                    String prefix = id + stopDescription + " departure_time " + departureTimeText + " (" + departureTime + ") is equal to previous stop arrival_time " + previousArrivalTimeText + " (" + previousArrivalTime + ")";
                                    RuleUtils.addOccurrence(E022, prefix, e022List, _log);
                                }
                                if (stopTimeUpdate.getArrival().hasTime() && departureTime < stopTimeUpdate.getArrival().getTime()) {
                                    // E025 - stop_time_update departure time is before arrival time
                                    String prefix = id + stopDescription + " departure_time " + departureTimeText
                                            + " (" + departureTime + ") is less than the same stop arrival_time " +
                                            TimestampUtils.posixToClock(stopTimeUpdate.getArrival().getTime(), gtfsMetadata.getTimeZone())
                                            + " (" + stopTimeUpdate.getArrival().getTime() + ")";
                                    RuleUtils.addOccurrence(E025, prefix, e025List, _log);
                                }
                            }
                        }
                        if (arrivalTime != null) {
                            previousArrivalTime = arrivalTime;
                            previousArrivalTimeText = TimestampUtils.posixToClock(previousArrivalTime, gtfsMetadata.getTimeZone());
                        }
                        if (departureTime != null) {
                            previousDepartureTime = departureTime;
                            previousDepartureTimeText = TimestampUtils.posixToClock(previousDepartureTime, gtfsMetadata.getTimeZone());
                        }
                    }
                }
            }

            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition vehiclePosition = entity.getVehicle();
                long vehicleTimestamp = vehiclePosition.getTimestamp();

                if (vehicleTimestamp == 0) {
                    // W001 - Timestamp not populated
                    RuleUtils.addOccurrence(W001, "vehicle_id " + vehiclePosition.getVehicle().getId(), w001List, _log);
                } else {
                    String prefix = "vehicle_id " + vehiclePosition.getVehicle().getId() + " timestamp " + vehicleTimestamp;
                    if (headerTimestamp != 0 && vehicleTimestamp > headerTimestamp) {
                        // E012 - Header timestamp should be greater than or equal to all other timestamps
                        RuleUtils.addOccurrence(E012, prefix, e012List, _log);
                    }
                    if (!isPosix(vehicleTimestamp)) {
                        // E001 - Not in POSIX time
                        RuleUtils.addOccurrence(E001, prefix, e001List, _log);
                    } else {
                        if (TimestampUtils.isInFuture(currentTimeMillis, vehicleTimestamp, IN_FUTURE_TOLERANCE_SECONDS)) {
                            // E050 - timestamp is in the future
                            long ageMillis = getAge(currentTimeMillis, vehicleTimestamp);
                            long ageMinutes = Math.abs(TimeUnit.MILLISECONDS.toMinutes(ageMillis));
                            long ageSeconds = Math.abs(TimeUnit.MILLISECONDS.toSeconds(ageMillis));
                            String vehicleTimestampText = TimestampUtils.posixToClock(vehicleTimestamp, gtfsMetadata.getTimeZone());
                            RuleUtils.addOccurrence(E050, "vehicle_id " + vehiclePosition.getVehicle().getId() + " timestamp " + vehicleTimestampText + " (" + vehicleTimestamp + ") is " + ageMinutes + " min " + ageSeconds % 60 + " sec greater than " + currentTimeText + " (" + currentTimeMillis + ")", e050List, _log);
                        }
                    }
                }
            }

            if (entity.hasAlert()) {
                checkAlertE001(entity, e001List);
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
        if (!e025List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E025), e025List));
        }
        if (!e048List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E048), e048List));
        }
        if (!e050List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E050), e050List));
        }
        return errors;
    }

    /**
     * Validate Alert time ranges - E001
     *
     * @param entity entity that has alerts to check
     * @param errors list to which any errors can be added
     */
    private void checkAlertE001(GtfsRealtime.FeedEntity entity, List<OccurrenceModel> errors) {
        GtfsRealtime.Alert alert = entity.getAlert();
        List<GtfsRealtime.TimeRange> activePeriods = alert.getActivePeriodList();
        if (activePeriods != null) {
            for (GtfsRealtime.TimeRange range : activePeriods) {
                if (range.hasStart()) {
                    if (!isPosix(range.getStart())) {
                        RuleUtils.addOccurrence(E001, "alert in entity " + entity.getId() + " active_period.start " + range.getStart(), errors, _log);
                    }
                }
                if (range.hasEnd()) {
                    if (!isPosix(range.getEnd())) {
                        RuleUtils.addOccurrence(E001, "alert in entity " + entity.getId() + " active_period.end " + range.getEnd(), errors, _log);
                    }
                }
            }
        }
    }
}
