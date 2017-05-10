/*
 * Copyright (C) 2017 University of South Florida
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

package edu.usf.cutr.gtfsrtvalidator.background;

import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.*;
import org.slf4j.LoggerFactory;

import java.util.*;

import static edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils.logDuration;

/**
 * This is a container class for metadata about a GTFS feed that's used in rule validation
 */
public class GtfsMetadata {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(GtfsMetadata.class);

    String mFeedUrl;
    TimeZone mTimeZone;

    private Set<String> mRouteIds = new HashSet<>();
    // Maps trip_ids to the GTFS trip
    private Map<String, Trip> mTrips = new HashMap<>();
    // Maps trip_ids to a list of StopTimes
    private Map<String, List<StopTime>> mTripStopTimes = new HashMap<>();
    private Set<String> mStopIds = new HashSet<>();
    private Set<String> mExactTimesZeroTripIds = new HashSet<>();
    // Maps trip_id to a list of Frequency objects
    private Map<String, List<Frequency>> mExactTimesOneTrips = new HashMap<>();

    /**
     * key is stops.txt stop_id, value is stops.txt location_type
     * <p>
     * Note that we can't consolidate this with the mStopIds HashSet, because location_type is an optional
     * field in stops.txt and therefore can be null.
     */
    private Map<String, Integer> mStopToLocationTypeMap = new HashMap<>();

    /**
     * Builds the metadata for a particular GTFS feed
     *
     * @param feedUrl URL for the GTFS zip file
     * @param timeZone the agency_timezone from GTFS agency.txt, or null if the current time zone should be used.
     * @param gtfsData GTFS feed to build the metadata for
     */
    public GtfsMetadata(String feedUrl, TimeZone timeZone, GtfsDaoImpl gtfsData) {
        long startTime = System.nanoTime();

        mFeedUrl = feedUrl;
        mTimeZone = timeZone;

        // Get all route_ids from the GTFS feed
        Collection<Route> gtfsRouteList = gtfsData.getAllRoutes();
        for (Route r : gtfsRouteList) {
            mRouteIds.add(r.getId().getId());
        }

        // Get all StopTimes and map them to trip_ids
        for (StopTime stopTime : gtfsData.getAllStopTimes()) {
            String tripId = stopTime.getTrip().getId().getId();

            // If there isn't already a list for this trip, create one
            List<StopTime> stopTimes = mTripStopTimes.computeIfAbsent(tripId, k -> new ArrayList<>());
            stopTimes.add(stopTime);
        }

        // Get all trip_ids from the GTFS feed
        Collection<Trip> gtfsTripList = gtfsData.getAllTrips();
        for (Trip trip : gtfsTripList) {
            String tripId = trip.getId().getId();
            mTrips.put(tripId, trip);

            List<StopTime> stopTimes = mTripStopTimes.get(tripId);
            if (stopTimes != null) {
                // Make sure StopTimes are sorted by stop_sequence for this trip (stop_times.txt isn't necessary sorted)
                stopTimes.sort(Comparator.comparing(stopTime -> (stopTime.getStopSequence())));
            }
        }

        // Create a set of stop_ids from the GTFS feeds stops.txt, and store their location_type in a map
        Collection<Stop> stops = gtfsData.getAllStops();
        for (Stop stop : stops) {
            mStopIds.add(stop.getId().getId());
            mStopToLocationTypeMap.put(stop.getId().getId(), stop.getLocationType());
        }

        // Create a set of all exact_times=0 trips
        Collection<Frequency> frequencies = gtfsData.getAllFrequencies();
        for (Frequency f : frequencies) {
            if (f.getExactTimes() == 0) {
                mExactTimesZeroTripIds.add(f.getTrip().getId().getId());
            } else if (f.getExactTimes() == 1) {
                List<Frequency> frequencyList = mExactTimesOneTrips.get(f.getTrip().getId().getId());
                if (frequencyList == null) {
                    frequencyList = new ArrayList<>();
                }
                frequencyList.add(f);
                mExactTimesOneTrips.put(f.getTrip().getId().getId(), frequencyList);
            }
        }

        logDuration(_log, "Built GtfsMetadata for " + feedUrl + " in ", startTime);
    }

    public Set<String> getRouteIds() {
        return mRouteIds;
    }

    /**
     * Returns a map where key is trips.txt trip_id, and value is an object representing the GTFS trip
     *
     * @return a map where key is trips.txt trip_id, and value is an object representing the GTFS trip
     */
    public Map<String, Trip> getTrips() {
        return mTrips;
    }

    public Set<String> getStopIds() {
        return mStopIds;
    }

    /**
     * Returns a map where key is stops.txt stop_id, value is stops.txt location_type
     *
     * @return a map where key is stops.txt stop_id, value is stops.txt location_type
     */
    public Map<String, Integer> getStopToLocationTypeMap() {
        return mStopToLocationTypeMap;
    }

    public Set<String> getExactTimesZeroTripIds() {
        return mExactTimesZeroTripIds;
    }

    /**
     * Returns a map where key is trips.txt trip_id, value is a list of Frequency objects representing frequencies.txt data for that trip_id
     *
     * @return a map where key is trips.txt trip_id, value is a list of Frequency objects representing frequencies.txt data for that trip_id
     */
    public Map<String, List<Frequency>> getExactTimesOneTrips() {
        return mExactTimesOneTrips;
    }

    /**
     * Returns a map where key is trips.txt trip_id, and the value is a list of StopTime objects from stop_times.txt sorted by stop_sequence
     *
     * @return a map where key is trips.txt trip_id, and the value is a list of StopTime objects from stop_times.txt sorted by stop_sequence
     */
    public Map<String, List<StopTime>> getTripStopTimes() {
        return mTripStopTimes;
    }

    /**
     * Returns the agency_timezone from GTFS agency.txt, or null if the current time zone should be used.  Please refer to http://en.wikipedia.org/wiki/List_of_tz_zones for a list of valid values.
     *
     * @return the agency_timezone from GTFS agency.txt, or null if the current time zone should be used.  Please refer to http://en.wikipedia.org/wiki/List_of_tz_zones for a list of valid values.
     */
    public TimeZone getTimeZone() {
        return mTimeZone;
    }
}
