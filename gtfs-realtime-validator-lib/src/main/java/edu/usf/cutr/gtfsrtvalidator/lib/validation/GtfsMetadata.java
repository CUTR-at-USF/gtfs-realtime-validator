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

package edu.usf.cutr.gtfsrtvalidator.lib.validation;

import edu.usf.cutr.gtfsrtvalidator.lib.util.TimestampUtils;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.ShapeFactory;
import org.onebusaway.gtfs.model.*;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * This is a container class for metadata about a GTFS feed that's used in rule validation
 */
public class GtfsMetadata {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(GtfsMetadata.class);

    // Spatial operation buffer values
    public static final double REGION_BUFFER_METERS = 1609; // Roughly 1 mile
    public static final double TRIP_BUFFER_METERS = 200; // Roughly 1/8 of a mile
    public static final double TRIP_BUFFER_DEGREES = DistanceUtils.KM_TO_DEG * (TRIP_BUFFER_METERS / 1000.0d);

    String mFeedUrl;
    TimeZone mTimeZone;

    private Set<String> mAgencyIds = new HashSet<>();
    private Set<String> mRouteIds = new HashSet<>();
    // Maps trip_ids to the GTFS trip
    private Map<String, Trip> mTrips = new HashMap<>();
    // Maps trip_ids to a list of StopTimes
    private Map<String, List<StopTime>> mTripStopTimes = new HashMap<>();
    private Set<String> mStopIds = new HashSet<>();
    private Set<String> mExactTimesZeroTripIds = new HashSet<>();
    // Maps trip_id to a list of Frequency objects
    private Map<String, List<Frequency>> mExactTimesOneTrips = new HashMap<>();
    // Maps shape_id to a list of ShapePoints
    private Map<String, List<ShapePoint>> mShapePoints = new HashMap<>();
    // Map trip_id to a polyline of the trip shape from shapes.txt
    private Map<String, Shape> mTripShapes = new HashMap<>();
    // Map trip_id to a buffered polyline of the trip shape from shapes.txt
    private Map<String, Shape> mTripShapesBuffered = new ConcurrentHashMap<>();

    // A geographic bounding box that includes all the stops from GTFS stops.txt
    private Rectangle mStopBoundingBox;

    // A geographic bounding box that that includes all stops from GTFS stops.txt PLUS a buffer
    private Rectangle mStopBoundingBoxWithBuffer;

    // A geographic bounding box that includes all the points from GTFS shapes.txt, if the GTFS feed includes shapes.txt
    private Rectangle mShapeBoundingBox = null;

    // A geographic bounding box that includes all the points from GTFS shapes.txt, if the GTFS feed includes shapes.txt, PLUS a buffer
    private Rectangle mShapeBoundingBoxWithBuffer = null;

    /**
     * key is stops.txt stop_id, value is stops.txt location_type
     * <p>
     * Note that we can't consolidate this with the mStopIds HashSet, because location_type is an optional
     * field in stops.txt and therefore can be null.
     */
    private Map<String, Integer> mStopToLocationTypeMap = new HashMap<>();

    // A map of trips that visit a stop more than once, where the key is the trip_id and the value is a list of the stops visited more than once
    private Map<String, List<String>> mTripsWithMultiStops = new HashMap<>();

