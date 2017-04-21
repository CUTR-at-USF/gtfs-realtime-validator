# Implemented rules

### Table of Warnings

| Warning ID    | Warning Title             |
|---------------|---------------------------|
| [W001](#W001) | `timestamps` not populated
| [W002](#W002) | `vehicle_id` not populated
| [W003](#W003) | `VehiclePosition` and `TripUpdate` feed mismatch
| [W004](#W004) | VehiclePosition has unrealistic speed


### Table of Errors

| Error ID      | Error Title         |
|---------------|---------------------------|
| [E001](#E001) | Not in POSIX time
| [E002](#E002) | Unsorted `stop_sequence`
| [E003](#E003) | GTFS-rt `trip_id` does not appear in GTFS data
| [E004](#E004) | GTFS-rt `route_id` does not appear in GTFS data
| [E010](#E010) | `location_type` not `0` in `stops.txt` (Note that this is implemented but not executed because it's specific to GTFS - see #126)
| [E011](#E011) | GTFS-rt `stop_id` does not appear in GTFS data
| [E012](#E012) | Header timestamp should be greater than or equal to all other timestamps
| [E013](#E013) | Frequency type 0 trip schedule_relationship should be UNSCHEDULED or empty
| [E015](#E015) | All stop_ids referenced in GTFS-rt feeds must have the location_type = 0
| [E016](#E016) | trip_ids with schedule_relationship ADDED must not be in GTFS data

# Warnings

<a name="W001"/>

### W001 - `timestamp` not populated

`timestamps` should be populated for `FeedHeader`, `TripUpdates`, `VehiclePositions`, and `Alerts`

<a name="W002"/>

### W002 - `vehicle_id` not populated

`vehicle_id` should be populated for TripUpdates and VehiclePositions

<a name="W003"/>

### W003 - `VehiclePosition` and `TripUpdate` feed mismatch

If both vehicle positions and trip updates are provided, `VehicleDescriptor` or `TripDescriptor` values should match between the two feeds

<a name="W004"/>

### W004 - VehiclePosition has unrealistic speed

vehicle.position.speed has an unrealistic speed that may be incorrect

# Errors

<a name="E001"/>

### E001 - Not in POSIX time

All times and timestamps must be in POSIX time (i.e., number of **seconds** since January 1st 1970 00:00:00 UTC).

See:
* [`header.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)
* [`trip_update.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripupdate)
* [`vehicle_postion.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicleposition)
* [`stop_time_update.arrival/departure.time`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeevent)
* [`alert.active_period.start` and `alert.active_period.end`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-timerange)

*Common mistakes* - Accidentally using Java's `System.currentTimeMillis()`, which is the number of **milliseconds** since January 1st 1970 00:00:00 UTC.  

*Possible solution* - Use `TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())` to convert from milliseconds to seconds.

<a name="E002"/>

### E002 - Unsorted `stop_sequence`

`stop_time_updates` for a given `trip_id` must be sorted by increasing stop_sequence (this should always be enforced whether or not the feed contains the stop_sequence field). A TripUpdate can have multiple stop_time_updates (e.g., one prediction per stop) - so, this shouldn't be monitored across multiple feed messages, just in a single message.

<a name="E003"/>

### E003 - GTFS-rt `trip_id` does not appear in GTFS data

All `trip_ids` provided in the GTFS-rt feed must appear in the GTFS data, unless their `schedule_relationship` is set to `ADDED`.

<a name="E004"/>

### E004 - GTFS-rt `route_id` does not appear in GTFS data

All `route_ids` provided in the GTFS-rt feed must appear in the GTFS data

<a name="E010"/>

### E010 - `location_type` not `0` in stops.txt

(Note that this is implemented but not executed because it's specific to GTFS - see #126)

If location_type is used in `stops.txt`, all stops referenced in `stop_times.txt` must have `location_type` of `0`

<a name="E011"/>

### E011 - GTFS-rt `stop_id` does not appear in GTFS data

All `stop_ids` referenced in GTFS-rt feeds must appear in the GTFS data in `stops.txt`

<a name="E012"/>

### E012 - Header timestamp should be greater than or equal to all other timestamps

No timestamps for individual entities (TripUpdate, VehiclePosition, Alerts) in the feeds should be greater than the header timestamp.

<a name="E013"/>

### E013 - Frequency type 0 trip schedule_relationship should be UNSCHEDULED or empty

For frequency-based exact_times=0 trips, schedule_relationship should be UNSCHEDULED or empty.

<a name="E015"/>

### E015 - All stop_ids referenced in GTFS-rt feeds must have the location_type = 0

All stop_ids referenced in GTFS-rt feeds must have the location_type = 0 in GTFS `stops.txt`

<a name="E016"/>

### E016 - trip_ids with schedule_relationship ADDED must not be in GTFS data

Trips that have a schedule_relationship of ADDED must not be included in the GTFS data