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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.util.RuleUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.hsqldb.lib.StringUtil;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.*;

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.E047;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.W003;

/**
 * This class examines multiple GTFS-rt feeds for the same GTFS dataset to identify potential discrepencies in them.
 * It uses the combinedFeedMessage data structure for this, instead of the feedMessage
 *
 * W003 - ID in one feed missing from the other
 * E047 - VehiclePosition and TripUpdate ID pairing mismatch
 */
public class CrossFeedDescriptorValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(CrossFeedDescriptorValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
        if (combinedFeedMessage == null) {
            // If only one GTFS-rt feed is being monitored for the GTFS dataset, then don't run any of the cross-feed rules
            return new ArrayList<>();
        }

        List<OccurrenceModel> w003List = new ArrayList<>();
        List<OccurrenceModel> e047List = new ArrayList<>();

        // key is trip_id from TripUpdates feed, value is vehicle.id
        BiMap<String, String> tripUpdates = HashBiMap.create();
        // A set of trips (key = trip_id) that don't have any vehicle.ids
        Set<String> tripsWithoutVehicles = new HashSet<>();
        int tripUpdateCount = 0;

        // key is vehicle_id from VehiclePositions feed, value is trip_id
        BiMap<String, String> vehiclePositions = HashBiMap.create();
        // A set of vehicles (key = vehicle.id) that don't have any trip_ids
        Set<String> vehiclesWithoutTrips = new HashSet<>();
        int vehiclePositionCount = 0;

        // Build the maps
        for (GtfsRealtime.FeedEntity entity : combinedFeedMessage.getEntityList()) {
            if (entity.hasTripUpdate() && hasTripId(entity.getTripUpdate())) {
                tripUpdateCount++;
                String tripId = entity.getTripUpdate().getTrip().getTripId();
                String vehicleId = "";
                if (entity.getTripUpdate().hasVehicle() && entity.getTripUpdate().getVehicle().hasId()) {
                    vehicleId = entity.getTripUpdate().getVehicle().getId();
                }
                if (StringUtil.isEmpty(vehicleId)) {
                    // Trip does not have a vehicle.id - add it to the set (it can't exist in HashBiMap)
                    tripsWithoutVehicles.add(tripId);
                } else {
                    // Trip has a vehicle.id - add it to the HashBiMap
                    try {
                        tripUpdates.put(tripId, vehicleId);
                    } catch (IllegalArgumentException e) {
                        // TODO - We should log this as error under new rule - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/33
                        _log.error("Error adding trip_id " + tripId + " -> vehicle_id " + vehicleId + " to TripUpdates HashBiMap.  TripUpdate exists twice in feed, or more than one TripUpdate is assigned to the same vehicle. " + e);
                    }
                }

            }
            if (entity.hasVehicle() && hasVehicleId(entity.getVehicle())) {
                vehiclePositionCount++;
                String vehicleId = entity.getVehicle().getVehicle().getId();
                String tripId = "";
                if (entity.getVehicle().hasTrip() && entity.getVehicle().getTrip().hasTripId()) {
                    tripId = entity.getVehicle().getTrip().getTripId();
                }
                if (StringUtil.isEmpty(tripId)) {
                    // Vehicle does not have a trip_id - add it to the set (it can't exist in HashBiMap)
                    vehiclesWithoutTrips.add(vehicleId);
                } else {
                    // Vehicle has a trip_id - add it to the HashBiMap
                    try {
                        vehiclePositions.put(vehicleId, tripId);
                    } catch (IllegalArgumentException e) {
                        // TODO - We should log this as error under new rule - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/38
                        _log.error("Error adding vehicle.id " + vehicleId + " -> trip_id " + tripId + " to VehiclePositions HashBiMap.  Vehicle exists twice in feed, or more than one vehicle is assigned to same trip. " + e);
                    }
                }
            }
        }

        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (tripUpdateCount == 0 || vehiclePositionCount == 0) {
            // We are missing a VehiclePositions or TripUpdates feed, so we can't compare across feeds - return empty list;
            return errors;
        }

        /**
         * Create inverse maps, so we can efficiently check if a trip_id in TripUpdates is in VehiclePositions, and if
         * vehicle_id in VehiclePositions is in TripUpdates.
         *
         * tripUpdatesInverse - A map of vehicle_ids to trip_ids, from the TripUpdates feed
         * vehiclePositionsInverse - A map of trip_ids to vehicle_ids, from the VehiclePositions feed
         *
         * Note that we still need to check vehiclesWithoutTrips and tripsWithoutVehicles, as these trips/vehicles can't exist in HashBiMaps.
         * See https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/241#issuecomment-313194304.
         */
        BiMap<String, String> tripUpdatesInverse = tripUpdates.inverse();
        BiMap<String, String> vehiclePositionsInverse = vehiclePositions.inverse();

        // Check all trips that contained a vehicle
        for (Map.Entry<String, String> trip : tripUpdates.entrySet()) {
            if (!vehiclePositionsInverse.containsKey(trip.getKey())) {
                // W003 - TripUpdates feed has a trip_id that's not in VehiclePositions feed
                RuleUtils.addOccurrence(W003, "trip_id " + trip.getKey() + " is in TripUpdates but not in VehiclePositions feed", w003List, _log);
            }
            if (!vehiclePositions.containsKey(trip.getValue()) && !vehiclesWithoutTrips.contains(trip.getValue())) {
                // W003 - TripUpdates feed has a vehicle_id that's not in VehiclePositions feed
                RuleUtils.addOccurrence(W003, "vehicle_id " + trip.getValue() + " is in TripUpdates but not in VehiclePositions feed", w003List, _log);
            }
            checkE047TripUpdates(trip, vehiclePositionsInverse, e047List);
        }

        // Check all vehicles that contained a trip
        for (Map.Entry<String, String> vehiclePosition : vehiclePositions.entrySet()) {
            if (!tripUpdatesInverse.containsKey(vehiclePosition.getKey())) {
                // W003 - VehiclePositions has a vehicle_id that's not in TripUpdates feed
                RuleUtils.addOccurrence(W003, "vehicle_id " + vehiclePosition.getKey() + " is in VehiclePositions but not in TripUpdates feed", w003List, _log);
            }
            if (!tripUpdates.containsKey(vehiclePosition.getValue()) && !tripsWithoutVehicles.contains(vehiclePosition.getValue())) {
                // W003 - VehiclePositions has a trip_id that's not in the TripUpdates feed
                RuleUtils.addOccurrence(W003, "trip_id " + vehiclePosition.getValue() + " is in VehiclePositions but not in TripUpdates feed", w003List, _log);
            }
            checkE047VehiclePositions(vehiclePosition, tripUpdatesInverse, e047List);
        }

        // Check all trips that did NOT contain a vehicle
        for (String trip_id : tripsWithoutVehicles) {
            if (!vehiclePositionsInverse.containsKey(trip_id)) {
                // W003 - TripUpdates feed has a trip_id that's not in VehiclePositions feed
                RuleUtils.addOccurrence(W003, "trip_id " + trip_id + " is in TripUpdates but not in VehiclePositions feed", w003List, _log);
            }
        }

        // Check all vehicles that did NOT contain a trip
        for (String vehicle_id : vehiclesWithoutTrips) {
            if (!tripUpdatesInverse.containsKey(vehicle_id)) {
                // W003 - VehiclePositions has a vehicle_id that's not in TripUpdates feed
                RuleUtils.addOccurrence(W003, "vehicle_id " + vehicle_id + " is in VehiclePositions but not in TripUpdates feed", w003List, _log);
            }
        }

        if (!w003List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W003), w003List));
        }
        if (!e047List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E047), e047List));
        }
        return errors;
    }

    /**
     * Returns true if this tripUpdate has a trip_id, false if it does not
     *
     * @param tripUpdate to examine
     * @return true if this tripUpdate has a trip_id, false if it does not
     */
    private boolean hasTripId(GtfsRealtime.TripUpdate tripUpdate) {
        return tripUpdate.hasTrip() && tripUpdate.getTrip().hasTripId();
    }

    /**
     * Returns true if this vehiclePosition has a vehicle ID, false if it does not
     *
     * @param vehiclePosition to examine
     * @return true if this vehiclePosition has a vehicle ID, false if it does not
     */
    private boolean hasVehicleId(GtfsRealtime.VehiclePosition vehiclePosition) {
        return vehiclePosition.hasVehicle() && vehiclePosition.getVehicle().hasId();
    }

    /**
     * Checks E036 - "VehiclePosition and TripUpdate ID pairing mismatch" for TripUpdates, and adds a found error to the provided error list.
     *
     * @param trip                    A map entry of a trip_id to a vehicle_id that represents a trip in TripUpdates and a vehicle_id that the trip contains
     * @param vehiclePositionsInverse An inverse map of a VehiclePositions feed, with keys that represent the trip_ids that each VehiclePosition contains, and the value being the VehiclePosition vehicle.id that contains the trip_id
     * @param errors                  the list to add the errors to
     */
    private void checkE047TripUpdates(Map.Entry<String, String> trip, BiMap<String, String> vehiclePositionsInverse, List<OccurrenceModel> errors) {
        String vehiclePositionsVehicleId = vehiclePositionsInverse.get(trip.getKey());
        String tripUpdatesVehicleId = trip.getValue();
        if (!StringUtil.isEmpty(vehiclePositionsVehicleId)) {
            if (!tripUpdatesVehicleId.equals(vehiclePositionsVehicleId)) {
                RuleUtils.addOccurrence(E047, "vehicle_id " + trip.getValue() + " and trip_id " + trip.getKey() + " pairing in TripUpdates does not match vehicle_id " + vehiclePositionsVehicleId + " and trip_id " + trip.getKey() + " pairing in VehiclePositions feed", errors, _log);
            }
        }
    }

    /**
     * Checks E036 - "VehiclePosition and TripUpdate ID pairing mismatch" for VehiclePositions, and adds a found error to the provided error list.
     *
     * @param vehicle            A map entry of a vehicle_id to a trip_id that represents a vehicle in VehiclePositions and the trip_id that the vehicle contains
     * @param tripUpdatesInverse An inverse map of a TripUpdates feed, with keys that represent the vehicle_ids that each TripUpdate contains, and the value being the TripUpdate trip_id that contains the vehicle.id
     * @param errors             the list to add the errors to
     */
    private void checkE047VehiclePositions(Map.Entry<String, String> vehicle, BiMap<String, String> tripUpdatesInverse, List<OccurrenceModel> errors) {
        String tripUpdatesTripId = tripUpdatesInverse.get(vehicle.getKey());
        String vehiclePositionsTripId = vehicle.getValue();
        if (!StringUtil.isEmpty(tripUpdatesTripId)) {
            if (!vehiclePositionsTripId.equals(tripUpdatesTripId)) {
                RuleUtils.addOccurrence(E047, "trip_id " + vehicle.getValue() + " and vehicle_id " + vehicle.getKey() + " pairing in VehiclePositions does not match trip_id " + tripUpdatesTripId + " and vehicle_id " + vehicle.getKey() + " pairing in TripUpdates feed", errors, _log);
            }
        }
    }
}
