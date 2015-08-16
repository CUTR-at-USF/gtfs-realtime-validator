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

package edu.usf.cutr.gtfsrtvalidator.validation;
import edu.usf.cutr.gtfsrtvalidator.api.model.ValidationRule;

public class ValidationRules {
    //region Warnings
    //---------------------------------------------------------------------------------------
    public static final ValidationRule W001 = new ValidationRule("w001",
            "Timestamps should be populated for all elements");
    public static final ValidationRule W002 = new ValidationRule("w002",
            "vehicle_id should be populated in trip_update");
    public static final ValidationRule W003 = new ValidationRule("w003",
            "If both vehicle positions and trip updates are provided, VehicleDescriptor or TripDescriptor values should match between the two feeds");

    //---------------------------------------------------------------------------------------
    //endregion

    //region Errors
    //---------------------------------------------------------------------------------------
    public static final ValidationRule E001 = new ValidationRule("e001",
            "All timestamps must be in POSIX time (i.e., number of seconds since January 1st 1970 00:00:00 UTC)");
    public static final ValidationRule E002 = new ValidationRule("e002",
            "stop_time_updates for a given trip_id must be sorted by increasing stop_sequence");
    public static final ValidationRule E003 = new ValidationRule("e003",
            "All trip_ids provided in the GTFS-rt feed must appear in the GTFS data");
    public static final ValidationRule E004 = new ValidationRule("e004",
            "All route_ids provided in the GTFS-rt feed must appear in the GTFS data");
    public static final ValidationRule E005 = new ValidationRule("e005",
            "If only delay is provided in a stop_time_event (and not a time), then the GTFS stop_times.txt must contain arrival_times and/or departure_times for all stops referenced in the GTFS-rt feed (i.e., not just timepoints)");
    public static final ValidationRule E006 = new ValidationRule("e006",
            "Frequency-based trip_updates must contain trip_id and start_time");
    public static final ValidationRule E007 = new ValidationRule("e007",
            "If more than more trip_update has the same vehicle_id, then all trips must belong to the same block, and the arrival predictions for each trip must match the sequential order for the trips in the block. For example, if we have trip_ids 1, 2, and 3 that all belong to the same block, and the vehicle trips trip 1, then trip 2, and then trip 3, the arrival predictions should increase chronologically for trips 1, 2, and 3. For example, trip 3 predictions shouldn't be earlier than trip 2 predictions.");
    public static final ValidationRule E008 = new ValidationRule("e008",
            "If a GTFS block (defined by block_id in trips.txt) contains multiple references to the same stopId (i.e., the bus visits the same stopId more than once in the same block), but in different trips, then in the GTFS-rt data the tripId for each TripUpdate.TripDescriptor must be provided. In this case, the bus wouldn't visit the same stopId more than once in the same trip.");
    public static final ValidationRule E009 = new ValidationRule("e009",
            "If a GTFS trip contains multiple references to the same stopId (i.e., the bus visits the same stopId more than once in the SAME trip), then in the GTFS-rt data the stop_sequence for each TripUpdate.StopTimeUpdate must be provided.");

    /**
     * Issue: https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/8
     * Description: If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0
     * Affected Feed Type(s): GTFS feed
     * Reference(s): https://developers.google.com/transit/gtfs/reference?hl=en#stop_timestxt
     */
    public static final ValidationRule E010 = new ValidationRule("e010",
            "If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0");

    /**
     * Type: Error (Implicitly stated in the GTFS specifications)
     * Description: All stop_ids referenced in GTFS-rt feeds must have the "location_type" = 0
     * Affected Feed Type(s): TripUpdate, VehiclePostion
     * Reference(s): https://developers.google.com/transit/gtfs/reference?hl=en#stop_timestxt
     */
    public static final ValidationRule E011 = new ValidationRule("e011",
            "All stop_ids referenced in GTFS-rt feeds must have the location_type = 0");
    //---------------------------------------------------------------------------------------
    //endregion
}