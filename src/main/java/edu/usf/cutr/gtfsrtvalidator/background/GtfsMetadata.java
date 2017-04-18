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
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a container class for metadata about a GTFS feed that's used in rule validation
 */
public class GtfsMetadata {

    private Set<String> routeIds = new HashSet<>();
    private Set<String> tripIds = new HashSet<>();
    private Set<String> stopIds = new HashSet<>();
    private Set<String> exactTimesZeroTripIds = new HashSet<>();

    /**
     * Builds the metadata for a particular GTFS feed
     *
     * @param gtfsData GTFS feed to build the metadata for
     */
    public GtfsMetadata(GtfsDaoImpl gtfsData) {
        // Get all route_ids from the GTFS feed
        Collection<Route> gtfsRouteList = gtfsData.getAllRoutes();
        for (Route r : gtfsRouteList) {
            routeIds.add(r.getId().getId());
        }

        // Get all trip_ids from the GTFS feed
        Collection<Trip> gtfsTripList = gtfsData.getAllTrips();
        for (Trip trip : gtfsTripList) {
            tripIds.add(trip.getId().getId());
        }

        // Create a set of stop_ids from the GTFS feeds stops.txt
        Collection<Stop> stops = gtfsData.getAllStops();
        for (Stop stop : stops) {
            stopIds.add(stop.getId().getId());
        }

        // Create a set of all exact_times=0 trips
        Collection<Frequency> frequencies = gtfsData.getAllFrequencies();
        for (Frequency f : frequencies) {
            if (f.getExactTimes() == 0) {
                exactTimesZeroTripIds.add(f.getTrip().getId().getAgencyId());
            }
        }
    }

    public Set<String> getRouteIds() {
        return routeIds;
    }

    public Set<String> getTripIds() {
        return tripIds;
    }

    public Set<String> getStopIds() {
        return stopIds;
    }

    public Set<String> getExactTimesZeroTripIds() {
        return exactTimesZeroTripIds;
    }
}
