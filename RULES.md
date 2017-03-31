

### Table of Warnings

| Warning ID  | Warning Title             |
|-------------|---------------------------|
| [W001]      | Timestamps should be populated for all elements
| [W002]      | Vehicle_id not populated
| [W003]      | VehiclePosition and TripUpdate feed mismatch


### Table of Errors

| Error ID    | Error Description         |
|-------------|---------------------------|
| E001        | All timestamps must be in POSIX time (i.e., number of seconds since January 1st 1970 00:00:00 UTC)|
| E002        | stop_time_updates for a given trip_id must be sorted by increasing stop_sequence (this should always be enforced whether or not the feed contains the stop_sequence field). A TripUpdate can have multiple stop_time_updates (e.g., one prediction per stop) - so, this shouldn't be monitored across multiple feed messages, just in a single message.|
| E003        | All trip_ids provided in the GTFS-rt feed must appear in the GTFS data|
| E010        | If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0
| E011        | All stop_ids referenced in GTFS-rt feeds must have the location_type = 0

<a name="W001"/>

### W001 - Timestamp not populated

`timestamps` should be populated for `FeedHeader`, `TripUpdates`, `VehiclePositions`, and `Alerts`

<a name="W002"/>

### W002 - Vehicle_id not populated

`vehicle_id` should be populated in trip_update

<a name="W003"/>

### W003 - VehiclePosition and TripUpdate feed mismatch

If both vehicle positions and trip updates are provided, `VehicleDescriptor` or `TripDescriptor` values should match between the two feeds