    /**
     * Builds the metadata for a particular GTFS feed
     *
     * @param feedUrl URL for the GTFS zip file
     * @param timeZone the agency_timezone from GTFS agency.txt, or null if the current time zone should be used.
     * @param gtfsData GTFS feed to build the metadata for
     * @param ignoreShapes true if the GTFS shapes.txt should be ignored when generating metadata, or false if the shapes.txt metadata should be generated.  If
     *                      you are getting OutOfMemoryErrors you can try setting this to true.  Note that if true
     *                      certain spatial rules such as E029 will not be executed.
     */
    public GtfsMetadata(String feedUrl, TimeZone timeZone, GtfsMutableDao gtfsData, boolean ignoreShapes) {
        long startTime = System.nanoTime();
        _log.info("Building GtfsMetadata for " + feedUrl + "...");

        mFeedUrl = feedUrl;
        mTimeZone = timeZone;

        // Get all agency_ids from the GTFS feed
        Collection<Agency> agencyAndIds = gtfsData.getAllAgencies();
        for (Agency a : agencyAndIds) {
            mAgencyIds.add(a.getId());
        }

        // Get all route_ids from the GTFS feed
        Collection<Route> gtfsRouteList = gtfsData.getAllRoutes();
        for (Route r : gtfsRouteList) {
            mRouteIds.add(r.getId().getId());
        }

        /**
         * Process GTFS shapes.txt
         */
        double regionBufferDegrees = DistanceUtils.KM_TO_DEG * (REGION_BUFFER_METERS / 1000.0d);

        ShapeFactory sf = JtsSpatialContext.GEO.getShapeFactory();
        ShapeFactory.MultiPointBuilder shapeBuilder = sf.multiPoint();
        Collection<ShapePoint> shapePoints = gtfsData.getAllShapePoints();
        if (shapePoints != null && !ignoreShapes && shapePoints.size() > 3) {
            for (ShapePoint p : shapePoints) {
                String shapeId = p.getShapeId().getId();
                // If there isn't already a list for this shape_id, create one
                List<ShapePoint> shapePointList = mShapePoints.computeIfAbsent(shapeId, k -> new ArrayList<>());
                shapePointList.add(p);
                // Create GTFS shapes.txt bounding box
                shapeBuilder.pointXY(p.getLon(), p.getLat());
            }
            _log.debug("Loaded shapes.txt points for " + feedUrl);

            Shape shapePointShape = shapeBuilder.build();
            mShapeBoundingBox = shapePointShape.getBoundingBox();
            mShapeBoundingBoxWithBuffer = mShapeBoundingBox.getBuffered(regionBufferDegrees, mShapeBoundingBox.getContext()).getBoundingBox();
            _log.debug("Generated shapes.txt bounding boxes for " + feedUrl);

            // Order shape points by GTFS shapes.txt shape_pt_sequence
            _log.debug("Sorting shape points for " + feedUrl + "...");
            Collection<List<ShapePoint>> shapePointLists = mShapePoints.values();
            for (List<ShapePoint> shapePointList : shapePointLists) {
                shapePointList.sort(Comparator.comparing(shapePoint -> (shapePoint.getSequence())));
            }
            _log.debug("Shape points for " + feedUrl + " are sorted.");
        }

        // Get all StopTimes and map them to trip_ids
        for (StopTime stopTime : gtfsData.getAllStopTimes()) {
            String tripId = stopTime.getTrip().getId().getId();

            // If there isn't already a list for this trip, create one
            List<StopTime> stopTimes = mTripStopTimes.computeIfAbsent(tripId, k -> new ArrayList<>());
            stopTimes.add(stopTime);
        }

        /**
         * Process GTFS trips.txt - this is a long-running operation for feeds with huge shapes.txt, so log to INFO
         */
        _log.info("Processing trips and building trip shapes for " + feedUrl + "...");
        long tripStartTime = System.nanoTime();
        Collection<Trip> gtfsTripList = gtfsData.getAllTrips();
        for (Trip trip : gtfsTripList) {
            String tripId = trip.getId().getId();
            mTrips.put(tripId, trip);

            List<StopTime> stopTimes = mTripStopTimes.get(tripId);
            if (stopTimes != null) {
                // Make sure StopTimes are sorted by stop_sequence for this trip (stop_times.txt isn't necessary sorted)
                stopTimes.sort(Comparator.comparing(stopTime -> (stopTime.getStopSequence())));
            }

            // Create a polyline for each trip if the GTFS shapes.txt data exists
            AgencyAndId shapeAgencyAndId = trip.getShapeId();
            if (shapeAgencyAndId != null && !isEmpty(shapeAgencyAndId.getId())) {
                List<ShapePoint> tripShape = mShapePoints.get(shapeAgencyAndId.getId());
                if (tripShape != null) {
                    ShapeFactory.LineStringBuilder lineBuilder = sf.lineString();
                    for (ShapePoint p : tripShape) {
                        lineBuilder.pointXY(p.getLon(), p.getLat());
                    }
                    mTripShapes.put(tripId, lineBuilder.build());
                }
            }
        }
        TimestampUtils.logDuration(_log, "Trips polylines processed for " + feedUrl + " in ", tripStartTime);

        /**
         * Process GTFS stop_times.txt
         */
        long stopTimesStartTime = System.nanoTime();
        for (Map.Entry<String, List<StopTime>> tripStopTimes : mTripStopTimes.entrySet()) {
            // Create the map of trip_ids to List of stop_ids for trips that visit a stop more than once
            String tripId = tripStopTimes.getKey();
            Set<String> allStopIds = new HashSet<>();
            List<String> duplicateStopIds = new ArrayList<>();

            for (StopTime stopTime : tripStopTimes.getValue()) {
                if (allStopIds.contains(stopTime.getStop().getId().getId())) {
                    // If we've already seen this stop_id for this trip, then add it to the duplicates list
                    duplicateStopIds.add(stopTime.getStop().getId().getId());
                }
                allStopIds.add(stopTime.getStop().getId().getId());
            }

            if (!duplicateStopIds.isEmpty()) {
                mTripsWithMultiStops.put(tripId, duplicateStopIds);
            }
        }
        TimestampUtils.logDuration(_log, "Repeated stop_ids for trips in stop_times.txt processed for " + feedUrl + " in ", stopTimesStartTime);

        /**
         * Process GTFS stops.txt
         */
        ShapeFactory.MultiPointBuilder stopBuilder = sf.multiPoint();
        Collection<Stop> stops = gtfsData.getAllStops();
        for (Stop stop : stops) {
            // Create a set of stop_ids from the GTFS feeds stops.txt, and store their location_type in a map
            mStopIds.add(stop.getId().getId());
            mStopToLocationTypeMap.put(stop.getId().getId(), stop.getLocationType());
            // Create GTFS stops.txt bounding box
            if (stop.isLonSet() && stop.isLatSet()) {
                stopBuilder.pointXY(stop.getLon(), stop.getLat());
            }
        }

        Shape stopShape = stopBuilder.build();
        mStopBoundingBox = stopShape.getBoundingBox();
        mStopBoundingBoxWithBuffer = mStopBoundingBox.getBuffered(regionBufferDegrees, mStopBoundingBox.getContext()).getBoundingBox();

        /**
         * Process GTFS frequencies.txt
         */
        Collection<Frequency> frequencies = gtfsData.getAllFrequencies();
        for (Frequency f : frequencies) {
            if (f.getExactTimes() == 0) {
                // All exact_times=0 trips
                mExactTimesZeroTripIds.add(f.getTrip().getId().getId());
            } else if (f.getExactTimes() == 1) {
                // All exact_times=1 trips
                List<Frequency> frequencyList = mExactTimesOneTrips.get(f.getTrip().getId().getId());
                if (frequencyList == null) {
                    frequencyList = new ArrayList<>();
                }
                frequencyList.add(f);
                mExactTimesOneTrips.put(f.getTrip().getId().getId(), frequencyList);
            }
        }

        TimestampUtils.logDuration(_log, "Built GtfsMetadata for " + feedUrl + " in ", startTime);
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

    /**
     * Returns a geographic bounding box for the stop locations from GTFS stops.txt
     *
     * @return a geographic bounding box for the stop locations from GTFS stops.txt
     */
    public Rectangle getStopBoundingBox() {
        return mStopBoundingBox;
    }

    /**
     * Returns a geographic bounding box for the stop locations from GTFS stops.txt with an added buffer REGION_BUFFER_METERS
     *
     * @return a geographic bounding box for the stop locations from GTFS stops.txt with an added buffer REGION_BUFFER_METERS
     */
    public Rectangle getStopBoundingBoxWithBuffer() {
        return mStopBoundingBoxWithBuffer;
    }

    /**
     * Returns a geographic bounding box for the shape locations from GTFS shapes.txt, if the GTFS feed includes shapes.txt, or null if it does not
     *
     * @return a geographic bounding box for the shape locations from GTFS shapes.txt, if the GTFS feed includes shapes.txt, or null if it does not
     */
    public Rectangle getShapeBoundingBox() {
        return mShapeBoundingBox;
    }

    /**
     * Returns a geographic bounding box for the shape locations from GTFS shapes.txt, if the GTFS feed includes shapes.txt, with an added buffer REGION_BUFFER_METERS, or null if it does not
     *
     * @return a geographic bounding box for the shape locations from GTFS shapes.txt, if the GTFS feed includes shapes.txt, with an added buffer REGION_BUFFER_METERS, or null if it does not
     */
    public Rectangle getShapeBoundingBoxWithBuffer() {
        return mShapeBoundingBoxWithBuffer;
    }

    /**
     * Returns a map of GTFS shape_id to a list of ShapePoints from GTFS shapes.txt
     *
     * @return a map of GTFS shape_id to a list of ShapePoints from GTFS shapes.txt
     */
    public Map<String, List<ShapePoint>> getShapePoints() {
        return mShapePoints;
    }

    /**
     * Returns a map of GTFS trip_ids to a polyline of that trip's shape from shapes.txt
     *
     * @return a map of GTFS trip_ids to a polyline of that trip's shape from shapes.txt
     */
    public Map<String, Shape> getTripShapes() {
        return mTripShapes;
    }

    /**
     * Returns a buffered representation (TRIP_BUFFER_METERS) of a GTFS trip shape from shapes.txt for the given tripId,
     * or null if a shape doesn't exist for the given tripId.
     * <p>
     *
     * @param tripId the GTFS trip_id to retrieve a buffered trip shape for
     * @return a buffered representation (TRIP_BUFFER_METERS) of a GTFS trip shape from shapes.txt for the given tripId,
     * or null if a shape doesn't exist for the given trip.
     */
    public Shape getBufferedTripShape(String tripId) {
        if (mTripShapes == null) {
            // No shapes at all
            return null;
        }
        Shape s = mTripShapes.get(tripId);
        if (s == null) {
            // No shape for this trip_id
            return null;
        }
        // Create the buffered version of the trip shape if it doesn't yet exist
        return mTripShapesBuffered.computeIfAbsent(tripId, k -> s.getBuffered(TRIP_BUFFER_DEGREES, s.getContext()));
    }

    /**
     * Returns a set of agency_ids from GTFS agency.txt
     *
     * @return a set of agency_ids from GTFS agency.txt
     */
    public Set<String> getAgencyIds() {
        return mAgencyIds;
    }

    /**
     * Returns a map of trips that visit a stop more than once, where the key is the trip_id and the value is a list of the stops visited more than once
     *
     * @return a map of trips that visit a stop more than once, where the key is the trip_id and the value is a list of the stops visited more than once
     */
    public Map<String, List<String>> getTripsWithMultiStops() {
        return mTripsWithMultiStops;
    }
}
