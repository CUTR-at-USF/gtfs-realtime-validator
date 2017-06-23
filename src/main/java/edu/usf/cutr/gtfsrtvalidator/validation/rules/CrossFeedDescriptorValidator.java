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

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.util.RuleUtils;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.W003;

/**
 * W003 - If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values should match between the two feeds.
 */
public class CrossFeedDescriptorValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(CrossFeedDescriptorValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        List<OccurrenceModel> warningListW003 = new ArrayList<>();

        List<GtfsRealtime.TripUpdate> tripUpdates = new ArrayList<>();
        List<GtfsRealtime.VehiclePosition> vehiclePositions = new ArrayList<>();
        List<GtfsRealtime.Alert> alerts = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                tripUpdates.add(entity.getTripUpdate());
            }
            if (entity.hasVehicle()) {
                vehiclePositions.add(entity.getVehicle());
            }
            if (entity.hasAlert()) {
                alerts.add(entity.getAlert());
            }
        }

        // FIXME - Should be optimized since this would be costly with a higher number of feeds
        if (!tripUpdates.isEmpty() && !vehiclePositions.isEmpty()) {
            // Checks if all TripUpdate object has a matching tripId in any of the VehicleUpdate objects
            for (GtfsRealtime.TripUpdate trip : tripUpdates) {
                boolean matchingTrips = false;
                for (GtfsRealtime.VehiclePosition vehiclePosition : vehiclePositions) {

                    if (Objects.equals(trip.getTrip().getTripId(), vehiclePosition.getTrip().getTripId())) {
                        matchingTrips = true;
                        break;
                    } else if (Objects.equals(trip.getVehicle().getId(), vehiclePosition.getVehicle().getId())) {
                        matchingTrips = true;
                        break;
                    }
                }
                if (!matchingTrips) {
                    // W003 - If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values should match between the two feeds.
                    RuleUtils.addOccurrence(W003, "trip_id " + trip.getTrip().getTripId(), warningListW003, _log);
                }
            }

            // Checks if all VehicleUpdate object has a matching tripId in any of the TripUpdate objects
            for (GtfsRealtime.VehiclePosition vehiclePosition : vehiclePositions) {
                boolean matchingTrips = false;
                for (GtfsRealtime.TripUpdate trip : tripUpdates) {
                    if (Objects.equals(trip.getTrip().getTripId(), vehiclePosition.getTrip().getTripId())) {
                        matchingTrips = true;
                        break;
                    } else if (Objects.equals(trip.getVehicle().getId(), vehiclePosition.getVehicle().getId())) {
                        matchingTrips = true;
                        break;
                    }
                }
                if (!matchingTrips) {
                    // W003 - If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values should match between the two feeds.
                    RuleUtils.addOccurrence(W003, "trip_id " + vehiclePosition.getTrip().getTripId(), warningListW003, _log);
                }
            }
        }
        return Collections.singletonList(new ErrorListHelperModel(new MessageLogModel(W003), warningListW003));
    }
}
