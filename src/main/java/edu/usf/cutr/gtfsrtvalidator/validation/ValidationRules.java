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
            "vehicle_id should be populated for TripUpdates and VehiclePositions",
            "does not have a vehicle_id");
    public static final ValidationRule W003 = new ValidationRule("W003", "WARNING", "VehiclePosition and TripUpdate feed mismatch",
            "If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values should match between the two feeds",
            "does not exist in both VehiclePositions and TripUpdates feeds");
    public static final ValidationRule W004 = new ValidationRule("W004", "WARNING", "VehiclePosition has unrealistic speed",
            "vehicle.position.speed has an unrealistic speed that may be incorrect",
            "is unrealistic");
    public static final ValidationRule W005 = new ValidationRule("W005", "WARNING", "Missing vehicle_id for frequency-based exact_times = 0",
            "Frequency-based exact_times = 0 trip_updates and vehicle positions should contain vehicle_id",
            "is missing vehicle_id, which is suggested for frequency-based exact_times=0 trips");
    public static final ValidationRule W006 = new ValidationRule("W006", "WARNING", "trip_update missing trip_id",
            "trip_updates should include a trip_id",
            "trip_update does not contain a trip_id");
    public static final ValidationRule W007 = new ValidationRule("W007", "WARNING", "Refresh interval more than 35 seconds",
            "GTFS-realtime feeds should be refreshed at least every 30 seconds",
            "which is less than the recommended interval of 35 seconds");

    /**
     * Errors
     */
    public static final ValidationRule E001 = new ValidationRule("E001", "ERROR", "Not in POSIX time",
            "All timestamps must be in POSIX time (i.e., number of seconds since January 1st 1970 00:00:00 UTC)",
            "is not POSIX time");
    public static final ValidationRule E002 = new ValidationRule("E002", "ERROR", "Unsorted stop_sequence",
            "stop_time_updates for a given trip_id must be sorted by increasing stop_sequence",
            "is not sorted by increasing stop_sequence");
    public static final ValidationRule E003 = new ValidationRule("E003", "ERROR", "GTFS-rt trip_id does not exist in GTFS data and does not have schedule_relationship of ADDED",
            "All trip_ids provided in the GTFS-rt feed must exist in the GTFS data, unless the schedule_relationship is ADDED",
            "does not exist in the GTFS data and does not have schedule_relationship of ADDED");
    public static final ValidationRule E004 = new ValidationRule("E004", "ERROR", "GTFS-rt route_id does not exist in GTFS data",
            "All route_ids provided in the GTFS-rt feed must exist in the GTFS data",
            "does not exist in the GTFS data");

    // TODO - implement
    public static final ValidationRule E005 = new ValidationRule("E005", "ERROR", "GTFS stop_times.txt does not contain arrival_times and/or departure_times for all stops referenced in the GTFS-rt feed",
            "If only delay is provided in a stop_time_event (and not a time), then the GTFS stop_times.txt must contain arrival_times and/or departure_times for all stops referenced in the GTFS-rt feed (i.e., not just timepoints)",
            "has only delay but no arrival and/or departure time");

    public static final ValidationRule E006 = new ValidationRule("E006", "ERROR", "Missing required trip field for frequency-based exact_times = 0",
            "Frequency-based exact_times=0 trip_updates must contain trip_id, start_time, and start_date",
            "which is required for frequency-based exact_times = 0 trips");

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

    public static final ValidationRule E010 = new ValidationRule("E010", "ERROR", "location_type not 0 in stops.txt",
            "If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0",
            "is not location_type 0");

    public static final ValidationRule E011 = new ValidationRule("E011", "ERROR", "GTFS-rt stop_id does not exist in GTFS data",
            "All stop_ids referenced in GTFS-rt feeds must exist in GTFS stops.txt", "does not exist in GTFS data stops.txt");

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

    public static final ValidationRule E015 = new ValidationRule("E015", "ERROR", "All stop_ids referenced in GTFS-rt feeds must have the location_type = 0",
            "All stop_ids referenced in GTFS-rt feeds must have the location_type = 0 in GTFS stops.txt",
            "does not have location_type=0 in GTFS stops.txt");

    public static final ValidationRule E016 = new ValidationRule("E016", "ERROR", "trip_ids with schedule_relationship ADDED must not be in GTFS data",
            "Trips that have a schedule_relationship of ADDED must not be included in the GTFS data",
            "has a schedule_relationship of ADDED but exists in the GTFS data");

    public static final ValidationRule E017 = new ValidationRule("E017", "ERROR", "GTFS-rt content changed but has the same header timestamp",
            "The GTFS-rt header timestamp value should always change if the feed contents change - the feed contents must not change without updating the header timestamp",
            "was the same for this and the previous feed iteration but the feed content was not the same");

    public static final ValidationRule E018 = new ValidationRule("E018", "ERROR", "GTFS-rt header timestamp decreased between two sequential iterations",
            "The GTFS-rt header timestamp should be monotonically increasing -  it should always be the same value or greater than previous feed iterations if the feed contents are different",
            "from the previous feed iteration");
}