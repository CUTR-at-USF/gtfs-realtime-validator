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

package edu.usf.cutr.gtfsrtvalidator.validation.entity.combined;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class VehicleTripDescriptorValidator implements FeedEntityValidator {

    /**
     * ID: w003
     * Description: If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values should match between the two feeds.
     */
    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsRealtime.FeedMessage feedMessage) {

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

        MessageLogModel messageLogModel = new MessageLogModel(ValidationRules.W003);
        List<OccurrenceModel> errorOccurrenceList = new ArrayList<>();

        //Should be optimized since this would be costly with a higher number of feeds
        if (!tripUpdates.isEmpty() && !vehiclePositions.isEmpty()) {

            //Checks if all TripUpdate object has a matching tripId in any of the VehicleUpdate objects
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
                    errorOccurrenceList.add(new OccurrenceModel("$.entity.*.trip_update.trip[?(@.trip_id==\"" + trip.getTrip().getTripId() + "\")]", trip.getTrip().getTripId()));
                }
            }

            //Checks if all VehicleUpdate object has a matching tripId in any of the TripUpdate objects
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
                    errorOccurrenceList.add(new OccurrenceModel("$.entity.*.trip_update.trip[?(@.trip_id==\"" + vehiclePosition.getTrip().getTripId() + "\")]", vehiclePosition.getTrip().getTripId()));
                }
            }
        }
        return Arrays.asList(new ErrorListHelperModel(messageLogModel, errorOccurrenceList));
    }
}
