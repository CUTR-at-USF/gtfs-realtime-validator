# Implemented rules

Rules are declared in the [`ValidationRules` class](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/src/main/java/edu/usf/cutr/gtfsrtvalidator/validation/ValidationRules.java).  Below are details of currently implemented rules.

### Table of Errors

| Error ID      | Error Title         |
|---------------|---------------------------|
| [E001](#E001) | Not in POSIX time
| [E002](#E002) | Unsorted `stop_sequence`
| [E003](#E003) | GTFS-rt `trip_id` does not exist in GTFS data
| [E004](#E004) | GTFS-rt `route_id` does not exist in GTFS data
| [E006](#E006) | Missing required trip field for frequency-based `exact_times` = 0
| [E010](#E010) | `location_type` not `0` in `stops.txt` (Note that this is implemented but not executed because it's specific to GTFS - see [issue #126](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/126))
| [E011](#E011) | GTFS-rt `stop_id` does not exist in GTFS data
| [E012](#E012) | Header `timestamp` should be greater than or equal to all other `timestamps`
| [E013](#E013) | Frequency type 0 trip `schedule_relationship` should be `UNSCHEDULED` or empty
| [E015](#E015) | All `stop_ids` referenced in GTFS-rt feeds must have the `location_type` = 0
| [E016](#E016) | `trip_ids` with `schedule_relationship` `ADDED` must not be in GTFS data
| [E017](#E017) | GTFS-rt content changed but has the same header `timestamp`
| [E018](#E018) | GTFS-rt header `timestamp` decreased between two sequential iterations
| [E019](#E019) | GTFS-rt frequency type 1 trip `start_time` must be a multiple of GTFS `headway_secs` later than GTFS `start_time`
| [E020](#E020) | Invalid `start_time` format
| [E021](#E021) | Invalid `start_date` format
| [E022](#E022) | Sequential `stop_time_update` times are not increasing
| [E023](#E023) | trip `start_time` does not match first GTFS `arrival_time`
| [E024](#E024) | trip `direction_id` does not match GTFS data
| [E025](#E025) | `stop_time_update` departure time is before arrival time
| [E026](#E026) | Invalid vehicle `position`
| [E027](#E027) | Invalid vehicle `bearing`
| [E028](#E028) | Vehicle `position` outside agency coverage area
| [E029](#E029) | Vehicle `position` far from trip shape
| [E030](#E030) | GTFS-rt alert `trip_id` does not belong to GTFS-rt alert `route_id` in GTFS `trips.txt`
| [E031](#E031) | Alert `informed_entity.route_id` does not match `informed_entity.trip.route_id`
| [E032](#E032) | Alert does not have an `informed_entity`
| [E033](#E033) | Alert `informed_entity` does not have any specifiers
| [E034](#E034) | GTFS-rt `agency_id` does not exist in GTFS data
| [E035](#E035) | GTFS-rt `trip.trip_id` does not belong to GTFS-rt `trip.route_id` in GTFS `trips.txt`
| [E036](#E036) | Sequential `stop_time_updates` have the same `stop_sequence`
| [E037](#E037) | Sequential `stop_time_updates` have the same `stop_id`

### Table of Warnings

| Warning ID    | Warning Title             |
|---------------|---------------------------|
| [W001](#W001) | `timestamps` not populated
| [W002](#W002) | `vehicle_id` not populated
| [W003](#W003) | `VehiclePosition` and `TripUpdate` feed mismatch
| [W004](#W004) | `VehiclePosition` has unrealistic speed
| [W005](#W005) | Missing `vehicle_id` in `trip_update` for frequency-based `exact_times` = 0
| [W006](#W006) | `trip_update` missing `trip_id`
| [W007](#W007) | Refresh interval is more than 35 seconds
| [W008](#W008) | Header `timestamp` is older than 65 seconds

# Errors

<a name="E001"/>

### E001 - Not in POSIX time

All times and timestamps must be in [POSIX time](https://en.wikipedia.org/wiki/Unix_time) (i.e., number of **seconds** since January 1st 1970 00:00:00 UTC).

*Common mistakes* - Accidentally using Java's `System.currentTimeMillis()`, which is the number of **milliseconds** since January 1st 1970 00:00:00 UTC.  

*Possible solution* - Use `TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())` to convert from milliseconds to seconds.

#### References:
* [`header.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)
* [`trip_update.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripupdate)
* [`vehicle_postion.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicleposition)
* [`stop_time_update.arrival/departure.time`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeevent)
* [`alert.active_period.start` and `alert.active_period.end`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-timerange)

<a name="E002"/>

### E002 - Unsorted `stop_sequence`

`stop_time_updates` for a given `trip_id` must be sorted by increasing stop_sequence.

Note that this currently implemented when `stop_sequence` is provided in the GTFS-rt feed, but not when `stop_sequence` is omitted from the GTFS-rt feed (see [issue #159](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/159)).

*Common mistakes* - Assuming that the GTFS `stop_times.txt` file will be grouped by `trip_id` and sorted by `stop_sequence` - while sorting the data is a good practice, it's not strictly required by the spec.   

*Possible solution* - Group the GTFS `stop_times.txt` records by `trip_id` and sort by `stop_sequence`.

#### References:
* [`trip_update.stop_time_updates`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates)

<a name="E003"/>

### E003 - GTFS-rt `trip_id` does not exist in GTFS data

All `trip_ids` provided in the GTFS-rt feed must exist in the GTFS data, unless their `schedule_relationship` is set to `ADDED`.

<a name="E004"/>

### E004 - GTFS-rt `route_id` does not exist in GTFS data

All `route_ids` provided in the GTFS-rt feed must exist in the GTFS data

<a name="E006"/>

### E006 - Missing required trip field for frequency-based exact_times = 0

Frequency-based `exact_times` = 0 `trip_updates` must contain `trip_id`, `start_time`, and `start_date`.

#### References:
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

*Common mistakes* - If there are multiple instances of GTFS-realtime feed behind a load balancer, each instance may be pulling information from the real-time data source and publishing it to consumers slightly out of sync.  If a GTFS-rt consumer makes two back-to-back requests, and each request is served by a different GTFS-rt feed instance, the same feed contents could potentially be returned to the consumer with different timestamps. 

*Possible solution* - Configure the load balancer for "sticky routes", so that the consumer always receives the GTFS-rt feed contents from the same GTFS-rt instance.
 
#### References:
 * [`header.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)

<a name="E018"/>

### E018 - GTFS-rt header `timestamp` decreased between two sequential iterations

The GTFS-rt header `timestamp` should be monotonically increasing -  it should always be the same value or greater than previous feed iterations if the feed contents are different.

*Common mistakes* - If there are multiple instances of GTFS-realtime feed behind a load balancer, each instance may be pulling information from the real-time data source and publishing it to consumers slightly out of sync.  If a GTFS-rt consumer makes two back-to-back requests, and each request is served by a different GTFS-rt feed instance, the same feed contents could potentially be returned to the consumer with the most recent feed response having a timestamp that is less than the previous feed response.

*Possible solution* - Configure the load balancer for "sticky routes", so that the GTFS-rt consumer always receives the GTFS-rt feed contents from the same GTFS-rt instance.

#### References:
* [`header.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)

<a name="E019"/>

### E019 - GTFS-rt frequency type 1 trip `start_time` must be a multiple of GTFS data `start_time`

For frequency-based trips defined in `frequencies.txt` with `exact_times` = 1, the GTFS-rt trip `start_time` must be some multiple (including zero) of `headway_secs` later than the `start_time` in file `frequencies.txt` for the corresponding time period.  Note that this doesn't not apply to frequency-based trips defined in `frequencies.txt` with `exact_times` = 0.

<a name="E020"/>

### E020 - Invalid `start_time` format

`start_time` must be in the format `25:15:35`.  Note that times can exceed 24 hrs if service goes into the next service day.

#### References:
* [trip.start_time](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E021"/>

### E021 - Invalid `start_date` format

`start_date` must be in the `YYYYMMDD` format.

#### References:
* [trip.start_date](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E022"/>

### E022 - Sequential `stop_time_update` times are not increasing

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

#### References:
* [vehicle.position](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)

<a name="E027"/>

### E027 - Invalid vehicle `bearing`

Vehicle bearing must be between 0 and 360 degrees (inclusive).  The GTFS-rt spec says bearing is:

>...in degrees, clockwise from True North, i.e., 0 is North and 90 is East. This can be the compass bearing, or the direction towards the next stop or intermediate location. This should not be deduced from the sequence of previous positions, which clients can compute from previous data.

#### References:
* [vehicle.position.bearing](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)

<a name="E028"/>

### E028 - Vehicle `position` outside agency coverage area

The vehicle `position` should be inside the agency coverage area.  This is defined as within roughly 1/8 of a mile (200 meters) of the GTFS `shapes.txt` data, or `stops.txt` locations if the GTFS feed doesn't include `shapes.txt`.

Buffer is defined by `GtfsMetadata.REGION_BUFFER_METERS`, and is currently 1609 meters (roughly 1 mile).

#### References:
* [vehicle.position](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)

<a name="E029"/>

### E029 - Vehicle `position` far from trip shape

The vehicle `position` should be within the buffer of the GTFS `shapes.txt` data for the current trip unless there is an `alert` with the `effect` of `DETOUR` for this `trip_id`.

Buffer is defined by `GtfsMetadata.TRIP_BUFFER_METERS`, and is currently 200 meters (roughly 1/8 of a mile).

#### References:
* [GTFS shapes.txt](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#shapestxt)
* [vehicle.position](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)
* [alert.effect.DETOUR](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-effect)

<a name="E030"/>

### E030 - GTFS-rt alert `trip_id` does not belong to GTFS-rt alert `route_id` in GTFS `trips.txt`

The GTFS-rt `alert.informed_entity.trip.trip_id` should belong to the specified GTFS-rt `alert.informed_entity.route_id` in GTFS `trips.txt`.

#### References:
* [alert.informed_entity](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)
* [GTFS trips.txt](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#tripstxt)

<a name="E031"/>

### E031 - Alert `informed_entity.route_id` does not match `informed_entity.trip.route_id`

The `alert.informed_entity.trip.route_id` should be the same as the specified `alert.informed_entity.route_id`.

#### References:
* [alert.informed_entity](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)
* [alert.informed_entity.trip](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E032"/>

### E032 - Alert does not have an `informed_entity`

All alerts must have at least one `informed_entity`.

#### References:
* [alert.informed_entity](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)

<a name="E033"/>

### E033 - Alert `informed_entity` does not have any specifiers

Alert `informed_entity` should have at least one specified value (`route_id`, `trip_id`, `stop_id`, etc) to which the alert applies.

#### References:
* [alert.informed_entity](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)

<a name="E034"/>

### E034 - GTFS-rt `agency_id` does not exist in GTFS data

All `agency_ids` provided in the GTFS-rt `alert.informed_entity.agency_id` should also exist in GTFS `agency.txt`.

#### References:
* [alert.informed_entity](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)

<a name="E035"/>

### E035 - GTFS-rt `trip.trip_id` does not belong to GTFS-rt `trip.route_id` in GTFS trips.txt

The GTFS-rt `trip.trip_id` should belong to the specified `trip.route_id` in GTFS `trips.txt`.

#### References:
* [trip.trip_id](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E036"/>

### E036 - Sequential `stop_time_updates` have the same `stop_sequence`

Sequential GTFS-rt trip `stop_time_updates` should never have the same `stop_sequence` - `stop_sequence` must increase for each `stop_time_update`.

#### References:
* [`trip.stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates)

<a name="E036"/>

### E037 - Sequential `stop_time_updates` have the same `stop_id`

Sequential GTFS-rt trip `stop_time_updates` shouldn't have the same `stop_id` - sequential `stop_ids` should be different.  If a `stop_id` is visited more than once in a trip (i.e., a loop), and if no `stop_time_updates` in the loop are provided in the feed, and if the `stop_sequence` field of the stop where the loop starts/stops is provided in the GTFS-rt feed for the given `stop_id`, then this may not be an error.  

#### References:
* [`trip.stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates)

# Warnings

<a name="W001"/>

### W001 - `timestamp` not populated

`timestamps` should be populated for `FeedHeader`, `TripUpdates`, `VehiclePositions`, and `Alerts`.  

Including `timestamps` for each entity type enhances the transit rider experience, as consumers can show `timestamp` information to end users give them an idea of how old certain information is.  

For example, when a vehicle position is shown on a map, the marker may say "Data updated 17 sec ago" (see screenshot below).  If vehicle position `timestamps` aren't included, then the consumer must use the GTFS-rt header `timestamp`, which may be much more recent than the actual vehicle position, resulting in misleading information being show to end users.

![image](https://cloud.githubusercontent.com/assets/928045/26214158/55b82cb4-3bc9-11e7-9421-0e029cf198cb.png)

<a name="W002"/>

### W002 - `vehicle_id` not populated

`vehicle_id` should be populated for TripUpdates and VehiclePositions.  

Populating `vehicle_ids` in TripUpdates is important so consumers can relate a given arrival/departure prediction to a particular vehicle.

<a name="W003"/>

### W003 - `VehiclePosition` and `TripUpdate` feed mismatch

If separate vehicle positions and trip updates feeds are provided, `VehicleDescriptor` or `TripDescriptor` values should match between the two feeds.  

In other words, if the `VehiclePosition` has a `vehicle_id` A that is assigned to `trip_id` 4, then the `TripUpdate` feed should have a prediction for `trip_id` 4 that includes a reference to `vehicle_id` A.

<a name="W004"/>

### W004 - VehiclePosition has unrealistic speed

`vehicle.position.speed` has an unrealistic speed that may be incorrect.  

Speeds are flagged as unrealistic if they are greater than `VehicleValidator.MAX_REALISTIC_SPEED_METERS_PER_SECOND`, which is currently set to 26 meters per second (approx. 60 miles per hour). 

*Common mistakes* - Accidentally setting the speed value in *miles per hour*, instead of *meters per second*. 

*Possible solution* - Check to make sure the speed units are *meters per second*.

#### References:
* [vehicle.position.speed](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)

<a name="W005"/>

### W005 - Missing `vehicle_id` in `trip_update` for frequency-based exact_times = 0

Frequency-based exact_times = 0 trip_updates should contain `vehicle_id`.  This helps disambiguate predictions in situations where more than one vehicle is running the same trip instance simultaneously.

#### References:
* [`trip_update.trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#alternative-trip-matching)
* [GTFS `frequencies.txt` `exact_times` = 0](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#frequenciestxt)

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

#### References:
* [`trip_update.trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#alternative-trip-matching)

<a name="W007"/>

### W007 - Refresh interval is more than 35 seconds

GTFS-realtime feeds should be refreshed at least every 30 seconds.

<a name="W008"/>

### W008 - Header `timestamp` is older than 65 seconds

The data in a GTFS-realtime feed should always be less than one minute old.