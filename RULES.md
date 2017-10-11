# Implemented rules

Rules are declared in the [`ValidationRules` class](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/src/main/java/edu/usf/cutr/gtfsrtvalidator/validation/ValidationRules.java).  Below are details of currently implemented rules.

### Table of Errors

| Error ID      | Error Title         |
|---------------|---------------------------|
| [E001](#E001) | Not in POSIX time
| [E002](#E002) | `stop_time_updates` not strictly sorted
| [E003](#E003) | GTFS-rt `trip_id` does not exist in GTFS data
| [E004](#E004) | GTFS-rt `route_id` does not exist in GTFS data
| [E006](#E006) | Missing required trip field for frequency-based `exact_times` = 0
| [E009](#E009) | GTFS-rt `stop_sequence` isn't provided for `trip` that visits same `stop_id` more than once
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
| [E038](#E038) | Invalid `header.gtfs_realtime_version`
| [E039](#E039) | `FULL_DATASET` feeds should not include `entity.is_deleted`
| [E040](#E040) | `stop_time_update` doesn't contain `stop_id` or `stop_sequence`
| [E041](#E041) | `trip` doesn't have any `stop_time_updates`
| [E042](#E042) | `arrival` or `departure` provided for `NO_DATA` `stop_time_update`
| [E043](#E043) | `stop_time_update` doesn't have `arrival` or `departure`
| [E044](#E044) | `stop_time_update` `arrival/departure` doesn't have `delay` or `time`
| [E045](#E045) | GTFS-rt `stop_time_update` `stop_sequence` and `stop_id` do not match GTFS
| [E046](#E046) | GTFS-rt `stop_time_update` without `time` doesn't have arrival/departure time in GTFS
| [E047](#E047) | `VehiclePosition` and `TripUpdate` ID pairing mismatch
| [E048](#E048) | `header` `timestamp` not populated (GTFS-rt v2.0 and higher)
| [E049](#E049) | `header` `incrementality` not populated (GTFS-rt v2.0 and higher)

### Table of Warnings

| Warning ID    | Warning Title             |
|---------------|---------------------------|
| [W001](#W001) | `timestamps` not populated
| [W002](#W002) | `vehicle_id` not populated
| [W003](#W003) | ID in one feed missing from the other
| [W004](#W004) | vehicle `speed` is unrealistic
| [W005](#W005) | Missing `vehicle_id` in `trip_update` for frequency-based `exact_times` = 0
| [W006](#W006) | `trip_update` missing `trip_id`
| [W007](#W007) | Refresh interval is more than 35 seconds
| [W008](#W008) | Header `timestamp` is older than 65 seconds
| [W009](#W009) | `schedule_relationship` not populated

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

### E002 - `stop_time_updates` not strictly sorted

`stop_time_updates` for a given `trip_id` must be strictly ordered by increasing `stop_sequence` - this also means that no `stop_sequence` should be repeated. 

From [Stop Time Updates description](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates):

>Updates should be sorted by stop_sequence (or stop_ids in the order they occur in the trip).

From [GTFS `stop_times.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#stop_timestxt):

>The values for stop_sequence must be non-negative integers, and they must increase along the trip.

This validation rule is implemented for both when `stop_sequence` is provided in the GTFS-rt feed, and when `stop_sequence` is omitted from the GTFS-rt feed.

*Common mistakes* - Assuming that the GTFS `stop_times.txt` file will be grouped by `trip_id` and sorted by `stop_sequence` - while sorting the data is a good practice, it's not strictly required by the spec.   

*Possible solution* - Group the GTFS `stop_times.txt` records by `trip_id` and sort by `stop_sequence`.  Also, make sure that no `stop_sequence` is repeated in GTFS `stop_times.txt`.

#### References:
* [Stop Time Updates description](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates)
* [`stop_time_update` reference](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)
* [GTFS `stop_times.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#stop_timestxt)

<a name="E003"/>

### E003 - GTFS-rt `trip_id` does not exist in GTFS data

All `trip_ids` provided in the GTFS-rt feed must exist in the GTFS data, unless their `schedule_relationship` is set to `ADDED`.

[`trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor) says:

>`trip_id` - The trip_id from the GTFS feed that this selector refers to.

[`schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship-1) says:

>If a trip is done in accordance with temporary schedule, not reflected in GTFS, then it shouldn't be marked as SCHEDULED, but marked as ADDED...
>
>`ADDED` - An extra trip that was added in addition to a running schedule, for example, to replace a broken vehicle or to respond to sudden passenger load.

#### References:
* [`trip.trip_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)
* [`trip.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship-1)
* [GTFS `trips.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#tripstxt)

<a name="E004"/>

### E004 - GTFS-rt `route_id` does not exist in GTFS data

All `route_ids` provided in the GTFS-rt feed must exist in the GTFS data.

[`trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor) says:

>`route_id` - The route_id from the GTFS that this selector refers to.

#### References:
* [`trip.route_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)
* [GTFS `routes.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#routestxt)

<a name="E006"/>

### E006 - Missing required trip field for frequency-based exact_times = 0

Frequency-based `exact_times` = 0 `trip_updates` must contain `trip_id`, `start_time`, and `start_date`.

#### References:
* [`trip_update.trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#systems-with-repeated-trip_ids)

<a name="E009"/>

### E009 - GTFS-rt `stop_sequence` isn't provided for `trip` that visits same `stop_id` more than once

If a GTFS `trip` contains multiple references to the same `stop_id` (i.e., the vehicle visits the same `stop_id` more than once in the same trip), then GTFS-rt `stop_time_updates` for this trip must include `stop_sequence`.

From [`stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate):

>If the same stop_id is visited more than once in a trip, then stop_sequence should be provided in all StopTimeUpdates for that stop_id on that trip.

#### References:
* [`trip_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripupdate)

<a name="E010"/>

### E010 - `location_type` not `0` in stops.txt

(Note that this is implemented but not executed because it's specific to GTFS - see [issue #126](https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/126)

If location_type is used in `stops.txt`, all stops referenced in `stop_times.txt` must have `location_type` of `0`

<a name="E011"/>

### E011 - GTFS-rt `stop_id` does not exist in GTFS data

All `stop_ids` referenced in GTFS-rt feeds must exist in the GTFS data in `stops.txt`.

From [`stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)):

>`stop_id` - Must be the same as in stops.txt in the corresponding GTFS feed.

From [`position`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicleposition):

>`stop_id` - Identifies the current stop. The value must be the same as in stops.txt in the corresponding GTFS feed.

#### References:
* [`stop_time_update.stop_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)
* [`position.stop_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicleposition)
* [`informed_entity.stop_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)
* [GTFS `stops.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#stopstxt)

<a name="E012"/>

### E012 - Header `timestamp` should be greater than or equal to all other timestamps

No timestamps for individual entities (TripUpdate, VehiclePosition, Alerts) in the feeds should be greater than the header timestamp.

From [`header`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader):

>`timestamp` - This timestamp identifies the moment when the content of this feed has been created (in server time). In POSIX time (i.e., number of seconds since January 1st 1970 00:00:00 UTC). To avoid time skew between systems producing and consuming realtime information it is strongly advised to derive timestamp from a time server. It is completely acceptable to use Stratum 3 or even lower strata servers since time differences up to a couple of seconds are tolerable.

#### References:
* [`header`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)

<a name="E013"/>

### E013 - Frequency type 0 trip `schedule_relationship` should be `UNSCHEDULED` or empty

For frequency-based exact_times=0 trips, schedule_relationship should be `UNSCHEDULED` or empty.

From [Trip Updates -> Trip Descriptor description](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#trip-descriptor):

>`UNSCHEDULED` - This trip is running and is never associated with a schedule. For example, if there is no schedule and the buses run on a shuttle service.

From [`trip_update.trip.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship-1):

>`UNSCHEDULED` - A trip that is running with no schedule associated to it, for example, if there is no schedule at all.

#### References:
* [Trip Updates -> Trip Descriptor description](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#trip-descriptor)
* [`trip_update.trip.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship-1)

<a name="E015"/>

### E015 - All `stop_ids` referenced in GTFS-rt feeds must have the `location_type` = 0

All `stop_ids` referenced in GTFS-rt feeds must have the `location_type` = 0 in GTFS `stops.txt`.

From [GTFS `stop_times.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#stop_timestxt):

>`stop_id` - ...The stop_id is referenced from the stops.txt file. If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0.

#### References:
* [GTFS `stop_times.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#stop_timestxt)
* [GTFS `stops.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#stopstxt)
* [`stop_time_update.stop_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)
* [`position.stop_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicleposition)
* [`informed_entity.stop_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)

<a name="E016"/>

### E016 - `trip_ids` with `schedule_relationship` `ADDED` must not be in GTFS data

Trips that have a `schedule_relationship` of `ADDED` must **NOT** be included in the GTFS data.

From [`trip.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship-1):

>`ADDED` - An extra trip that was added in addition to a running schedule, for example, to replace a broken vehicle or to respond to sudden passenger load.

From [Trip Updates -> Trip Descriptor description](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#trip-descriptor):

>Added - This trip was not scheduled and has been added. For example, to cope with demand, or replace a broken down vehicle.

#### References:
* [`trip.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship-1)
* [Trip Updates -> Trip Descriptor description](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#trip-descriptor)
* [`trip.trip_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)
* [GTFS `trips.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#tripstxt)

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

From [`trip.start_time`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor):

>`start_time` - ...If the trip corresponds to exact_times=1 GTFS record, then start_time must be some multiple (including zero) of headway_secs later than frequencies.txt start_time for the corresponding time period.

#### References:
* [`trip.start_time`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)
* [`trip_update.trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#alternative-trip-matching)
* [GTFS `frequencies.txt` `exact_times` = 1](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#frequenciestxt)

<a name="E020"/>

### E020 - Invalid `start_time` format

`start_time` must be in the format `25:15:35`.  Note that times can exceed 24 hrs if service goes into the next service day.

From [`trip.start_time`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor):

>`start_time` - ...Format and semantics of the field is same as that of GTFS/frequencies.txt/start_time, e.g., 11:15:35 or 25:15:35.

#### References:
* [`trip.start_time`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E021"/>

### E021 - Invalid `start_date` format

`start_date` must be in the `YYYYMMDD` format.

From [`trip.start_date`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor):

>`start_date` - The scheduled start date of this trip instance...In YYYYMMDD format.

#### References:
* [`trip.start_date`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E022"/>

### E022 - Sequential `stop_time_update` times are not increasing

`stop_time_update` arrival/departure times between sequential stops should always increase - they should never be the same or decrease.

#### References:
* [Stop Time Updates description](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates)
* [`stop_time_update` reference](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)

<a name="E023"/>

### E023 - trip `start_time` does not match first GTFS `arrival_time`

For normal scheduled trips (i.e., not defined in `frequencies.txt`), the GTFS-realtime trip `start_time` must match the first GTFS `arrival_time` in `stop_times.txt` for this trip.

From [`trip.start_time`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor):

>`start_time` - The initially scheduled start time of this trip instance. When the trip_id corresponds to a non-frequency-based trip, this field should either be omitted or be equal to the value in the GTFS feed.

*Common mistakes* - Accidentally providing a GTFS-realtime time that is modulo 24hr, such as `00:02:00`, when that trip start time in GTFS `stop_times.txt` is after midnight of the service day, such as `24:02:00`  

*Possible solution* - Make sure that any `start_times` in GTFS-realtime match that same trip start time in GTFS `stop_times.txt`, especially if the trip starts after midnight of the service day.

#### References:
* [`trip.start_time`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)
* [GTFS `stop_times.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#stop_timestxt)

<a name="E024"/>

### E024 - trip `direction_id` does not match GTFS data

GTFS-rt trip `direction_id` must match the `direction_id` in GTFS `trips.txt`.

From [`trip.direction_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor):

>`direction_id` - The direction_id from the GTFS feed trips.txt file, indicating the direction of travel for trips this selector refers to.

#### References:
* [`trip.direction_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)
* [GTFS `trips.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#tripstxt)

<a name="E025"/>

### E025 - `stop_time_update` departure time is before arrival time

Within the same `stop_time_update`, arrival and departures times can be the same, or the departure time can be later than the arrival time - the departure time should never come before the arrival time.

#### References:
* [Stop Time Updates description](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates)
* [`stop_time_update` reference](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)

<a name="E026"/>

### E026 - Invalid vehicle `position`

Vehicle position must be valid WGS84 coordinates - latitude must be between -90 and 90 (inclusive), and vehicle longitude must be between -180  and 180 (inclusive).

From [`vehicle.position`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position):

>* `latitude` - Degrees North, in the WGS-84 coordinate system.
>* `longitude` - Degrees East, in the WGS-84 coordinate system.

#### References:
* [`vehicle.position`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)

<a name="E027"/>

### E027 - Invalid vehicle `bearing`

Vehicle bearing must be between 0 and 360 degrees (inclusive).  The GTFS-rt spec says bearing is:

>...in degrees, clockwise from True North, i.e., 0 is North and 90 is East. This can be the compass bearing, or the direction towards the next stop or intermediate location. This should not be deduced from the sequence of previous positions, which clients can compute from previous data.

#### References:
* [`vehicle.position.bearing`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)

<a name="E028"/>

### E028 - Vehicle `position` outside agency coverage area

The vehicle `position` should be inside the agency coverage area.  Coverage area is defined by a buffer surrounding the GTFS `shapes.txt` data, or `stops.txt` locations if the GTFS feed doesn't include `shapes.txt`.

Buffer distance is defined by `GtfsMetadata.REGION_BUFFER_METERS`, and is currently 1609 meters (roughly 1 mile).

#### References:
* [`vehicle.position`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)

<a name="E029"/>

### E029 - Vehicle `position` far from trip shape

The vehicle `position` should be within a buffer surrounding the GTFS `shapes.txt` data for the current trip unless there is an `alert` with the `effect` of `DETOUR` for this `trip_id`.

Buffer distance is defined by `GtfsMetadata.TRIP_BUFFER_METERS`, and is currently 200 meters (roughly 1/8 of a mile).

#### References:
* [GTFS `shapes.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#shapestxt)
* [`vehicle.position`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)
* [`alert.effect.DETOUR`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-effect)

<a name="E030"/>

### E030 - GTFS-rt alert `trip_id` does not belong to GTFS-rt alert `route_id` in GTFS `trips.txt`

The GTFS-rt `alert.informed_entity.trip.trip_id` should belong to the specified GTFS-rt `alert.informed_entity.route_id` in GTFS `trips.txt`.

#### References:
* [`alert.informed_entity`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)
* [GTFS `trips.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#tripstxt)

<a name="E031"/>

### E031 - Alert `informed_entity.route_id` does not match `informed_entity.trip.route_id`

The `alert.informed_entity.trip.route_id` should be the same as the specified `alert.informed_entity.route_id`.

#### References:
* [`alert.informed_entity`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)
* [`alert.informed_entity.trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E032"/>

### E032 - Alert does not have an `informed_entity`

All alerts must have at least one `informed_entity`.

From [`alert.informed_entity`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector):

> The values of the fields should correspond to the appropriate fields in the GTFS feed. *At least one specifier must be given.* If several are given, then the matching has to apply to all the given specifiers.

#### References:
* [`alert.informed_entity`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)

<a name="E033"/>

### E033 - Alert `informed_entity` does not have any specifiers

Alert `informed_entity` should have at least one specified value (`route_id`, `trip_id`, `stop_id`, etc) to which the alert applies.

#### References:
* [`alert.informed_entity`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)

<a name="E034"/>

### E034 - GTFS-rt `agency_id` does not exist in GTFS data

All `agency_ids` provided in the GTFS-rt `alert.informed_entity.agency_id` should also exist in GTFS `agency.txt`.

#### References:
* [`alert.informed_entity`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-entityselector)

<a name="E035"/>

### E035 - GTFS-rt `trip.trip_id` does not belong to GTFS-rt `trip.route_id` in GTFS trips.txt

The GTFS-rt `trip.trip_id` should belong to the specified `trip.route_id` in GTFS `trips.txt`.

[`trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor) says:

>If route_id is also set, then it should be same as one that the given trip corresponds to.

#### References:
* [`trip.trip_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)
* [`trip.route_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E036"/>

### E036 - Sequential `stop_time_updates` have the same `stop_sequence`

Sequential GTFS-rt trip `stop_time_updates` should never have the same `stop_sequence` - `stop_sequence` must increase for each `stop_time_update`.

From [GTFS `stop_times.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#stop_timestxt):

>The values for stop_sequence must be non-negative integers, and they must increase along the trip.

*Common mistakes* - Repeated records in the GTFS `stop_times.txt` file   

*Possible solution* - Make sure that no `stop_sequence` is repeated in GTFS `stop_times.txt`.

#### References:
* [`trip.stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates)
* [GTFS `stop_times.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#stop_timestxt)

<a name="E037"/>

### E037 - Sequential `stop_time_updates` have the same `stop_id`

Sequential GTFS-rt trip `stop_time_updates` shouldn't have the same `stop_id` - sequential `stop_ids` should be different.  If a `stop_id` is visited more than once in a trip (i.e., a loop), and if no `stop_time_updates` in the loop are provided in the feed, and if the `stop_sequence` field of the stop where the loop starts/stops is provided in the GTFS-rt feed for the given `stop_id`, then this may not be an error.  

#### References:
* [`trip.stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates)

<a name="E038"/>

### E038 - Invalid `header.gtfs_realtime_version`

`header.gtfs_realtime_version` is required and must be a valid value.  Currently, the only valid values are `1.0` and `2.0`.

#### References:
* [`header.gtfs_realtime_version`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)

<a name="E039"/>

### E039 - `FULL_DATASET` feeds should not include `entity.is_deleted`

The `entity.is_deleted` field should only be included in GTFS-rt feeds with `header.incrementality` of `DIFFERENTIAL`.

#### References:
* [`header.incrementality`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)
* [`entity.is_deleted`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedentity)

<a name="E040"/>

### E040 - `stop_time_update` doesn't contain `stop_id` or `stop_sequence`

All `stop_time_updates` must contain `stop_id` or `stop_sequence` - both fields cannot be left blank.

From [`trip.stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate):

>The update is linked to a specific stop either through stop_sequence or stop_id, so one of these fields must necessarily be set. 

#### References:
* [`trip.stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)

<a name="E041"/>

### E041 - `trip` doesn't have any `stop_time_updates`

Unless a `trip's` `schedule_relationship` is `CANCELED`, a `trip` must have at least one `stop_time_update`

#### References:
* [`trip_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripupdate)
* [`trip_update.stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)
* [`trip_update.trip.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship-1)

<a name="E042"/>

### E042 - `arrival` or `departure` provided for `NO_DATA` `stop_time_update`

If a `stop_time_update` has a `schedule_relationship` of `NO_DATA`, then neither `arrival` nor `departure` should be provided.

From [`stop_time_update.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship):

> `NO_DATA` -> 	No data is given for this stop. It indicates that there is no realtime information available. When set NO_DATA is propagated through subsequent stops so this is the recommended way of specifying from which stop you do not have realtime information. *When NO_DATA is set neither arrival nor departure should be supplied*.

#### References:
* [`stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)
* [`stop_time_update.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship)

<a name="E043"/>

### E043 - `stop_time_update` doesn't have `arrival` or `departure`

If a `stop_time_update` doesn't have a `schedule_relationship` of `SKIPPED` or `NO_DATA`, then either `arrival` or `departure` must be provided.

From [`stop_time_update.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship):

> `SCHEDULED` -> The vehicle is proceeding in accordance with its static schedule of stops, although not necessarily according to the times of the schedule. This is the default behavior. *At least one of arrival and departure must be provided*. If the schedule for this stop contains both arrival and departure times then so must this update.

#### References:
* [`stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)
* [`stop_time_update.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship)

<a name="E044"/>

### E044 - `stop_time_update` `arrival/departure` doesn't have `delay` or `time`

If the `stop_time_update.schedule_relationship` is not `SKIPPED`, `stop_time_update.arrival` and `stop_time_update.departure` must have either `delay` or `time` - both fields cannot be missing.

[Stop Time Updates description](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates) says:

>The update can provide a exact timing for arrival and/or departure at a stop in StopTimeUpdates using StopTimeEvent. This should contain either an absolute time or a delay (i.e. an offset from the scheduled time in seconds).

[`stop_time_update.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship) says:

>`SKIPPED` - The stop is skipped, i.e., the vehicle will not stop at this stop. Arrival and departure are optional.

#### References:
* [Stop Time Updates description](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#stop-time-updates)
* [`stop_time_update` reference](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)
* [`stop_time_update.arrival and stop_time_update.departure (StopTimeEvent)`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeevent)
* [`stop_time_update.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship)

<a name="E045"/>

### E045 - GTFS-rt `stop_time_update` `stop_sequence` and `stop_id` do not match GTFS

If GTFS-rt stop_time_update contains both stop_sequence and stop_id, the values must match the GTFS data in stop_times.txt

#### References:
* [`stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)

<a name="E046"/>

### E046 - GTFS-rt `stop_time_update` without `time` doesn't have arrival/departure_time in GTFS

If only `delay` is provided in a `stop_time_update` `arrival` or `departure` (and not a `time`), then the GTFS `stop_times.txt` must contain arrival_times and/or departure_times for these corresponding stops.  A `delay` value in the real-time feed is meaningless unless you have a clock time to add it to in the GTFS `stop_times.txt` file.

*Common mistakes* - Providing a `arrival/departure.delay` value, but not providing a `arrival/departure.time` value for non-timepoint stops that do not have an `arrival_time` or `departure_time` in GTFS `stop_times.txt`.  

*Possible solution* - Add a `time` value to the GTFS-rt feed for the `arrival` and `departure`, or add an `arrival_time` and `departure_time` in `GTFS stop_times.txt`.

#### References:
* [`stop_time_update`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeupdate)
* [`stop_time_update.arrival and stop_time_update.departure (StopTimeEvent)`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-stoptimeevent)
* [GTFS `stop_times.txt`](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#stop_timestxt)

<a name="E047"/>

### E047 - `VehiclePosition` and `TripUpdate` ID pairing mismatch

If separate `VehiclePositions` and `TripUpdates` feeds are provided, `VehicleDescriptor` or `TripDescriptor` ID value pairing should match between the two feeds.  

In other words, if the `VehiclePosition` has a `vehicle_id` A that is assigned to `trip_id` 4, then the `TripUpdate` feed should have a prediction for `trip_id` 4 that includes a reference to `vehicle_id` A.  If the `trip_id` of 4 is paired with a different `vehicle_id` B in one of the two feeds, this is an error.

Note that this is different from W003, which simply checks to see if an ID that is provided in one feed is provided in the other - that is a warning.

#### References:
* [`vehicle.id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicledescriptor)
* [`trip.trip_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="E048"/>

### E048 - `header` `timestamp` not populated (GTFS-rt v2.0 and higher)

`timestamp` must be populated in `FeedHeader` for `gtfs_realtime_version` v2.0 and higher.

#### References:
* [`header.timestamp`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)

<a name="E049"/>

### E049 - `header` `incrementality` not populated (GTFS-rt v2.0 and higher)

`incrementality` must be populated in `FeedHeader` for `gtfs_realtime_version` v2.0 and higher.

#### References:
* [`header.incrementality`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-feedheader)

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

### W003 - ID in one feed missing from the other

If separate `VehiclePositions` and `TripUpdates` feeds are provided, a `trip_id` that is provided in the `VehiclePositions` feed should be provided in the `TripUpdates` feed, and a vehicle_id that is provided in the `TripUpdates` feed should be provided in the `VehiclePositions` feed.

In other words, if the `VehiclePosition` has a vehicle that is assigned to `trip_id` 4, then the `TripUpdate` feed should have a prediction for `trip_id` 4.

Note that this is different from E047, which checks for a mismatch of IDs between the feeds - that is an error.

#### References:
* [`vehicle.id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-vehicledescriptor)
* [`trip.trip_id`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-tripdescriptor)

<a name="W004"/>

### W004 - vehicle `speed` is unrealistic

`vehicle.position.speed` has an unrealistic speed that may be incorrect.  

Speeds are flagged as unrealistic if they are greater than `VehicleValidator.MAX_REALISTIC_SPEED_METERS_PER_SECOND`, which is currently set to 26 meters per second (approx. 60 miles per hour). 

*Common mistakes* - Accidentally setting the speed value in *miles per hour*, instead of *meters per second*. 

*Possible solution* - Check to make sure the speed units are *meters per second*.

#### References:
* [`vehicle.position.speed`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#message-position)

<a name="W005"/>

### W005 - Missing `vehicle_id` in `trip_update` for frequency-based exact_times = 0

Frequency-based exact_times = 0 trip_updates should contain `vehicle_id`.  This helps disambiguate predictions in situations where more than one vehicle is running the same trip instance simultaneously.

#### References:
* [`trip_update.trip`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/trip-updates.md#alternative-trip-matching)
* [GTFS `frequencies.txt` `exact_times` = 0](https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#frequenciestxt)

<a name="W006"/>

### W006 - `trip` missing `trip_id`

`trips` should include a `trip_id`.  A missing `trip_id` is usually an error in the feed (especially for frequency-based `exact_times` = 0 trips - see [E006](https://github.com/CUTR-at-USF/gtfs-realtime-validator/blob/master/RULES.md#E006)), although the section on "Alternative trip matching" includes one exception:

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

<a name="W009"/>

### W009 - `schedule_relationship` not populated

`trip.schedule_relationship` and `stop_time_update.schedule_relationship` should be populated.

#### References:
* [`trip.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship-1)
* [`stop_time_update.schedule_relationship`](https://github.com/google/transit/blob/master/gtfs-realtime/spec/en/reference.md#enum-schedulerelationship)
