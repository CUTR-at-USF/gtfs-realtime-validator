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

package edu.usf.cutr.gtfsrtvalidator.lib.validation;

import edu.usf.cutr.gtfsrtvalidator.lib.model.ValidationRule;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationRules {
    /**
     * Warnings
     */
    public static final ValidationRule W001 = new ValidationRule("W001", "WARNING", "timestamp not populated",
            "Timestamps should be populated for all elements",
            "does not have a timestamp");
    public static final ValidationRule W002 = new ValidationRule("W002", "WARNING", "vehicle_id not populated",
            "vehicle_id should be populated for TripUpdates and VehiclePositions",
            "does not have a vehicle_id");
    public static final ValidationRule W003 = new ValidationRule("W003", "WARNING", "ID in one feed missing from the other",
            "a trip_id that is provided in the VehiclePositions feed should be provided in the TripUpdates feed, and a vehicle_id that is provided in the TripUpdates feed should be provided in the VehiclePositions feed",
            "");
    public static final ValidationRule W004 = new ValidationRule("W004", "WARNING", "vehicle speed is unrealistic",
            "vehicle.position.speed has an unrealistic speed that may be incorrect",
            "is unrealistic");
    public static final ValidationRule W005 = new ValidationRule("W005", "WARNING", "Missing vehicle_id for frequency-based exact_times = 0",
            "Frequency-based exact_times = 0 trip_updates and vehicle positions should contain vehicle_id",
            "is missing vehicle_id, which is suggested for frequency-based exact_times=0 trips");
    public static final ValidationRule W006 = new ValidationRule("W006", "WARNING", "trip missing trip_id",
            "trip should include a trip_id",
            "does not contain a trip_id");
    public static final ValidationRule W007 = new ValidationRule("W007", "WARNING", "Refresh interval is more than 35 seconds",
            "GTFS-realtime feeds should be refreshed at least every 30 seconds",
            "which is less than the recommended interval of 35 seconds");
    public static final ValidationRule W008 = new ValidationRule("W008", "WARNING", "Header timestamp is older than 65 seconds",
            "The data in a GTFS-realtime feed should always be less than one minute old",
            "old which is greater than the recommended age of 65 seconds");
    public static final ValidationRule W009 = new ValidationRule("W009", "WARNING", "schedule_relationship not populated",
            "trip.schedule_relationship and stop_time_update.schedule_relationship should be populated",
            "does not have a schedule_relationship");

    /**
     * Errors
     */
    public static final ValidationRule E001 = new ValidationRule("E001", "ERROR", "Not in POSIX time",
            "All timestamps must be in POSIX time (i.e., number of seconds since January 1st 1970 00:00:00 UTC)",
            "is not POSIX time");
    public static final ValidationRule E002 = new ValidationRule("E002", "ERROR", "stop_times_updates not strictly sorted",
            "stop_time_updates for a given trip_id must be strictly sorted by increasing stop_sequence",
            "is not strictly sorted by increasing stop_sequence");
    public static final ValidationRule E003 = new ValidationRule("E003", "ERROR", "GTFS-rt trip_id does not exist in GTFS data and does not have schedule_relationship of ADDED",
            "All trip_ids provided in the GTFS-rt feed must exist in the GTFS data, unless the schedule_relationship is ADDED",
            "does not exist in the GTFS data and does not have schedule_relationship of ADDED");
    public static final ValidationRule E004 = new ValidationRule("E004", "ERROR", "GTFS-rt route_id does not exist in GTFS data",
            "All route_ids provided in the GTFS-rt feed must exist in the GTFS data",
            "does not exist in the GTFS data routes.txt");

    // TODO - implement - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/155
    public static final ValidationRule E005 = new ValidationRule("E005", "ERROR", "stop_time_update contains only delay (no times) and GTFS stop_times.txt does not contain corresponding arrival and/or departure_time for that stop",
            "If only delay is provided in a stop_time_update arrival or departure (and not a time), then the GTFS stop_times.txt must contain arrival_times and/or departure_times for these corresponding stops",
            "has only delay but no stops_times.txt arrival and/or departure time");

    public static final ValidationRule E006 = new ValidationRule("E006", "ERROR", "Missing required trip field for frequency-based exact_times = 0",
            "Frequency-based exact_times=0 trip_updates must contain trip_id, start_time, and start_date",
            "which is required for frequency-based exact_times = 0 trips");

    // TODO - implement - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/156
    public static final ValidationRule E007 = new ValidationRule("E007", "ERROR", "Trips with same vehicle_id are not in the same block",
            "If more than one trip_update has the same vehicle_id, then these trips must belong to the same GTFS trips.txt block_id",
            "do not belong to the same block but have the same vehicle_id");

    // TODO - implement - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/157
    public static final ValidationRule E008 = new ValidationRule("E008", "ERROR", "trip_id not provided for blocks with reoccurring stop_ids",
            "If a GTFS block contains multiple references to the same stopId (i.e., the bus visits the same stopId more than once in the same block), but in different trips, then in the GTFS-rt data the tripId for each TripUpdate.TripDescriptor must be provided. In this case, the bus wouldn't visit the same stopId more than once in the same trip.",
            "does not have a trip_id but visits the same stop_id more than once in the block");

    public static final ValidationRule E009 = new ValidationRule("E009", "ERROR", "GTFS-rt stop_sequence isn't provided for trip that visits same stop_id more than once",
            "If a GTFS trip contains multiple references to the same stop_id (i.e., the vehicle visits the same stop_id more than once in the same trip), then GTFS-rt stop_time_updates for this trip must include stop_sequence",
            "more than once and GTFS-rt stop_time_update is missing stop_sequence field - stop_sequence must be provided");

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

    // TODO - implement - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/158
    public static final ValidationRule E014 = new ValidationRule("E014", "ERROR", "Predictions for trips are out-of-order in the block",
            "trip_updates for each trip in the feed must match the sequential order for the trips in the block. For example, if we have trip_ids 1, 2, and 3 that all belong to the same block, and the vehicle travels trip 1, then trip 2, and then trip 3, the trip_updates should occur in the GTFS-rt feed in the order trips 1, 2, and 3. For example, trip 3 predictions shouldn't occur in the feed prior to trip 2 predictions.",
            "predictions are not ordered by appearance in block");

    public static final ValidationRule E015 = new ValidationRule("E015", "ERROR", "All stop_ids referenced in GTFS-rt TripUpdates and VehiclePositions feeds must have the location_type = 0",
            "All stop_ids referenced in GTFS-rt TripUpdates and VehiclePositions feeds must have the location_type = 0 in GTFS stops.txt",
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

    public static final ValidationRule E019 = new ValidationRule("E019", "ERROR", "GTFS-rt frequency type 1 trip start_time must be a multiple of GTFS headway_secs later than GTFS start_time",
            "For frequency-based trips defined in frequencies.txt with exact_times = 1, the GTFS-rt trip start_time must be some multiple (including zero) of headway_secs later than the start_time in file frequencies.txt for the corresponding time period.  Note that this doesn't not apply to frequency-based trips defined in frequencies.txt with exact_times = 0.",
            "- the GTFS-rt start_time is not a multiple of headway_secs later than GTFS start_time");

    public static final ValidationRule E020 = new ValidationRule("E020", "ERROR", "Invalid start_time format",
            "start_time must be in the format HH:MM:SS or H:MM:SS",
            "which is not the valid format of HH:MM:SS or H:MM:SS");

    public static final ValidationRule E021 = new ValidationRule("E021", "ERROR", "Invalid start_date format",
            "start_date must be in the YYYYMMDD format",
            "which is not the valid format of YYYYMMDD");

    public static final ValidationRule E022 = new ValidationRule("E022", "ERROR", "Sequential stop_time_update times are not increasing",
            "stop_time_update arrival/departure times between sequential stops should always increase - they should never be the same or decrease.",
            "- times must increase between two sequential stops");

    public static final ValidationRule E023 = new ValidationRule("E023", "ERROR", "trip start_time does not match first GTFS arrival_time",
            "For normal scheduled trips (i.e., not defined in frequencies.txt), the GTFS-realtime trip start_time must match the first GTFS arrival_time in stop_times.txt for this trip",
            "- times do not match");

    public static final ValidationRule E024 = new ValidationRule("E024", "ERROR", "trip direction_id does not match GTFS data",
            "GTFS-rt trip direction_id must match the direction_id in GTFS trips.txt",
            "- direction_id does not match");

    public static final ValidationRule E025 = new ValidationRule("E025", "ERROR", "stop_time_update departure time is before arrival time",
            "Within the same stop_time_update, arrival and departures times can be the same, or the departure time can be later than the arrival time - the departure time should never come before the arrival time.",
            "- departure time must be equal to or greater than arrival time");

    public static final ValidationRule E026 = new ValidationRule("E026", "ERROR", "Invalid vehicle position",
            "Vehicle position latitude must be between -90 and 90 (inclusive), and vehicle longitude must be between -180  and 180 (inclusive)",
            "- these are invalid WGS84 coordinates");

    public static final ValidationRule E027 = new ValidationRule("E027", "ERROR", "Invalid vehicle bearing",
            "Vehicle bearing must be between 0 and 360 degrees (inclusive)",
            "- bearing must be between 0 and 360 degrees (inclusive)");

    public static final ValidationRule E028 = new ValidationRule("E028", "ERROR", "Vehicle position outside agency coverage area",
            "The vehicle position should be inside the agency coverage area.  This is defined as within roughly 1/8 of a mile (200 meters) of the GTFS shapes.txt data, or stops.txt locations if the GTFS feed doesn't include shapes.txt.",
            "- vehicle should be within area");

    public static final ValidationRule E029 = new ValidationRule("E029", "ERROR", "Vehicle position far from trip shape",
            "The vehicle position should be within a certain distance of the GTFS shapes.txt data for the current trip unless there is a Service Alert with the Effect of DETOUR for this trip_id.",
            "- vehicle should be near trip shape or on DETOUR");

    public static final ValidationRule E030 = new ValidationRule("E030", "ERROR", "GTFS-rt alert trip_id does not belong to GTFS-rt alert route_id in GTFS trips.txt",
            "The alert.informed_entity.trip.trip_id should belong to the specified alert.informed_entity.route_id in GTFS trips.txt",
            "- informed_entity.route_id must match GTFS data");

    public static final ValidationRule E031 = new ValidationRule("E031", "ERROR", "Alert informed_entity.route_id does not match informed_entity.trip.route_id",
            "The alert.informed_entity.trip.route_id should be the same as the specified alert-informed_entity.route_id.",
            "- routes_ids must be the same");

    public static final ValidationRule E032 = new ValidationRule("E032", "ERROR", "Alert does not have an informed_entity",
            "All alerts must have at least one informed_entity.",
            "- alerts must have at least one informed_entity");

    public static final ValidationRule E033 = new ValidationRule("E033", "ERROR", "Alert informed_entity does not have any specifiers",
            "Alert informed_entity should have at least one specified value (route_id, trip_id, stop_id, etc) to which the alert applies.",
            "- alert informed_entity should have at least one specified value");

    public static final ValidationRule E034 = new ValidationRule("E034", "ERROR", "GTFS-rt agency_id does not exist in GTFS data",
            "All agency_ids provided in the GTFS-rt alert.informed_entity.agency_id should also exist in GTFS agency.txt",
            "does not exist in the GTFS data agency.txt");

    public static final ValidationRule E035 = new ValidationRule("E035", "ERROR", "GTFS-rt trip.trip_id does not belong to GTFS-rt trip.route_id in GTFS trips.txt",
            "The GTFS-rt trip.trip_id should belong to the specified trip.route_id in GTFS trips.txt",
            "in GTFS trips.txt");

    public static final ValidationRule E036 = new ValidationRule("E036", "ERROR", "Sequential stop_time_updates have the same stop_sequence",
            "Sequential GTFS-rt trip stop_time_updates should never have the same stop_sequence",
            "- stop_sequence must increase for each stop_time_update");

    public static final ValidationRule E037 = new ValidationRule("E037", "ERROR", "Sequential stop_time_updates have the same stop_id",
            "Sequential GTFS-rt trip stop_time_updates shouldn't have the same stop_id",
            "- sequential stop_ids should be different");

    public static final ValidationRule E038 = new ValidationRule("E038", "ERROR", "Invalid header.gtfs_realtime_version",
            "header.gtfs_realtime_version should be a valid value",
            "is invalid");

    public static final ValidationRule E039 = new ValidationRule("E039", "ERROR", "FULL_DATASET feeds should not include entity.is_deleted",
            "The entity.is_deleted field should only be included in GTFS-rt feeds with header.incrementality of DIFFERENTIAL",
            "- FULL_DATASET feeds should not include is_deleted field");

    public static final ValidationRule E040 = new ValidationRule("E040", "ERROR", "stop_time_update doesn't contain stop_id or stop_sequence",
            "All stop_time_updates must contain stop_id or stop_sequence - both fields cannot be left blank",
            "doesn't contain stop_id or stop_sequence");

    public static final ValidationRule E041 = new ValidationRule("E041", "ERROR", "trip doesn't have any stop_time_updates",
            "Unless a trip's schedule_relationship is CANCELED, a trip must have at least one stop_time_update",
            "doesn't have any stop_time_updates and isn't CANCELED");

    public static final ValidationRule E042 = new ValidationRule("E042", "ERROR", "arrival or departure provided for NO_DATA stop_time_update",
            "If a stop_time_update has a schedule_relationship of NO_DATA, then neither arrival nor departure should be provided",
            "and schedule_relationship of NO_DATA");

    public static final ValidationRule E043 = new ValidationRule("E043", "ERROR", "stop_time_update doesn't have arrival or departure",
            "If a stop_time_update doesn't have a schedule_relationship of SKIPPED or NO_DATA, then either arrival or departure must be provided",
            "doesn't have arrival or departure");

    public static final ValidationRule E044 = new ValidationRule("E044", "ERROR", "stop_time_update arrival/departure doesn't have delay or time",
            "stop_time_update.arrival and stop_time_update.departure must have either delay or time - both fields cannot be missing",
            "doesn't have delay or time");

    public static final ValidationRule E045 = new ValidationRule("E045", "ERROR", "GTFS-rt stop_time_update stop_sequence and stop_id do not match GTFS",
            "If GTFS-rt stop_time_update contains both stop_sequence and stop_id, the values must match the GTFS data in stop_times.txt",
            "- stop_ids should be the same");

    public static final ValidationRule E046 = new ValidationRule("E046", "ERROR", "GTFS-rt stop_time_update without time doesn't have arrival/departure_time in GTFS",
            "If only delay is provided in a stop_time_update arrival or departure (and not a time), then the GTFS stop_times.txt must contain arrival_times and/or departure_times for these corresponding stops.",
            "isn't set and GTFS doesn't have arrival/departure_time in stop_times.txt");

    public static final ValidationRule E047 = new ValidationRule("E047", "ERROR", "VehiclePosition and TripUpdate ID pairing mismatch",
            "If separate `VehiclePositions` and `TripUpdates` feeds are provided, `VehicleDescriptor` or `TripDescriptor` ID value pairing should match between the two feeds.",
            "- ID pairing between feeds should match");

    public static final ValidationRule E048 = new ValidationRule("E048", "ERROR", "header timestamp not populated",
            "Header timestamp must be populated for gtfs_realtime_version v2.0 and higher",
            "Header does not have a timestamp and gtfs_realtime_version is v2.0 or higher");

    public static final ValidationRule E049 = new ValidationRule("E049", "ERROR", "header incrementality not populated",
            "Header incrementality must be populated for gtfs_realtime_version v2.0 and higher",
            "Header does not have incrementality and gtfs_realtime_version is v2.0 or higher");

    public static final ValidationRule E050 = new ValidationRule("E050", "ERROR", "timestamp is in the future",
            "All timestamps must be less than the current time",
            "- the current time in milliseconds");

    public static final ValidationRule E051 = new ValidationRule("E051", "ERROR", "GTFS-rt stop_sequence not found in GTFS data",
            "All stop_time_update stop_sequences in GTFS-realtime data must appear in GTFS stop_times.txt for that trip",
            "that does not exist in GTFS stop_times.txt for this trip");

    public static final ValidationRule E052 = new ValidationRule("E052", "ERROR", "vehicle.id is not unique",
            "Each vehicle should have a unique ID",
            "which is used by more than one vehicle in the feed");

    private static List<ValidationRule> mAllRules = new ArrayList<>();

    /**
     * Returns a read-only list of all currently-defined validation rules
     * @return a read-only list of all currently-defined validation rules
     */
    public static synchronized List<ValidationRule> getRules() {
        if (mAllRules.isEmpty()) {
            // Use reflection to get the list of rules
            Field[] fields = ValidationRules.class.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    Class classType = field.getType();
                    if (classType == ValidationRule.class) {
                        ValidationRule rule = new ValidationRule();
                        try {
                            Object value = field.get(rule);
                            rule = (ValidationRule)value;
                            mAllRules.add(rule);
                        } catch (IllegalAccessException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
        // Return a read-only list of these rules so different threads can't modify the list
        return Collections.unmodifiableList(mAllRules);
    }
}