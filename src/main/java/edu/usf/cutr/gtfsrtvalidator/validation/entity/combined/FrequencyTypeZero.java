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

import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;

/**
 * ID: E013
 * Description: Frequency type 0 trip schedule_relationship should be UNSCHEDULED or empty
 */
public class FrequencyTypeZero implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(FrequencyTypeZero.class);

    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        List<OccurrenceModel> errorListE006 = new ArrayList<>();
        List<OccurrenceModel> errorListE013 = new ArrayList<>();
        List<OccurrenceModel> errorListW005 = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();

                if (gtfsMetadata.getExactTimesZeroTripIds().contains(tripUpdate.getTrip().getTripId())) {
                    /**
                     * E006 - Missing required trip_update trip field for frequency-based exact_times = 0
                     * NOTE - W006 checks for missing trip_ids, because we can't check for that here
                     */

                    // Check for missing start_date
                    if (!tripUpdate.getTrip().hasStartDate()) {
                        OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() + " is missing start_date");
                        errorListE006.add(om);
                        _log.debug(om.getPrefix() + " " + E006.getOccurrenceSuffix());
                    }

                    // Check for missing start_time
                    if (!tripUpdate.getTrip().hasStartTime()) {
                        OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() + " is missing start_time");
                        errorListE006.add(om);
                        _log.debug(om.getPrefix() + " " + E006.getOccurrenceSuffix());
                    }

                    /**
                     * E013 - Validate schedule_relationship is UNSCHEDULED or empty
                     */
                    if (!(tripUpdate.getTrip().hasScheduleRelationship() || tripUpdate.getTrip().getScheduleRelationship().equals(GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED))) {
                        OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId() + " schedule_relationship " + tripUpdate.getTrip().getScheduleRelationship());
                        errorListE013.add(om);
                        _log.debug(om.getPrefix() + " " + E013.getOccurrenceSuffix());
                    }

                    /**
                     * W005 - Missing vehicle_id in trip_update for frequency-based exact_times = 0
                     */
                    if (!tripUpdate.hasVehicle() || !tripUpdate.getVehicle().hasId()) {
                        OccurrenceModel om = new OccurrenceModel("trip_id " + tripUpdate.getTrip().getTripId());
                        errorListW005.add(om);
                        _log.debug(om.getPrefix() + " " + W005.getOccurrenceSuffix());
                    }
                }
            }

            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition vehiclePosition = entity.getVehicle();
                if (vehiclePosition.hasTrip() &&
                        gtfsMetadata.getExactTimesZeroTripIds().contains(vehiclePosition.getTrip().getTripId())) {

                    /**
                     * E006 - Missing required vehicle_position trip field for frequency-based exact_times = 0
                     * NOTE - W006 checks for missing trip_ids, because we can't check for that here
                     */

                    // Check for missing start_date
                    if (!vehiclePosition.getTrip().hasStartDate()) {
                        OccurrenceModel om = new OccurrenceModel("vehicle_id " + vehiclePosition.getVehicle().getId() + " trip_id " + vehiclePosition.getTrip().getTripId() + " is missing start_date");
                        errorListE006.add(om);
                        _log.debug(om.getPrefix() + " " + E006.getOccurrenceSuffix());
                    }

                    // Check for missing start_time
                    if (!vehiclePosition.getTrip().hasStartTime()) {
                        OccurrenceModel om = new OccurrenceModel("vehicle_id " + vehiclePosition.getVehicle().getId() + " trip_id " + vehiclePosition.getTrip().getTripId() + " is missing start_time");
                        errorListE006.add(om);
                        _log.debug(om.getPrefix() + " " + E006.getOccurrenceSuffix());
                    }

                    /**
                     * E013 - Validate schedule_relationship is UNSCHEDULED or empty
                     */
                    if (!(vehiclePosition.getTrip().hasScheduleRelationship() || vehiclePosition.getTrip().getScheduleRelationship().equals(GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED))) {
                        OccurrenceModel om = new OccurrenceModel("vehicle_id " + vehiclePosition.getVehicle().getId() + " trip_id " + vehiclePosition.getTrip().getTripId() + " schedule_relationship " + vehiclePosition.getTrip().getScheduleRelationship());
                        errorListE013.add(om);
                        _log.debug(om.getPrefix() + " " + E013.getOccurrenceSuffix());
                    }


                    /**
                     * W005 - Missing vehicle_id for frequency-based exact_times = 0
                     */
                    if (!vehiclePosition.getVehicle().hasId()) {
                        OccurrenceModel om = new OccurrenceModel("entity ID" + entity.getId() + "with trip_id " + vehiclePosition.getTrip().getTripId());
                        errorListW005.add(om);
                        _log.debug(om.getPrefix() + " " + W005.getOccurrenceSuffix());
                    }
                }
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorListE006.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E006), errorListE006));
        }
        if (!errorListE013.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E013), errorListE013));
        }
        if (!errorListW005.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(W005), errorListW005));
        }
        return errors;
    }
}
