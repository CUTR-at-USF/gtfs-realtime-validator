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
import org.apache.commons.lang3.StringUtils;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import java.util.*;

import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.E047;
import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.W003;

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
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsMutableDao gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
        if (combinedFeedMessage == null) {
            // If only one GTFS-rt feed is being monitored for the GTFS dataset, then don't run any of the cross-feed rules
            return new ArrayList<>();
        }

        List<OccurrenceModel> w003List = new ArrayList<>();
        List<OccurrenceModel> e047List = new ArrayList<>();

        /*
          Create inverse maps, so we can efficiently check if a trip_id in TripUpdates is in VehiclePositions, and if
          vehicle_id in VehiclePositions is in TripUpdates.
         */

        // key is trip_id from TripUpdates feed, value is vehicle.id
        HashMap<String, String> tripUpdatesTripIdToVehicleId = new HashMap<>();
        // key is vehicle.id from TripUpdates feed, value is trip_id
        HashMap<String, String> tripUpdatesVehicleIdToTripId = new HashMap<>();
        // A set of trips (key = trip_id) that don't have any vehicle.ids
        Set<String> tripsWithoutVehicles = new HashSet<>();
        int tripUpdateCount = 0;

        // key is vehicle_id from VehiclePositions feed, value is trip_id
        HashMap<String, String> vehiclePositionsVehicleIdToTripId = new HashMap<>();
        // key is trip_id from VehiclePositions feed, value is vehicle_id
        HashMap<String, String> vehiclePositionsTripIdToVehicleId = new HashMap<>();
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
                if (StringUtils.isEmpty(vehicleId)) {
                    // Trip does not have a vehicle.id - add it to the set
                    tripsWithoutVehicles.add(tripId);
                } else {
                    // Trip has a vehicle.id - add it to the HashMap
                    tripUpdatesTripIdToVehicleId.put(tripId, vehicleId);
                    tripUpdatesVehicleIdToTripId.put(vehicleId, tripId);
                    // TODO - New rule - check that there is at most one TripUpdate entity per scheduled trip_id - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/33
                }

            }
            if (entity.hasVehicle() && hasVehicleId(entity.getVehicle())) {
                vehiclePositionCount++;
                String vehicleId = entity.getVehicle().getVehicle().getId();
                String tripId = "";
                if (entity.getVehicle().hasTrip() && entity.getVehicle().getTrip().hasTripId()) {
                    tripId = entity.getVehicle().getTrip().getTripId();
                }
                if (StringUtils.isEmpty(tripId)) {
                    // Vehicle does not have a trip_id - add it to the set
                    vehiclesWithoutTrips.add(vehicleId);
                } else {
                    // Vehicle has a trip_id - add it to the HashMap
                    vehiclePositionsVehicleIdToTripId.put(vehicleId, tripId);
                    vehiclePositionsTripIdToVehicleId.put(tripId, vehicleId);
                    // TODO - New rule - check that there is at most one vehicle assigned each trip - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/38
                }
            }
        }

        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (tripUpdateCount == 0 || vehiclePositionCount == 0) {
            // We are missing a VehiclePositions or TripUpdates feed, so we can't compare across feeds - return empty list;
            return errors;
        }

        // Check all trips that contained a vehicle
        for (Map.Entry<String, String> trip : tripUpdatesTripIdToVehicleId.entrySet()) {
            if (!vehiclePositionsTripIdToVehicleId.containsKey(trip.getKey())) {
                // W003 - TripUpdates feed has a trip_id that's not in VehiclePositions feed
                RuleUtils.addOccurrence(W003, "trip_id " + trip.getKey() + " is in TripUpdates but not in VehiclePositions feed", w003List, _log);
            }
            if (!vehiclePositionsVehicleIdToTripId.containsKey(trip.getValue()) && !vehiclesWithoutTrips.contains(trip.getValue())) {
                // W003 - TripUpdates feed has a vehicle_id that's not in VehiclePositions feed
                RuleUtils.addOccurrence(W003, "vehicle_id " + trip.getValue() + " is in TripUpdates but not in VehiclePositions feed", w003List, _log);
            }
            checkE047TripUpdates(trip, vehiclePositionsTripIdToVehicleId, e047List);
        }

        // Check all vehicles that contained a trip
        for (Map.Entry<String, String> vehiclePosition : vehiclePositionsVehicleIdToTripId.entrySet()) {
            if (!tripUpdatesVehicleIdToTripId.containsKey(vehiclePosition.getKey())) {
                // W003 - VehiclePositions has a vehicle_id that's not in TripUpdates feed
                RuleUtils.addOccurrence(W003, "vehicle_id " + vehiclePosition.getKey() + " is in VehiclePositions but not in TripUpdates feed", w003List, _log);
            }
            if (!tripUpdatesTripIdToVehicleId.containsKey(vehiclePosition.getValue()) && !tripsWithoutVehicles.contains(vehiclePosition.getValue())) {
                // W003 - VehiclePositions has a trip_id that's not in the TripUpdates feed
                RuleUtils.addOccurrence(W003, "trip_id " + vehiclePosition.getValue() + " is in VehiclePositions but not in TripUpdates feed", w003List, _log);
            }
            checkE047VehiclePositions(vehiclePosition, tripUpdatesVehicleIdToTripId, gtfsMetadata, e047List);
        }

        // Check all trips that did NOT contain a vehicle
        for (String trip_id : tripsWithoutVehicles) {
            if (!vehiclePositionsTripIdToVehicleId.containsKey(trip_id)) {
                // W003 - TripUpdates feed has a trip_id that's not in VehiclePositions feed
                RuleUtils.addOccurrence(W003, "trip_id " + trip_id + " is in TripUpdates but not in VehiclePositions feed", w003List, _log);
            }
        }

        // Check all vehicles that did NOT contain a trip
        for (String vehicle_id : vehiclesWithoutTrips) {
            if (!tripUpdatesVehicleIdToTripId.containsKey(vehicle_id)) {
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
     * Checks E047 - "VehiclePosition and TripUpdate ID pairing mismatch" for TripUpdates, and adds a found error to the provided error list.
     *
     * @param trip                    A map entry of a trip_id to a vehicle_id that represents a trip in TripUpdates and a vehicle_id that the trip contains
     * @param vehiclePositionsInverse An inverse map of a VehiclePositions feed, with keys that represent the trip_ids that each VehiclePosition contains, and the value being the VehiclePosition vehicle.id that contains the trip_id
     * @param errors                  the list to add the errors to
     */
    private void checkE047TripUpdates(Map.Entry<String, String> trip, HashMap<String, String> vehiclePositionsInverse, List<OccurrenceModel> errors) {
        String vehiclePositionsVehicleId = vehiclePositionsInverse.get(trip.getKey());
        String tripUpdatesVehicleId = trip.getValue();
        if (!StringUtils.isEmpty(vehiclePositionsVehicleId)) {
            if (!tripUpdatesVehicleId.equals(vehiclePositionsVehicleId)) {
                RuleUtils.addOccurrence(E047, "vehicle_id " + trip.getValue() + " and trip_id " + trip.getKey() + " pairing in TripUpdates does not match vehicle_id " + vehiclePositionsVehicleId + " and trip_id " + trip.getKey() + " pairing in VehiclePositions feed", errors, _log);
            }
        }
    }

    /**
     * Checks E047 - "VehiclePosition and TripUpdate ID pairing mismatch" for VehiclePositions, and adds a found error to the provided error list.
     *
     * @param vehicle            A map entry of a vehicle_id to a trip_id that represents a vehicle in VehiclePositions and the trip_id that the vehicle contains
     * @param tripUpdatesInverse An inverse map of a TripUpdates feed, with keys that represent the vehicle_ids that each TripUpdate contains, and the value being the TripUpdate trip_id that contains the vehicle.id
     * @param errors             the list to add the errors to
     */
    private void checkE047VehiclePositions(Map.Entry<String, String> vehicle, HashMap<String, String> tripUpdatesInverse, GtfsMetadata gtfsMetadata, List<OccurrenceModel> errors) {
        String tripUpdatesTripId = tripUpdatesInverse.get(vehicle.getKey());
        String vehiclePositionsTripId = vehicle.getValue();
        if (!StringUtils.isEmpty(tripUpdatesTripId)) {
            if (!vehiclePositionsTripId.equals(tripUpdatesTripId)) {
                // Log E047 if either trip_id is missing from GTFS, if the block_id is missing for either trip (block_id is an optional field) or if the two trips aren't in the same block
                Trip tripA = gtfsMetadata.getTrips().get(vehiclePositionsTripId);
                Trip tripB = gtfsMetadata.getTrips().get(tripUpdatesTripId);
                if (tripA == null || tripB == null ||
                        StringUtils.isEmpty(tripA.getBlockId()) || StringUtils.isEmpty(tripB.getBlockId()) ||
                        !tripA.getBlockId().equals(tripB.getBlockId())) {
                    // E047 - "VehiclePosition and TripUpdate ID pairing mismatch" for VehiclePositions
                    RuleUtils.addOccurrence(E047, "trip_id " + vehicle.getValue() + " and vehicle_id " + vehicle.getKey() + " pairing in VehiclePositions does not match trip_id " + tripUpdatesTripId + " and vehicle_id " + vehicle.getKey() + " pairing in TripUpdates feed and trip block_ids aren't the same", errors, _log);
                }
            }
        }
    }
}
