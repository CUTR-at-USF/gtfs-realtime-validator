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
package edu.usf.cutr.gtfsrtvalidator.validation.entity.combined;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.hsqldb.lib.StringUtil;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.Trip;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils.isAddedTrip;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;

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
 * W006 - trip_update missing trip_id
 */
public class TripDescriptorValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(TripDescriptorValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
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
        List<OccurrenceModel> errorListW006 = new ArrayList<>();

        // Check the route_id values against the values from the GTFS feed
        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                if (!tripUpdate.getTrip().hasTripId()) {
                    // W006 - No trip_id
                    OccurrenceModel om = new OccurrenceModel("entity ID " + entity.getId());
                    errorListW006.add(om);
                    _log.debug(om.getPrefix() + " " + W006.getOccurrenceSuffix());
                } else {
                    String tripId = tripUpdate.getTrip().getTripId();
                    Trip trip = gtfsMetadata.getTrips().get(tripId);
                    if (trip == null) {
                        if (!isAddedTrip(tripUpdate.getTrip())) {
                            // Trip isn't in GTFS data and isn't an ADDED trip - E003
                            OccurrenceModel om = new OccurrenceModel("trip_id " + tripId);
                            errorListE003.add(om);
                            _log.debug(om.getPrefix() + " " + E003.getOccurrenceSuffix());
                        }
                    } else {
                        if (isAddedTrip(tripUpdate.getTrip())) {
                            // Trip is in GTFS data and is an ADDED trip - E016
                            OccurrenceModel om = new OccurrenceModel("trip_id " + tripId);
                            errorListE016.add(om);
                            _log.debug(om.getPrefix() + " " + E016.getOccurrenceSuffix());
                        }
                    }
                }

                if (tripUpdate.getTrip().hasStartTime()) {
                    checkE020(tripUpdate, tripUpdate.getTrip(), errorListE020);
                    checkE023(tripUpdate, tripUpdate.getTrip(), gtfsMetadata, errorListE023);
                }

                checkE021(tripUpdate, tripUpdate.getTrip(), errorListE021);
                checkE004(tripUpdate, tripUpdate.getTrip(), gtfsMetadata, errorListE004);
                checkE024(tripUpdate, tripUpdate.getTrip(), gtfsMetadata, errorListE024);
            }
            if (entity.hasVehicle() && entity.getVehicle().hasTrip()) {
                GtfsRealtime.TripDescriptor trip = entity.getVehicle().getTrip();
                if (!trip.hasTripId()) {
                    // W006 - No trip_id
                    OccurrenceModel om = new OccurrenceModel("entity ID " + entity.getId());
                    errorListW006.add(om);
                    _log.debug(om.getPrefix() + " " + W006.getOccurrenceSuffix());
                } else {
                    String tripId = trip.getTripId();
                    if (!StringUtil.isEmpty(tripId)) {
                        Trip gtfsTrip = gtfsMetadata.getTrips().get(tripId);
                        if (gtfsTrip == null) {
                            if (!isAddedTrip(trip)) {
                                // Trip isn't in GTFS data and isn't an ADDED trip - E003
                                OccurrenceModel om = new OccurrenceModel("vehicle_id " + entity.getVehicle().getVehicle().getId() + " trip_id " + tripId);
                                errorListE003.add(om);
                                _log.debug(om.getPrefix() + " " + E003.getOccurrenceSuffix());
                            }
                        } else {
                            if (isAddedTrip(trip)) {
                                // Trip is in GTFS data and is an ADDED trip - E016
                                OccurrenceModel om = new OccurrenceModel("vehicle_id " + entity.getVehicle().getVehicle().getId() + " trip_id " + tripId);
                                errorListE016.add(om);
                                _log.debug(om.getPrefix() + " " + E016.getOccurrenceSuffix());
                            }
                        }
                    }
                }

                if (trip.hasStartTime()) {
                    checkE020(entity.getVehicle(), trip, errorListE020);
                    checkE023(entity.getVehicle(), trip, gtfsMetadata, errorListE023);
                }

                checkE004(entity.getVehicle(), trip, gtfsMetadata, errorListE004);
                checkE021(entity.getVehicle(), trip, errorListE021);
                checkE024(entity.getVehicle(), trip, gtfsMetadata, errorListE024);
            }
            if (entity.hasAlert()) {
                GtfsRealtime.Alert alert = entity.getAlert();
                List<GtfsRealtime.EntitySelector> entitySelectors = alert.getInformedEntityList();
                if (entitySelectors != null && entitySelectors.size() > 0) {
                    for (GtfsRealtime.EntitySelector entitySelector : entitySelectors) {
                        checkE033(entity, entitySelector, errorListE033);
                        if (entitySelector.hasRouteId() && entitySelector.hasTrip()) {
                            checkE030(entity, entitySelector, gtfsMetadata, errorListE030);
                            checkE031(entity, entitySelector, errorListE031);
                        }
                    }
                } else {
                    // E032 - Alert does not have an informed_entity
                    OccurrenceModel om = new OccurrenceModel("alert ID " + entity.getId() + " does not have an informed_entity");
                    errorListE032.add(om);
                    _log.debug(om.getPrefix() + " " + E032.getOccurrenceSuffix());
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
        if (!errorListW006.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W006), errorListW006));
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
        if (!StringUtil.isEmpty(routeId) && !gtfsMetadata.getRouteIds().contains(routeId)) {
            OccurrenceModel om = new OccurrenceModel(GtfsUtils.getVehicleAndRouteId(entity));
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E004.getOccurrenceSuffix());
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
            OccurrenceModel om = new OccurrenceModel(GtfsUtils.getVehicleAndTripIdText(entity) + " start_time is " + startTime);
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E020.getOccurrenceSuffix());
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
            int firstArrivalTime = gtfsMetadata.getTripStopTimes().get(tripId).get(0).getArrivalTime();
            String formattedArrivalTime = TimestampUtils.secondsAfterMidnightToClock(firstArrivalTime);
            if (!startTime.equals(formattedArrivalTime)) {
                OccurrenceModel om = new OccurrenceModel("GTFS-rt " + GtfsUtils.getVehicleAndTripIdText(entity) + " start_time is " + startTime + " and GTFS initial arrival_time is " + formattedArrivalTime);
                errors.add(om);
                _log.debug(om.getPrefix() + " " + E023.getOccurrenceSuffix());
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
                // E021 - Invalid start_date format
                OccurrenceModel om = new OccurrenceModel(GtfsUtils.getVehicleAndTripIdText(entity) + " start_date is " + trip.getStartDate());
                errors.add(om);
                _log.debug(om.getPrefix() + " " + E021.getOccurrenceSuffix());
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
                String ids = GtfsUtils.getVehicleAndTripIdText(entity);
                // E024 - trip direction_id does not match GTFS data
                OccurrenceModel om = new OccurrenceModel("GTFS-rt " + ids + " trip.direction_id is " + directionId +
                        " but GTFS trip.direction_id is " + gtfsTrip.getDirectionId());
                errors.add(om);
                _log.debug(om.getPrefix() + " " + E024.getOccurrenceSuffix());
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
                // E030 - Alert trip_id does not belong to alert route_id
                OccurrenceModel om = new OccurrenceModel("alert ID " + entity.getId() + " informed_entity.trip.trip_id "
                        + tripDescriptor.getTripId() + " does not belong to informed_entity.route_id " + routeId + " (GTFS says it belongs to route_id " + gtfsTrip.getRoute().getId().getId() + ")");
                errors.add(om);
                _log.debug(om.getPrefix() + " " + E030.getOccurrenceSuffix());
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
                // E031 - Alert informed_entity.route_id does not match informed_entity.trip.route_id
                OccurrenceModel om = new OccurrenceModel("alert ID " + entity.getId() + " informed_entity.route_id " + routeId + " does not equal informed_entity.trip.route_id " + entitySelector.getTrip().getRouteId());
                errors.add(om);
                _log.debug(om.getPrefix() + " " + E031.getOccurrenceSuffix());
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
                // E033 - Alert informed_entity does not have any specifiers
                OccurrenceModel om = new OccurrenceModel("alert ID " + entity.getId() + " informed_entity and informed_entity.trip do not not reference any agency, route, trip, or stop");
                errors.add(om);
                _log.debug(om.getPrefix() + " " + E033.getOccurrenceSuffix());
            }
        }
    }
}
