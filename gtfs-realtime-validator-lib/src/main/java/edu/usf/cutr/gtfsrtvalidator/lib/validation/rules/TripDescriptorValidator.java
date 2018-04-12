/*
 * Copyright (C) 2017 University of South Florida.
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
import org.apache.commons.lang3.StringUtils;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.*;

/**
 * E003 - All trip_ids provided in the GTFS-rt feed must appear in the GTFS data
 * (unless schedule_relationship is ADDED)
 *
 * E004 - All route_ids provided in the GTFS-rt feed must appear in the GTFS data
 *
 * E016 - trip_ids with schedule_relationship ADDED must not be in GTFS data
 *
 * E020 - Invalid start_time format
 *
 * E021 - Invalid start_date format
 *
 * E023 - start_time does not match GTFS initial arrival_time
 *
 * E024 - trip direction_id does not match GTFS data
 *
 * E030 - Alert trip_id does not belong to alert route_id
 *
 * E031 - Alert informed_entity.route_id does not match informed_entity.trip.route_id
 *
 * E032 - Alert does not have an informed_entity
 *
 * E033 - Alert informed_entity does not have any specifiers
 *
 * E034 - GTFS-rt agency_id does not exist in GTFS data
 *
 * E035 - GTFS-rt trip.trip_id does not belong to GTFS-rt trip.route_id in GTFS trips.txt
 *
 * W006 - trip_update missing trip_id
 *
 * W009 - schedule_relationship not populated
 */
