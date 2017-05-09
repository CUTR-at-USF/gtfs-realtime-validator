# Implemented rules

Rules are defined in the [`ValidationRules` class](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/src/main/java/edu/usf/cutr/gtfsrtvalidator/validation/ValidationRules.java).  Below are details of currently implemented rules.

### Table of Warnings

| Warning ID    | Warning Title             |
|---------------|---------------------------|
| [W001](#W001) | `timestamps` not populated
| [W002](#W002) | `vehicle_id` not populated
| [W003](#W003) | `VehiclePosition` and `TripUpdate` feed mismatch
| [W004](#W004) | `VehiclePosition` has unrealistic speed
| [W005](#W005) | Missing `vehicle_id` in `trip_update` for frequency-based `exact_times` = 0
| [W006](#W006) | trip_update missing trip_id
| [W007](#W007) | Refresh interval is more than 35 seconds
| [W008](#W008) | Header timestamp is older than 65 seconds

### Table of Errors

| Error ID      | Error Title         |
|---------------|---------------------------|
| [E001](#E001) | Not in POSIX time
| [E002](#E002) | Unsorted `stop_sequence`
| [E003](#E003) | GTFS-rt `trip_id` does not exist in GTFS data
| [E004](#E004) | GTFS-rt `route_id` does not exist in GTFS data
| [E006](#E006) | Missing required trip field for frequency-based exact_times = 0
| [E010](#E010) | `location_type` not `0` in `stops.txt` (Note that this is implemented but not executed because it's specific to GTFS - see [issue #126](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/126))
| [E011](#E011) | GTFS-rt `stop_id` does not exist in GTFS data
| [E012](#E012) | Header `timestamp` should be greater than or equal to all other timestamps
| [E013](#E013) | Frequency type 0 trip `schedule_relationship` should be `UNSCHEDULED` or empty
| [E015](#E015) | All `stop_ids` referenced in GTFS-rt feeds must have the `location_type` = 0
| [E016](#E016) | `trip_ids` with `schedule_relationship` `ADDED` must not be in GTFS data
| [E017](#E017) | GTFS-rt content changed but has the same header `timestamp`
| [E018](#E018) | GTFS-rt header `timestamp` decreased between two sequential iterations
| [E019](#E019) | GTFS-rt frequency type 1 trip `start_time` must be a multiple of GTFS `headway_secs` later than GTFS `start_time`
| [E020](#E020) | Invalid `start_time` format
| [E021](#E021) | Invalid `start_date` format
| [E022](#E022) | Sequential trip `stop_time_update` times are not increasing
| [E023](#E023) | trip `start_time` does not match first GTFS `arrival_time`
| [E024](#E024) | trip `direction_id` does not match GTFS data
| [E025](#E025) | `stop_time_update` departure time is before arrival time
| [E026](#E026) | Invalid vehicle `position`
| [E027](#E027) | Invalid vehicle `bearing`

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

`vehicle.position.speed` has an unrealistic speed that may be incorrect

<a name="W005"/>

### W005 - Missing `vehicle_id` in `trip_update` for frequency-based exact_times = 0

Frequency-based exact_times = 0 trip_updates should contain `vehicle_id`.  This helps disambiguate predictions in situations where more than one vehicle is running the same trip instance simultaneously.

<a name="W006"/>

### W006 - `trip_update` missing `trip_id`

`trip_updates` should include a `trip_id`.  A missing `trip_id` is usually an error in the feed (especially for frequency-based `exact_times` = 0 trips - see [E006](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/RULES.md#E006), although the section on "Alternative trip matching" includes one exception:

>Trips which are not frequency based may also be uniquely identified by a TripDescriptor including the combination of:
>
> * `route_id`
> * `direction_id`
> * `start_time`
> * `start_date`
>
> ...where `start_time` is the scheduled start time as defined in the static schedule, as long as the combination of ids provided resolves to a unique trip.

See:
* [`trip_update.trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#alternative-trip-matching)

<a name="W007"/>

### W007 - Refresh interval is more than 35 seconds

GTFS-realtime feeds should be refreshed at least every 30 seconds.

<a name="W008"/>

### W008 - Header timestamp is older than 65 seconds

The data in a GTFS-realtime feed should always be less than one minute old.

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

`stop_time_updates` for a given `trip_id` must be sorted by increasing stop_sequence.

Note that this currently implemented when `stop_sequence` is provided in the GTFS-rt feed, but not when `stop_sequence is omitted from the GTFS-rt feed (see [issue #159](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/159)).

<a name="E003"/>

### E003 - GTFS-rt `trip_id` does not exist in GTFS data

All `trip_ids` provided in the GTFS-rt feed must exist in the GTFS data, unless their `schedule_relationship` is set to `ADDED`.

<a name="E004"/>

### E004 - GTFS-rt `route_id` does not exist in GTFS data

All `route_ids` provided in the GTFS-rt feed must exist in the GTFS data

<a name="E006"/>

### E006 - Missing required trip field for frequency-based exact_times = 0

Frequency-based `exact_times` = 0 `trip_updates` must contain `trip_id`, `start_time`, and `start_date`.

See:
* [`trip_update.trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#systems-with-repeated-trip_ids)

<a name="E010"/>

### E010 - `location_type` not `0` in stops.txt

(Note that this is implemented but not executed because it's specific to GTFS - see [issue #126](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/126)

If location_type is used in `stops.txt`, all stops referenced in `stop_times.txt` must have `location_type` of `0`

<a name="E011"/>

### E011 - GTFS-rt `stop_id` does not exist in GTFS data

All `stop_ids` referenced in GTFS-rt feeds must exist in the GTFS data in `stops.txt`

<a name="E012"/>

### E012 - Header `timestamp` should be greater than or equal to all other timestamps

No timestamps for individual entities (TripUpdate, VehiclePosition, Alerts) in the feeds should be greater than the header timestamp.

<a name="E013"/>

### E013 - Frequency type 0 trip `schedule_relationship` should be `UNSCHEDULED` or empty

For frequency-based exact_times=0 trips, schedule_relationship should be `UNSCHEDULED` or empty.

<a name="E015"/>

### E015 - All `stop_ids` referenced in GTFS-rt feeds must have the `location_type` = 0

All `stop_ids` referenced in GTFS-rt feeds must have the `location_type` = 0 in GTFS `stops.txt`

<a name="E016"/>

### E016 - `trip_ids` with `schedule_relationship` `ADDED` must not be in GTFS data

Trips that have a `schedule_relationship` of `ADDED` must not be included in the GTFS data

<a name="E017"/>

### E017 - GTFS-rt content changed but has the same header `timestamp`

The GTFS-rt header `timestamp` value should always change if the feed contents change - the feed contents must not change without updating the header `timestamp`.

See:
* [`header.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)

*Common mistakes* - If there are multiple instances of GTFS-realtime feed behind a load balancer, each instance may be pulling information from the real-time data source and publishing it to consumers slightly out of sync.  If a GTFS-rt consumer makes two back-to-back requests, and each request is served by a different GTFS-rt feed instance, the same feed contents could potentially be returned to the consumer with different timestamps. 

*Possible solution* - Configure the load balancer for "sticky routes", so that the consumer always receives the GTFS-rt feed contents from the same GTFS-rt instance. 

<a name="E018"/>

### E018 - GTFS-rt header `timestamp` decreased between two sequential iterations

The GTFS-rt header `timestamp` should be monotonically increasing -  it should always be the same value or greater than previous feed iterations if the feed contents are different.

See:
* [`header.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)

*Common mistakes* - If there are multiple instances of GTFS-realtime feed behind a load balancer, each instance may be pulling information from the real-time data source and publishing it to consumers slightly out of sync.  If a GTFS-rt consumer makes two back-to-back requests, and each request is served by a different GTFS-rt feed instance, the same feed contents could potentially be returned to the consumer with the most recent feed response having a timestamp that is less than the previous feed response.

*Possible solution* - Configure the load balancer for "sticky routes", so that the GTFS-rt consumer always receives the GTFS-rt feed contents from the same GTFS-rt instance.

<a name="E019"/>

### E019 - GTFS-rt frequency type 1 trip `start_time` must be a multiple of GTFS data `start_time`

For frequency-based trips defined in `frequencies.txt` with `exact_times` = 1, the GTFS-rt trip `start_time` must be some multiple (including zero) of `headway_secs` later than the `start_time` in file `frequencies.txt` for the corresponding time period.  Note that this doesn't not apply to frequency-based trips defined in `frequencies.txt` with `exact_times` = 0.

<a name="E020"/>

### E020 - Invalid `start_time` format

`start_time` must be in the format `25:15:35`.  Note that times can exceed 24 hrs if service goes into the next service day.

See:
* [trip.start_Time](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E021"/>

### E021 - Invalid `start_date` format

`start_date` must be in the `YYYYMMDD` format.

See:
* [trip.start_date](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E022"/>

### E022 - Sequential trip `stop_time_update` times are not increasing

`stop_time_update` arrival/departure times between sequential stops should always increase - they should never be the same or decrease.

<a name="E023"/>

### E023 - trip `start_time` does not match first GTFS `arrival_time`

For normal scheduled trips (i.e., not defined in `frequencies.txt`), the GTFS-realtime trip `start_time` must match the first GTFS `arrival_time` in `stop_times.txt` for this trip.

<a name="E024"/>

### E024 - trip `direction_id` does not match GTFS data

GTFS-rt trip `direction_id` must match the `direction_id` in GTFS `trips.txt`.

<a name="E025"/>

### E025 - `stop_time_update` departure time is before arrival time

Within the same `stop_time_update`, arrival and departures times can be the same, or the departure time can be later than the arrival time - the departure time should never come before the arrival time.

<a name="E026"/>

### E026 - Invalid vehicle `position`

Vehicle position must be valid WGS84 coordinates - latitude must be between -90 and 90 (inclusive), and vehicle longitude must be between -180  and 180 (inclusive).

See:
* [vehicle.position](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)

<a name="E027"/>

### E027 - Invalid vehicle `bearing`

Vehicle bearing must be between 0 and 360 degrees (inclusive).  The GTFS-rt spec says bearing is:

>...in degrees, clockwise from True North, i.e., 0 is North and 90 is East. This can be the compass bearing, or the direction towards the next stop or intermediate location. This should not be deduced from the sequence of previous positions, which clients can compute from previous data.

See:
* [vehicle.position.bearing](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)