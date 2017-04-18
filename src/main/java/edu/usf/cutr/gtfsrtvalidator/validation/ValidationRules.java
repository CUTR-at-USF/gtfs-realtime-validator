/*
 * Copyright (C) 2011-2017 Nipuna Gunathilake, University of South Florida.
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

package edu.usf.cutr.gtfsrtvalidator.validation;
import edu.usf.cutr.gtfsrtvalidator.api.model.ValidationRule;

public class ValidationRules {
    /**
     * Warnings
     */
    public static final ValidationRule W001 = new ValidationRule("W001", "WARNING", "Timestamp not populated",
            "Timestamps should be populated for all elements",
            "does not have a timestamp");
    public static final ValidationRule W002 = new ValidationRule("W002", "WARNING", "Vehicle_id not populated",
            "vehicle_id should be populated in trip_update",
            "does not have a vehicle_id");
    public static final ValidationRule W003 = new ValidationRule("W003", "WARNING", "VehiclePosition and TripUpdate feed mismatch",
            "If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values should match between the two feeds",
            "does not appear in both VehiclePositions and TripUpdates feeds");

    /**
     * Errors
     */
    public static final ValidationRule E001 = new ValidationRule("E001", "ERROR", "Not in POSIX time",
            "All timestamps must be in POSIX time (i.e., number of seconds since January 1st 1970 00:00:00 UTC)",
            "is not POSIX time");
    public static final ValidationRule E002 = new ValidationRule("E002", "ERROR", "Unsorted stop_sequence",
            "stop_time_updates for a given trip_id must be sorted by increasing stop_sequence",
            "is not sorted by increasing stop_sequence");
    public static final ValidationRule E003 = new ValidationRule("E003", "ERROR", "GTFS-rt trip_id does not appear in GTFS data",
            "All trip_ids provided in the GTFS-rt feed must appear in the GTFS data",
            "does not appear in the GTFS data");
    public static final ValidationRule E004 = new ValidationRule("E004", "ERROR", "GTFS-rt route_id does not appear in GTFS data",
            "All route_ids provided in the GTFS-rt feed must appear in the GTFS data",
            "does not appear in the GTFS data");

    // TODO - implement
    public static final ValidationRule E005 = new ValidationRule("E005", "ERROR", "GTFS stop_times.txt does not contain arrival_times and/or departure_times for all stops referenced in the GTFS-rt feed",
            "If only delay is provided in a stop_time_event (and not a time), then the GTFS stop_times.txt must contain arrival_times and/or departure_times for all stops referenced in the GTFS-rt feed (i.e., not just timepoints)",
            "has only delay but no arrival and/or departure time");

    // TODO - implement - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/59
    public static final ValidationRule E006 = new ValidationRule("E006", "ERROR", "Missing trip_id and start_time in Frequency-based trip_updates",
            "Frequency-based trip_updates must contain trip_id, start_time, and start_date",
            "is missing trip_id and/or start_time");

    // TODO - implement
    public static final ValidationRule E007 = new ValidationRule("E007", "ERROR", "Trips with same vehicle_id do not belong to the same block",
            "If more than one trip_update has the same vehicle_id, then all trips must belong to the same block",
            "do not belong to the same block but have the same vehicle_id");

    // TODO - implement
    public static final ValidationRule E008 = new ValidationRule("E008", "ERROR", "trip_id for each TripUpdate.TripDescriptor is not provided",
            "If a GTFS block contains multiple references to the same stopId (i.e., the bus visits the same stopId more than once in the same block), but in different trips, then in the GTFS-rt data the tripId for each TripUpdate.TripDescriptor must be provided. In this case, the bus wouldn't visit the same stopId more than once in the same trip.",
            "does not have a trip_id");

    // TODO - implement - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/17
    public static final ValidationRule E009 = new ValidationRule("E009", "ERROR", "The stop_sequence for each TripUpdate.StopTimeUpdate is not provided",
            "If a GTFS trip contains multiple references to the same stopId (i.e., the bus visits the same stopId more than once in the SAME trip), then in the GTFS-rt data the stop_sequence for each TripUpdate.StopTimeUpdate must be provided.",
            "does not contain stop_sequence");

    /**
     * Issue: https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/8
     * Description: If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0
     * Affected Feed Type(s): GTFS feed
     * Reference(s): https://developers.google.com/transit/gtfs/reference?hl=en#stop_timestxt
     */
    public static final ValidationRule E010 = new ValidationRule("E010", "ERROR", "location_type not 0 in stops.txt",
            "If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0",
            "is not location_type 0");

    /**
     * Type: Error (Implicitly stated in the GTFS specifications)
     * Description: All stop_ids referenced in GTFS-rt feeds must have the "location_type" = 0
     * Affected Feed Type(s): TripUpdate, VehiclePostion
     * Reference(s): https://developers.google.com/transit/gtfs/reference?hl=en#stop_timestxt
     */
    public static final ValidationRule E011 = new ValidationRule("E011", "ERROR", "Location_type not 0 in GTFS-rt",
            "All stop_ids referenced in GTFS-rt feeds must have the location_type = 0", "is not location_type 0");
    //---------------------------------------------------------------------------------------
    //endregion

    public static final ValidationRule E012 = new ValidationRule("E012", "ERROR", "Header timestamp should be greater than or equal to all other timestamps",
            "No timestamps for individual entities (TripUpdate, VehiclePosition) in the feeds should be greater than the header timestamp",
            "is greater than the header");

    public static final ValidationRule E013 = new ValidationRule("E013", "ERROR", "Frequency type 0 trip schedule_relationship should be UNSCHEDULED or empty",
            "For frequency-based exact_times=0 trips, schedule_relationship should be UNSCHEDULED or empty.",
            "schedule_relationship is not UNSCHEDULED or empty");

    // TODO - implement
    public static final ValidationRule E014 = new ValidationRule("E014", "ERROR", "Predictions for trips are out-of-order in the block",
            "Arrival predictions for each trip in the feed must match the sequential order for the trips in the block. For example, if we have trip_ids 1, 2, and 3 that all belong to the same block, and the vehicle trips trip 1, then trip 2, and then trip 3, the arrival predictions should increase chronologically for trips 1, 2, and 3. For example, trip 3 predictions shouldn't be earlier in the feed than trip 2 predictions.",
            "predictions are not ordered by appearance in block");
}