public class TripDescriptorValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(TripDescriptorValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsMutableDao gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
        List<OccurrenceModel> errorListE003 = new ArrayList<>();
        List<OccurrenceModel> errorListE004 = new ArrayList<>();
        List<OccurrenceModel> errorListE016 = new ArrayList<>();
        List<OccurrenceModel> errorListE020 = new ArrayList<>();
        List<OccurrenceModel> errorListE021 = new ArrayList<>();
        List<OccurrenceModel> errorListE023 = new ArrayList<>();
        List<OccurrenceModel> errorListE024 = new ArrayList<>();
        List<OccurrenceModel> errorListE030 = new ArrayList<>();
        List<OccurrenceModel> errorListE031 = new ArrayList<>();
        List<OccurrenceModel> errorListE032 = new ArrayList<>();
        List<OccurrenceModel> errorListE033 = new ArrayList<>();
        List<OccurrenceModel> errorListE034 = new ArrayList<>();
        List<OccurrenceModel> errorListE035 = new ArrayList<>();
        List<OccurrenceModel> errorListW006 = new ArrayList<>();
        List<OccurrenceModel> errorListW009 = new ArrayList<>();

        // Check the route_id values against the values from the GTFS feed
        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                if (!tripUpdate.getTrip().hasTripId()) {
                    checkW006(entity, tripUpdate.getTrip(), errorListW006);
                } else {
                    String tripId = tripUpdate.getTrip().getTripId();
                    Trip trip = gtfsMetadata.getTrips().get(tripId);
                    if (trip == null) {
                        if (!GtfsUtils.isAddedTrip(tripUpdate.getTrip())) {
                            // Trip isn't in GTFS data and isn't an ADDED trip - E003
                            RuleUtils.addOccurrence(E003, GtfsUtils.getTripId(entity, tripUpdate), errorListE003, _log);
                        }
                    } else {
                        if (GtfsUtils.isAddedTrip(tripUpdate.getTrip())) {
                            // Trip is in GTFS data and is an ADDED trip - E016
                            RuleUtils.addOccurrence(E016, GtfsUtils.getTripId(entity, tripUpdate), errorListE016, _log);
                        }
                        if (tripUpdate.getTrip().hasStartTime()) {
                            checkE023(tripUpdate, tripUpdate.getTrip(), gtfsMetadata, errorListE023);
                        }
                    }
                }

                if (tripUpdate.getTrip().hasStartTime()) {
                    checkE020(tripUpdate, tripUpdate.getTrip(), errorListE020);
                }

                checkE021(tripUpdate, tripUpdate.getTrip(), errorListE021);
                checkE004(tripUpdate, tripUpdate.getTrip(), gtfsMetadata, errorListE004);
                checkE024(tripUpdate, tripUpdate.getTrip(), gtfsMetadata, errorListE024);
                checkE035(entity, tripUpdate.getTrip(), gtfsMetadata, errorListE035);

                boolean foundW009 = false;
                List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdateList = tripUpdate.getStopTimeUpdateList();
                for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdateList) {
                    // Only flag one occurrence of W009 for stop_time_update per trip to avoid flooding the database
                    if (!foundW009) {
                        checkW009(entity, stopTimeUpdate, errorListW009);
                        if (!errorListW009.isEmpty()) {
                            foundW009 = true;
                        }
                    }
                }
                if (tripUpdate.hasTrip()) {
                    checkW009(entity, tripUpdate.getTrip(), errorListW009);
                }
            }
            if (entity.hasVehicle() && entity.getVehicle().hasTrip()) {
                GtfsRealtime.TripDescriptor trip = entity.getVehicle().getTrip();
                if (!trip.hasTripId()) {
                    checkW006(entity, trip, errorListW006);
                } else {
                    String tripId = trip.getTripId();
                    if (!StringUtils.isEmpty(tripId)) {
                        Trip gtfsTrip = gtfsMetadata.getTrips().get(tripId);
                        if (gtfsTrip == null) {
                            if (!GtfsUtils.isAddedTrip(trip)) {
                                // E003 - Trip isn't in GTFS data and isn't an ADDED trip
                                RuleUtils.addOccurrence(E003, "vehicle_id " + entity.getVehicle().getVehicle().getId() + " trip_id " + tripId, errorListE003, _log);
                            }
                        } else {
                            if (GtfsUtils.isAddedTrip(trip)) {
                                // E016 - Trip is in GTFS data and is an ADDED trip
                                RuleUtils.addOccurrence(E016, "vehicle_id " + entity.getVehicle().getVehicle().getId() + " trip_id " + tripId, errorListE016, _log);
                            }
                            if (trip.hasStartTime()) {
                                checkE023(entity.getVehicle(), trip, gtfsMetadata, errorListE023);
                            }
                        }
                    }
                }

                if (trip.hasStartTime()) {
                    checkE020(entity.getVehicle(), trip, errorListE020);
                }

                checkE004(entity.getVehicle(), trip, gtfsMetadata, errorListE004);
                checkE021(entity.getVehicle(), trip, errorListE021);
                checkE024(entity.getVehicle(), trip, gtfsMetadata, errorListE024);
                checkE035(entity, trip, gtfsMetadata, errorListE035);
                checkW009(entity, trip, errorListW009);
            }
            if (entity.hasAlert()) {
                GtfsRealtime.Alert alert = entity.getAlert();
                List<GtfsRealtime.EntitySelector> entitySelectors = alert.getInformedEntityList();
                if (entitySelectors != null && entitySelectors.size() > 0) {
                    for (GtfsRealtime.EntitySelector entitySelector : entitySelectors) {
                        checkE033(entity, entitySelector, errorListE033);
                        checkE034(entity, entitySelector, gtfsMetadata, errorListE034);
                        checkE035(entity, entitySelector.getTrip(), gtfsMetadata, errorListE035);
                        if (entitySelector.hasRouteId() && entitySelector.hasTrip()) {
                            checkE030(entity, entitySelector, gtfsMetadata, errorListE030);
                            checkE031(entity, entitySelector, errorListE031);
                        }
                        if (entitySelector.hasTrip()) {
                            checkW006(entity, entitySelector.getTrip(), errorListW006);
                            checkW009(entity, entitySelector.getTrip(), errorListW009);
                        }
                    }
                } else {
                    // E032 - Alert does not have an informed_entity
                    RuleUtils.addOccurrence(E032, "alert ID " + entity.getId() + " does not have an informed_entity", errorListE032, _log);
                }
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorListE003.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E003), errorListE003));
        }
        if (!errorListE004.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E004), errorListE004));
        }
        if (!errorListE016.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E016), errorListE016));
        }
        if (!errorListE020.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E020), errorListE020));
        }
        if (!errorListE021.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E021), errorListE021));
        }
        if (!errorListE023.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E023), errorListE023));
        }
        if (!errorListE024.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E024), errorListE024));
        }
        if (!errorListE030.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E030), errorListE030));
        }
        if (!errorListE031.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E031), errorListE031));
        }
        if (!errorListE032.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E032), errorListE032));
        }
        if (!errorListE033.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E033), errorListE033));
        }
        if (!errorListE034.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E034), errorListE034));
        }
        if (!errorListE035.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E035), errorListE035));
        }
        if (!errorListW006.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W006), errorListW006));
        }
        if (!errorListW009.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W009), errorListW009));
        }
        return errors;
    }


    /**
     * Checks rule E004 - "All route_ids provided in the GTFS-rt feed must appear in the GTFS data", and adds any errors
     * that are found to the provided error list
     *
     * @param entity       The VehiclePosition or TripUpdate that contains the data to be evaluated for rule E004
     * @param trip         The TripDescriptor be evaluated for rule E004
     * @param gtfsMetadata metadata for the static GTFS data
     * @param errors       list to add any errors for E004 to
     */
    private void checkE004(Object entity, GtfsRealtime.TripDescriptor trip, GtfsMetadata gtfsMetadata, List<OccurrenceModel> errors) {
        String routeId = trip.getRouteId();
        if (!StringUtils.isEmpty(routeId) && !gtfsMetadata.getRouteIds().contains(routeId)) {
            RuleUtils.addOccurrence(E004, GtfsUtils.getVehicleAndRouteId(entity), errors, _log);
        }
    }

    /**
     * Checks rule E020 - "Invalid start_time format", and adds any errors that are found to the provided error list
     *
     * @param entity The VehiclePosition or TripUpdate that contains the data to be evaluated for rule E020
     * @param trip   The TripDescriptor be evaluated for rule E020
     * @param errors list to add any errors for E020 to
     */
    private void checkE020(Object entity, GtfsRealtime.TripDescriptor trip, List<OccurrenceModel> errors) {
        String startTime = trip.getStartTime();
        if (!TimestampUtils.isValidTimeFormat(startTime)) {
            RuleUtils.addOccurrence(E020, GtfsUtils.getVehicleAndTripIdText(entity) + " start_time is " + startTime, errors, _log);
        }
    }

    /**
     * Checks rule E023 - "start_time does not match GTFS initial arrival_time", and adds any errors that are found to the provided error list
     *
     * @param entity The VehiclePosition or TripUpdate that contains the data to be evaluated for rule E023
     * @param trip   The TripDescriptor be evaluated for rule E023
     * @param errors list to add any errors for E023 to
     */
    private void checkE023(Object entity, GtfsRealtime.TripDescriptor trip, GtfsMetadata gtfsMetadata, List<OccurrenceModel> errors) {
        String startTime = trip.getStartTime();
        String tripId = trip.getTripId();
        if (tripId != null && !gtfsMetadata.getExactTimesZeroTripIds().contains(tripId) && !gtfsMetadata.getExactTimesOneTrips().containsKey(tripId)) {
            // Trip is a normal (not frequencies.txt) trip
            List<StopTime> stopTimes = gtfsMetadata.getTripStopTimes().get(tripId);
            if (stopTimes == null || stopTimes.isEmpty()) {
                // There isn't a trip in GTFS trips.txt for this trip, or it doesn't have any records in GTFS stop_times.txt
                return;
            }
            int firstArrivalTime = stopTimes.get(0).getArrivalTime();
            String formattedArrivalTime = TimestampUtils.secondsAfterMidnightToClock(firstArrivalTime);
            if (!startTime.equals(formattedArrivalTime)) {
                String prefix = "GTFS-rt " + GtfsUtils.getVehicleAndTripIdText(entity) + " start_time is " + startTime + " and GTFS initial arrival_time is " + formattedArrivalTime;
                RuleUtils.addOccurrence(E023, prefix, errors, _log);
            }
        }
    }

    /**
     * Checks rule E021 - "Invalid start_date format", and adds any errors that are found to the provided error list
     *
     * @param entity The VehiclePosition or TripUpdate that contains the data to be evaluated for rule E021
     * @param trip   The TripDescriptor be evaluated for rule E021
     * @param errors list to add any errors for E021 to
     */
    private void checkE021(Object entity, GtfsRealtime.TripDescriptor trip, List<OccurrenceModel> errors) {
        if (trip.hasStartDate()) {
            if (!TimestampUtils.isValidDateFormat(trip.getStartDate())) {
                RuleUtils.addOccurrence(E021, GtfsUtils.getVehicleAndTripIdText(entity) + " start_date is " + trip.getStartDate(), errors, _log);
            }
        }
    }

    /**
     * Checks rule E024 - "trip direction_id does not match GTFS data" and adds any errors that are found to the provided error list
     *
     * @param entity       The VehiclePosition or TripUpdate that contains the data to be evaluated for rule E024
     * @param trip         The TripDescriptor be evaluated for rule E024
     * @param gtfsMetadata metadata for the static GTFS data
     * @param errors       list to add any errors for E024 to
     */
    private void checkE024(Object entity, GtfsRealtime.TripDescriptor trip, GtfsMetadata gtfsMetadata, List<OccurrenceModel> errors) {
        if (trip.hasDirectionId()) {
            int directionId = trip.getDirectionId();
            Trip gtfsTrip = gtfsMetadata.getTrips().get(trip.getTripId());
            if (gtfsTrip != null &&
                    (gtfsTrip.getDirectionId() == null || !gtfsTrip.getDirectionId().equals(String.valueOf(directionId)))) {
                String prefix = "GTFS-rt " + GtfsUtils.getVehicleAndTripIdText(entity) + " trip.direction_id is " + directionId + " but GTFS trip.direction_id is " + gtfsTrip.getDirectionId();
                RuleUtils.addOccurrence(E024, prefix, errors, _log);
            }
        }
    }

    /**
     * Checks rule E030 - "GTFS-rt alert trip_id does not belong to GTFS-rt alert route_id in GTFS trips.txt" and adds
     * any errors that are found to the provided errors list
     *
     * @param entity         feed entity to examine that contains an alert
     * @param entitySelector EntitySelector that has both a routeId and a tripDescriptor
     * @param gtfsMetadata   metadata for the static GTFS data
     * @param errors         list to add any errors for E030 to
     */
    private void checkE030(GtfsRealtime.FeedEntity entity, GtfsRealtime.EntitySelector entitySelector, GtfsMetadata gtfsMetadata, List<OccurrenceModel> errors) {
        String routeId = entitySelector.getRouteId();
        GtfsRealtime.TripDescriptor tripDescriptor = entitySelector.getTrip();
        if (tripDescriptor.hasTripId()) {
            Trip gtfsTrip = gtfsMetadata.getTrips().get(tripDescriptor.getTripId());
            if (gtfsTrip != null && !routeId.equals(gtfsTrip.getRoute().getId().getId())) {
                String prefix = "alert ID " + entity.getId() + " informed_entity.trip.trip_id "
                        + tripDescriptor.getTripId() + " does not belong to informed_entity.route_id " + routeId + " (GTFS says it belongs to route_id " + gtfsTrip.getRoute().getId().getId() + ")";
                RuleUtils.addOccurrence(E030, prefix, errors, _log);
            }
        }
    }

    /**
     * Checks rule E031 - "Alert informed_entity.route_id does not match informed_entity.trip.route_id" and adds
     * any errors that are found to the provided errors list
     *
     * @param entity         feed entity to examine that contains an alert
     * @param entitySelector EntitySelector that has both a routeId and a tripDescriptor
     * @param errors         list to add any errors for E031 to
     */
    private void checkE031(GtfsRealtime.FeedEntity entity, GtfsRealtime.EntitySelector entitySelector, List<OccurrenceModel> errors) {
        if (entitySelector.getTrip().hasRouteId()) {
            String routeId = entitySelector.getRouteId();
            if (!entitySelector.getTrip().getRouteId().equals(routeId)) {
                RuleUtils.addOccurrence(E031, "alert ID " + entity.getId() + " informed_entity.route_id " + routeId + " does not equal informed_entity.trip.route_id " + entitySelector.getTrip().getRouteId(), errors, _log);
            }
        }
    }

    /**
     * Checks rule E033 - "Alert informed_entity does not have any specifiers" and adds
     * any errors that are found to the provided errors list
     *
     * @param entity         feed entity to examine that contains an alert
     * @param entitySelector EntitySelector to examine for specifiers
     * @param errors         list to add any errors for E033 to
     */
    private void checkE033(GtfsRealtime.FeedEntity entity, GtfsRealtime.EntitySelector entitySelector, List<OccurrenceModel> errors) {
        GtfsRealtime.TripDescriptor trip = null;
        if (entitySelector.hasTrip()) {
            trip = entitySelector.getTrip();
        }

        if (!entitySelector.hasAgencyId() &&
                !entitySelector.hasRouteId() &&
                !entitySelector.hasRouteType() &&
                !entitySelector.hasStopId()) {
            // informed_entity isn't populated - check TripDescriptor
            if (trip == null ||
                    (!trip.hasTripId() &&
                            !trip.hasRouteId())) {
                RuleUtils.addOccurrence(E033, "alert ID " + entity.getId() + " informed_entity and informed_entity.trip do not not reference any agency, route, trip, or stop", errors, _log);
            }
        }
    }


    /**
     * Checks rule E034 - "GTFS-rt agency_id does not exist in GTFS data", and adds any errors that are found to the provided error list
     *
     * @param entity         feed entity to examine that contains an alert
     * @param entitySelector EntitySelector to examine for agency_id specifier
     * @param gtfsMetadata   information about the GTFS dataset
     * @param errors         list to add any errors for E034 to
     */
    private void checkE034(GtfsRealtime.FeedEntity entity, GtfsRealtime.EntitySelector entitySelector, GtfsMetadata gtfsMetadata, List<OccurrenceModel> errors) {
        if (entitySelector.hasAgencyId()) {
            if (!gtfsMetadata.getAgencyIds().contains(entitySelector.getAgencyId())) {
                RuleUtils.addOccurrence(E034, "alert ID " + entity.getId() + " agency_id " + entitySelector.getAgencyId(), errors, _log);
            }
        }
    }

    /**
     * Checks rule E035 - "GTFS-rt trip.trip_id does not belong to GTFS-rt trip.route_id in GTFS trips.txt", and adds any errors that are found to the provided error list
     *
     * @param entity       entity which contains the specified trip
     * @param trip         trip to examine to see if the trip_id belongs to the route_id
     * @param gtfsMetadata information about the GTFS dataset
     * @param errors       list to add any errors for E034 to
     */
    private void checkE035(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripDescriptor trip, GtfsMetadata gtfsMetadata, List<OccurrenceModel> errors) {
        if (trip.hasTripId() && trip.hasRouteId()) {
            if (!gtfsMetadata.getRouteIds().contains(trip.getRouteId())) {
                // route_id isn't in GTFS data (which will be caught by E004) - return;
                return;
            }
            Trip gtfsTrip = gtfsMetadata.getTrips().get(trip.getTripId());
            if (gtfsTrip == null) {
                // trip_id isn't in GTFS data (which will be caught by E003) - return;
                return;
            }
            String gtfsRouteId = gtfsTrip.getRoute().getId().getId();
            if (!gtfsRouteId.equals(trip.getRouteId())) {
                RuleUtils.addOccurrence(E035, "GTFS-rt entity ID " + entity.getId() + " trip_id " + trip.getTripId() + " has route_id " + trip.getRouteId() + " but belongs to GTFS route_id " + gtfsRouteId, errors, _log);
            }
        }
    }

    /**
     * Checks rule W006 - "trip missing trip_id", and adds any warnings that are found to the provided warning list
     *
     * @param entity         entity which contains the specified trip
     * @param tripDescriptor trip to examine to see if it has trip_id
     * @param warnings       list to add any warnings for W009 to
     */
    private void checkW006(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripDescriptor tripDescriptor, List<OccurrenceModel> warnings) {
        if (tripDescriptor != null && !tripDescriptor.hasTripId()) {
            RuleUtils.addOccurrence(W006, "entity ID " + entity.getId(), warnings, _log);
        }
    }

    /**
     * Checks rule W009 - "schedule_relationship not populated", and adds any warnings that are found to the provided warning list
     *
     * @param entity         entity which contains the specified trip
     * @param tripDescriptor trip to examine to see if it has a schedule_relationship
     * @param warnings       list to add any warnings for W009 to
     */
    private void checkW009(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripDescriptor tripDescriptor, List<OccurrenceModel> warnings) {
        if (tripDescriptor != null && !tripDescriptor.hasScheduleRelationship()) {
            RuleUtils.addOccurrence(W009, GtfsUtils.getTripId(entity, tripDescriptor), warnings, _log);
        }
    }

    /**
     * Checks rule W009 - "schedule_relationship not populated", and adds any warnings that are found to the provided warning list
     *
     * @param entity         entity which contains the specified trip.stop_time_update
     * @param stopTimeUpdate stop_time_update to examine to see if it has a schedule_relationship
     * @param warnings       list to add any warnings for W009 to
     */
    private void checkW009(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, List<OccurrenceModel> warnings) {
        if (stopTimeUpdate != null && !stopTimeUpdate.hasScheduleRelationship()) {
            RuleUtils.addOccurrence(W009, GtfsUtils.getTripId(entity, entity.getTripUpdate().getTrip()) + " " + GtfsUtils.getStopTimeUpdateId(stopTimeUpdate) + " (and potentially more for this trip)", warnings, _log);
        }
    }
}
