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
package edu.usf.cutr.gtfsrtvalidator.lib.validation.rules;

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.lib.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.util.RuleUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules.E019;

/**
 * Rules for frequency-based type 1 trips - trips defined in GTFS frequencies.txt with exact_times = 1
 * <p>
 * E019 - GTFS-rt frequency type 1 trip start_time must be a multiple of GTFS data start_time
 */
public class FrequencyTypeOneValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(FrequencyTypeOneValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsMutableDao gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
        List<OccurrenceModel> errorListE019 = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();

                List<Frequency> frequenceTypeOneList = gtfsMetadata.getExactTimesOneTrips().get(tripUpdate.getTrip().getTripId());
                if (frequenceTypeOneList != null) {
                    boolean foundMatch = false;
                    String gtfsStartTimeString = null;
                    Integer headwaySecs = null;
                    // For at least one frequency period for this trip_id, start_time in the GTFS-rt data must be some multiple (including zero) of headway_secs later than the start_time
                    for (Frequency f : frequenceTypeOneList) {
                        int startTime = f.getStartTime();
                        // See if the GTFS-rt start_time matches at least one multiple of GTFS start_time for this frequency
                        while (startTime < f.getEndTime()) {
                            // Convert seconds after midnight to 24hr clock time like "06:00:00"
                            gtfsStartTimeString = TimestampUtils.secondsAfterMidnightToClock(startTime);
                            headwaySecs = f.getHeadwaySecs();
                            _log.debug("start time = " + startTime);
                            _log.debug("formatted start time = " + gtfsStartTimeString);
                            if (tripUpdate.getTrip().getStartTime().equals(gtfsStartTimeString)) {
                                // We found a matching multiple - no error for this GTFS-rt start_time
                                foundMatch = true;
                                break;
                            }
                            startTime += f.getHeadwaySecs();
                        }
                        if (foundMatch) {
                            // If we found at least one matching frequency with a matching multiple of headway_secs for the GTFS-rt start_time, then no error
                            break;
                        }
                    }
                    if (!foundMatch) {
                        // E019 - GTFS-rt frequency exact_times = 1 trip start_time must match GTFS data
                        String prefix = "GTFS-rt trip_id " + tripUpdate.getTrip().getTripId() +
                                " has start_time of " + tripUpdate.getTrip().getStartTime() +
                                " and GTFS frequencies.txt start_time is " + gtfsStartTimeString + " with a headway of " + headwaySecs + " seconds ";
                        RuleUtils.addOccurrence(E019, prefix, errorListE019, _log);
                    }
                }
            }

            if (entity.hasVehicle()) {
                GtfsRealtime.VehiclePosition vehiclePosition = entity.getVehicle();

                // E019 - GTFS-rt frequency exact_times = 1 trip start_date and start_time must match GTFS data
                List<Frequency> frequenceTypeOneList = gtfsMetadata.getExactTimesOneTrips().get(vehiclePosition.getTrip().getTripId());
                if (frequenceTypeOneList != null) {
                    boolean foundMatch = false;
                    String gtfsStartTimeString = null;
                    Integer headwaySecs = null;
                    // For at least one frequency period for this trip_id, start_time in the GTFS-rt data must be some multiple (including zero) of headway_secs later than the start_time
                    for (Frequency f : frequenceTypeOneList) {
                        int startTime = f.getStartTime();
                        // See if the GTFS-rt start_time matches at least one multiple of GTFS start_time for this frequency
                        while (startTime < f.getEndTime()) {
                            // Convert seconds after midnight to 24hr clock time like "06:00:00"
                            gtfsStartTimeString = String.format("%02d:%02d:%02d", startTime / 3600, startTime % 360, startTime % 60);
                            headwaySecs = f.getHeadwaySecs();
                            _log.debug("start time = " + startTime);
                            _log.debug("formatted start time = " + gtfsStartTimeString);
                            if (vehiclePosition.hasTrip() && vehiclePosition.getTrip().getStartTime().equals(gtfsStartTimeString)) {
                                // We found a matching multiple - no error for this GTFS-rt start_time
                                foundMatch = true;
                                break;
                            }
                            startTime += f.getHeadwaySecs();
                        }
                        if (foundMatch) {
                            // If we found at least one matching frequency with a matching multiple of headway_secs for the GTFS-rt start_time, then no error
                            break;
                        }
                    }
                    if (!foundMatch) {
                        // E019 - GTFS-rt frequency exact_times = 1 trip start_time must match GTFS data
                        String prefix = "GTFS-rt trip_id " + vehiclePosition.getTrip().getTripId() +
                                " has start_time of " + vehiclePosition.getTrip().getStartTime() +
                                " and GTFS frequencies.txt start_time is " + gtfsStartTimeString + " with a headway of " + headwaySecs + " seconds ";
                        RuleUtils.addOccurrence(E019, prefix, errorListE019, _log);
                    }
                }
            }
        }
        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!errorListE019.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E019), errorListE019));
        }
        return errors;
    }
}
