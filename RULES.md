# Implemented rules

### Table of Warnings

| Warning ID    | Warning Title             |
|---------------|---------------------------|
| [W001](#W001) | `timestamps` not populated
| [W002](#W002) | `vehicle_id` not populated
| [W003](#W003) | `VehiclePosition` and `TripUpdate` feed mismatch


### Table of Errors

| Error ID      | Error Title         |
|---------------|---------------------------|
| [E001](#E001) | Not in POSIX time
| [E002](#E002) | Unsorted `stop_sequence`
| [E003](#E003) | `trip_id` mismatch in GTFS-rt and GTFS
| [E010](#E010) | `location_type` not `0` in `stops.txt`
| [E011](#E011) | `location_type` not `0` in GTFS-rt

# Warnings

<a name="W001"/>

### W001 - `timestamp` not populated

`timestamps` should be populated for `FeedHeader`, `TripUpdates`, `VehiclePositions`, and `Alerts`

<a name="W002"/>

### W002 - `vehicle_id` not populated

`vehicle_id` should be populated in trip_update

<a name="W003"/>

### W003 - `VehiclePosition` and `TripUpdate` feed mismatch

If both vehicle positions and trip updates are provided, `VehicleDescriptor` or `TripDescriptor` values should match between the two feeds

# Errors

<a name="E001"/>

### E001 - Not in POSIX time

All timestamps must be in POSIX time (i.e., number of seconds since January 1st 1970 00:00:00 UTC)

<a name="E002"/>

### E002 - Unsorted `stop_sequence`

`stop_time_updates` for a given `trip_id` must be sorted by increasing stop_sequence (this should always be enforced whether or not the feed contains the stop_sequence field). A TripUpdate can have multiple stop_time_updates (e.g., one prediction per stop) - so, this shouldn't be monitored across multiple feed messages, just in a single message.

<a name="E003"/>

### E003 - `trip_id` mismatch in GTFS-rt and GTFS

All `trip_ids` provided in the GTFS-rt feed must appear in the GTFS data

<a name="E010"/>

### E010 - `location_type` not `0` in stops.txt

If location_type is used in `stops.txt`, all stops referenced in `stop_times.txt` must have `location_type` of `0`

<a name="E011"/>

### E011 - `location_type` not `0` in GTFS-rt

All `stop_ids` referenced in GTFS-rt feeds must have the `location_type` = `0`