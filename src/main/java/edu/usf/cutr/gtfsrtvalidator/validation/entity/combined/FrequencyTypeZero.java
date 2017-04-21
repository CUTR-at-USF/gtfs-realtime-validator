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
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.E013;

/**
 * ID: E013
 * Description: Frequency type 0 trip schedule_relationship should be UNSCHEDULED or empty
 */
public class FrequencyTypeZero implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(FrequencyTypeZero.class);

    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        List<OccurrenceModel> errorListE013 = new ArrayList<>();

        /**
         * E013 - Validate schedule_relationship is UNSCHEDULED or empty
         */
        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                if (gtfsMetadata.getExactTimesZeroTripIds().contains(tripUpdate.getTrip().getTripId()) &&
                        !(tripUpdate.getTrip().hasScheduleRelationship() || tripUpdate.getTrip().getScheduleRelationship().equals(GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED))) {
                    OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() + " schedule_relationship " + tripUpdate.getTrip().getScheduleRelationship());
                    errorListE013.add(om);
                    _log.debug(om.getPrefix() + " " + E013.getOccurrenceSuffix());
                }
            }

            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition vehiclePosition = entity.getVehicle();
                if (vehiclePosition.hasTrip() &&
                        gtfsMetadata.getExactTimesZeroTripIds().contains(vehiclePosition.getTrip().getTripId()) &&
                        !(vehiclePosition.getTrip().hasScheduleRelationship() || vehiclePosition.getTrip().getScheduleRelationship().equals(GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED))) {
                    OccurrenceModel om = new OccurrenceModel("vehicle_id " + vehiclePosition.getVehicle().getId() + " trip_id " + vehiclePosition.getTrip().getTripId() + " schedule_relationship " + vehiclePosition.getTrip().getScheduleRelationship());
                    errorListE013.add(om);
                    _log.debug(om.getPrefix() + " " + E013.getOccurrenceSuffix());
                }
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorListE013.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E013), errorListE013));
        }
        return errors;
    }
}
