/*
 * Copyright (C) 2017 University of South Florida.
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
package edu.usf.cutr.gtfsrtvalidator.lib.util;

import com.google.transit.realtime.GtfsRealtime;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.ShapeFactory;
import org.locationtech.spatial4j.shape.SpatialRelation;

import static org.locationtech.spatial4j.context.SpatialContext.GEO;

/**
 * Utilities for working with GTFS and GTFS-realtime objects
 */
public class GtfsUtils {

    public static final String GTFS_RT_V1 = "1.0";
    public static final String GTFS_RT_V2 = "2.0";

    /**
     * Returns true if the version for the provided GTFS-rt header is valid, false if it is not
     *
     * @param feedHeader the feed header to check the version for
     * @return true if the version for the provided GTFS-rt header is valid, false if it is not
     */
    public static boolean isValidVersion(GtfsRealtime.FeedHeader feedHeader) {
        return !feedHeader.hasGtfsRealtimeVersion() || feedHeader.getGtfsRealtimeVersion().equals(GTFS_RT_V1) || feedHeader.getGtfsRealtimeVersion().equals(GTFS_RT_V2);
    }

    /**
     * Returns true if the version for the provided GTFS-rt header is v2 or higher, false if the version is v1 or unrecognized
     *
     * @param feedHeader the feed header to check the version for
     * @return true if the version for the provided GTFS-rt header is v2 or higher, false if the version is v1 or unrecognized
     */
    public static boolean isV2orHigher(GtfsRealtime.FeedHeader feedHeader) {
        float version = Float.parseFloat(feedHeader.getGtfsRealtimeVersion());
        if (version >= 2.0f) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if this tripDescriptor has a schedule_relationship of ADDED, false if it does not
     *
     * @param tripDescriptor TripDescriptor to examine
     * @return true if this tripDescriptor has a schedule_relationship of ADDED, false if it does not
     */
    public static boolean isAddedTrip(GtfsRealtime.TripDescriptor tripDescriptor) {
        return tripDescriptor.hasScheduleRelationship() && tripDescriptor.getScheduleRelationship() == GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED;
    }

    /**
     * Converts the provided speed in metersPerSecond to miles per hour
     *
     * @param metersPerSecond
     * @return the provided speed in metersPerSecond converted to miles per hour
     */
    public static float toMilesPerHour(float metersPerSecond) {
        return metersPerSecond * 2.23694f;
    }

    /**
     * Converts the provided distance in meters to miles
     *
     * @param meters
     * @return the provided distance in meters converted to miles
     */
    public static double toMiles(double meters) {
        return meters * 0.000621371d;
    }

    /**
     * Returns vehicle and trip IDs text (vehicle_id X trip_id = Y) for the given entity if the entity is a VehiclePosition, or the trip ID text (trip_id = Y) for the given entity if the entity is a TripUpdate
     *
     * @param entity Either the VehiclePosition or TripUpdate for which to generate the ID text
     * @return vehicle and trip IDs text (vehicle_id X trip_id = Y) for the given entity if the entity is a VehiclePosition, or the trip ID text (trip_id = Y) for the given entity if the entity is a TripUpdate
     */
    public static String getVehicleAndTripIdText(Object entity) {
        if (!(entity instanceof GtfsRealtime.VehiclePosition || entity instanceof GtfsRealtime.TripUpdate)) {
            throw new IllegalArgumentException("entity must be instance of VehiclePosition or TripUpdate");
        }
        String ids = null;
        if (entity instanceof GtfsRealtime.VehiclePosition) {
            GtfsRealtime.VehiclePosition vp = (GtfsRealtime.VehiclePosition) entity;
            ids = "vehicle_id " + vp.getVehicle().getId() + " trip_id " + vp.getTrip().getTripId();
        } else if (entity instanceof GtfsRealtime.TripUpdate) {
            GtfsRealtime.TripUpdate tu = (GtfsRealtime.TripUpdate) entity;
            ids = "trip_id " + tu.getTrip().getTripId();
        }
        return ids;
    }

    /**
     * Returns vehicle and route IDs text (vehicle_id X route_id = Y) for the given entity if the entity is a VehiclePosition, or the route ID text (route_id = Y) for the given entity if the entity is a TripUpdate
     *
     * @param entity Either the VehiclePosition or TripUpdate for which to generate the ID text
     * @return vehicle and route IDs text (vehicle_id X route_id = Y) for the given entity if the entity is a VehiclePosition, or the route ID text (route_id = Y) for the given entity if the entity is a TripUpdate
     */
    public static String getVehicleAndRouteId(Object entity) {
        if (!(entity instanceof GtfsRealtime.VehiclePosition || entity instanceof GtfsRealtime.TripUpdate)) {
            throw new IllegalArgumentException("entity must be instance of VehiclePosition or TripUpdate");
        }
        String ids = null;
        if (entity instanceof GtfsRealtime.VehiclePosition) {
            GtfsRealtime.VehiclePosition vp = (GtfsRealtime.VehiclePosition) entity;
            ids = "vehicle_id " + vp.getVehicle().getId() + " route_id " + vp.getTrip().getRouteId();
        } else if (entity instanceof GtfsRealtime.TripUpdate) {
            GtfsRealtime.TripUpdate tu = (GtfsRealtime.TripUpdate) entity;
            ids = "route_id " + tu.getTrip().getRouteId();
        }
        return ids;
    }

    /**
     * Returns true if this position has valid latitude and longitude values, and false if it does not
     *
     * @param position Vehicle position to validate
     * @return true if this position has valid latitude and longitude values, and false if it does not
     */
    public static boolean isPositionValid(GtfsRealtime.Position position) {
        if (position.getLatitude() < -90f || position.getLatitude() > 90f) {
            return false;
        }
        if (position.getLongitude() < -180f || position.getLongitude() > 180f) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if this position has valid bearing, and false if it does not
     *
     * @param position Vehicle position to validate
     * @return true if this position has valid bearing, and false if it does not
     */
    public static boolean isBearingValid(GtfsRealtime.Position position) {
        if (!position.hasBearing()) {
            return true;
        }
        if (position.getBearing() < 0 || position.getBearing() > 360) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if the provided vehiclePosition is within the provided shape, false if it is not
     *
     * @param vehiclePosition the vehiclePosition to test against the shape
     * @param bounds          the shape to test against the vehiclePosition
     * @return true if the provided vehiclePosition is within the provided shape, false if it is not
     */
    public static boolean isPositionWithinShape(GtfsRealtime.Position vehiclePosition, Shape bounds) {
        ShapeFactory sf = GEO.getShapeFactory();
        org.locationtech.spatial4j.shape.Point p = sf.pointXY(vehiclePosition.getLongitude(), vehiclePosition.getLatitude());
        return bounds.relate(p).equals(SpatialRelation.CONTAINS);
    }

    /**
     * Returns the trip_id for the given TripUpdate if one exists, if not the entity ID is returned in the format
     * "trip_id 1234" or "entity ID 4321".
     *
     * @param entity     the entity that the TripUpdate belongs to
     * @param tripUpdate the tripUpdate to get the ID for
     * @return the trip_id for the given TripUpdate if one exists, if not the entity ID is returned in the format "trip_id 1234" or "entity ID 4321".
     */
    public static String getTripId(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripUpdate tripUpdate) {
        if (!tripUpdate.hasTrip()) {
            return "entity ID " + entity.getId();
        }
        return getTripId(entity, tripUpdate.getTrip());
    }

    /**
     * Returns the trip_id for the given TripDescriptor if one exists, if not the entity ID is returned in the format
     * "trip_id 1234" or "entity ID 4321".
     *
     * @param entity     the entity that the TripUpdate belongs to
     * @param tripDescriptor the tripDescriptor to get the ID for
     * @return the trip_id for the given TripUpdate if one exists, if not the entity ID is returned in the format "trip_id 1234" or "entity ID 4321".
     */
    public static String getTripId(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripDescriptor tripDescriptor) {
        return tripDescriptor.hasTripId() ? "trip_id " + tripDescriptor.getTripId() : "entity ID " + entity.getId();
    }

    /**
     * Returns the vehicle id for the given VehiclePosition if one exists, if not the entity ID is returned in the format
     * "vehicle.id 1234" or "entity ID 4321".
     *
     * @param entity          the entity that the VehiclePosition belongs to
     * @param vehiclePosition the VehiclePosition to get the ID for
     * @return the vehicle.id for the given VehiclePosition if one exists, if not the entity ID is returned in the format "vehicle.id 1234" or "entity ID 4321".
     */
    public static String getVehicleId(GtfsRealtime.FeedEntity entity, GtfsRealtime.VehiclePosition vehiclePosition) {
        if (!vehiclePosition.hasVehicle()) {
            return "entity ID " + entity.getId();
        }
        return getVehicleId(entity, vehiclePosition.getVehicle());
    }

    /**
     * Returns the vehicle.id for the given VehicleDescriptor if one exists, if not the entity ID is returned in the format
     * "vehicle.id 1234" or "entity ID 4321".
     *
     * @param entity            the entity that the VehiclePosition belongs to
     * @param vehicleDescriptor the vehicleDescriptor to get the ID for
     * @return the vehicle.id for the given VehiclePosition if one exists, if not the entity ID is returned in the format "vehicle.id 1234" or "entity ID 4321".
     */
    public static String getVehicleId(GtfsRealtime.FeedEntity entity, GtfsRealtime.VehicleDescriptor vehicleDescriptor) {
        return vehicleDescriptor.hasId() ? "vehicle.id " + vehicleDescriptor.getId() : "entity ID " + entity.getId();
    }

    /**
     * Returns the stop_sequence for the given StopTimeUpdate if one exists, if not the stop_id is returned in the format
     * "stop_sequence 1234" or "stop_id 9876".
     *
     * @param stopTimeUpdate the stop_time_update to generate the stop_sequence or stop_id text from
     * @return the stop_sequence for the given StopTimeUpdate if one exists, if not the stop_id is returned in the format"stop_sequence 1234" or "stop_id 9876".
     */
    public static String getStopTimeUpdateId(GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate) {
        if (stopTimeUpdate.hasStopSequence()) {
            return "stop_sequence " + stopTimeUpdate.getStopSequence();
        } else {
            return "stop_id " + stopTimeUpdate.getStopId();
        }
    }

    /**
     * Checks for the presence of a "combined feed" with multiple entities at one URL (see #85)
     *
     * @param message GTFS-rt message (containing multiple entities) to be evaluated
     * @return true if the provided message is a combined feed, false if it is not
     */
    public static boolean isCombinedFeed(GtfsRealtime.FeedMessage message) {
        // See if more than one entity type exists in this feed
        int countEntityTypes = 0;
        boolean foundTu = false;
        boolean foundVp = false;
        boolean foundSa = false;
        for (GtfsRealtime.FeedEntity entity : message.getEntityList()) {
            if (entity.hasTripUpdate() && !foundTu) {
                foundTu = true;
                countEntityTypes++;
            }
            if (entity.hasVehicle() && !foundVp) {
                foundVp = true;
                countEntityTypes++;
            }
            if (entity.hasAlert() && !foundSa) {
                foundSa = true;
                countEntityTypes++;
            }
            // Terminate if we've already found more than one entity type
            if (countEntityTypes > 1) {
                return true;
            }
        }
        return false;
    }
}
