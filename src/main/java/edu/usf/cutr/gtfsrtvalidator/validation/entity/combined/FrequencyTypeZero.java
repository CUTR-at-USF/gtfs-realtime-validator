/*
 * Copyright (C) 2017 University of South Florida
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
import org.onebusaway.gtfs.model.Frequency;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * ID: E013
 * Description: Frequency type 0 trip schedule_relationship should be UNSCHEDULED or empty
 */
public class FrequencyTypeZero implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(FrequencyTypeZero.class);

    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsRealtime.FeedMessage feedMessage) {
        MessageLogModel modelE013 = new MessageLogModel(ValidationRules.E013);
        List<OccurrenceModel> errorListE013 = new ArrayList<>();

        Collection<Frequency> frequencies = gtfsData.getAllFrequencies();
        Set<String> exactTimesZeroTrips = new HashSet<>();

        for (Frequency f : frequencies) {
            // Create a set of all exact_times=0 trips
            if (f.getExactTimes() == 0) {
                exactTimesZeroTrips.add(f.getTrip().getId().getAgencyId());
            }
        }

        /**
         * E013 - Validate schedule_relationship is UNSCHEDULED or empty
         */
        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
            if (exactTimesZeroTrips.contains(tripUpdate.getTrip().getTripId()) &&
                    !(tripUpdate.getTrip().hasScheduleRelationship() || tripUpdate.getTrip().getScheduleRelationship().equals(GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED))) {
                _log.info("TripUpdate trip_id " + tripUpdate.getTrip().getTripId() + " is exact_times=0 and has an incorrect ScheduleRelationship of " + tripUpdate.getTrip().getScheduleRelationship());
                errorListE013.add(new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId(), "Incorrect ScheduleRelationship of " + tripUpdate.getTrip().getScheduleRelationship()));
            }

            GtfsRealtime.VehiclePosition vehiclePosition = entity.getVehicle();
            if (vehiclePosition.hasTrip() &&
                    exactTimesZeroTrips.contains(vehiclePosition.getTrip().getTripId()) &&
                    !(vehiclePosition.getTrip().hasScheduleRelationship() || vehiclePosition.getTrip().getScheduleRelationship().equals(GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED))) {
                _log.info("vehicle ID " + vehiclePosition.getVehicle().getId() + "trip_id " + vehiclePosition.getTrip().getTripId() + " is exact_times=0 and has an incorrect ScheduleRelationship of " + vehiclePosition.getTrip().getScheduleRelationship());
                errorListE013.add(new OccurrenceModel("vehicle ID " + vehiclePosition.getVehicle().getId() + "trip_id " + vehiclePosition.getTrip().getTripId(), "Incorrect ScheduleRelationship of " + vehiclePosition.getTrip().getScheduleRelationship()));
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorListE013.isEmpty()) {
            errors.add(new ErrorListHelperModel(modelE013, errorListE013));
        }
        return errors;
    }
}
