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
package edu.usf.cutr.gtfsrtvalidator.util;

import com.google.transit.realtime.GtfsRealtime;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.ShapeFactory;
import org.locationtech.spatial4j.shape.SpatialRelation;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static org.locationtech.spatial4j.context.SpatialContext.GEO;

/**
 * Utilities for working with GTFS and GTFS-realtime objects
 */
public class GtfsUtils {

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
     * Logs the amount of time that a particular activity took, based on the given start time
     *
     * @param log            the log to write to
     * @param prefix         text to write to log before the amount of time that the activity took
     * @param startTimeNanos the starting time of this iteration, in nanoseconds (e.g., System.nanoTime())
     */
    public static void logDuration(org.slf4j.Logger log, String prefix, long startTimeNanos) {
        long durationNanos = System.nanoTime() - startTimeNanos;
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos);
        long durationSeconds = TimeUnit.NANOSECONDS.toSeconds(durationNanos);

        log.info(prefix + durationSeconds + "." + durationMillis + " seconds");
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
     * Returns true if today is an active service date, based on GTFS calendar.txt (serviceCalendar) and GTFS calendar_dates.txt (serviceCalendarDate), false if it is not.
     * If serviceCalendar and serviceCalendarDate are both null, it will return true and assume service is active.
     *
     * @param today               today's date, set to the same timezone as the GTFS data
     * @param serviceCalendar     GTFS data from calendar.txt
     * @param serviceCalendarDate GTFS data from calendar_dates.txt
     * @return true if today is an active service date, based on GTFS calendar.txt (serviceCalendar) and GTFS calendar_dates.txt (serviceCalendarDate), false if it is not.  If serviceCalendar and serviceCalendarDate are both null, it will return true and assume service is active.
     */
    public static boolean isServiceActive(ZonedDateTime today, ServiceCalendar serviceCalendar, ServiceCalendarDate serviceCalendarDate) {
        if (serviceCalendar == null && serviceCalendarDate == null) {
            // No date information - assume active
            return true;
        }
        if (serviceCalendar != null &&
                serviceCalendar.getStartDate().getAsDate().toInstant().isBefore(today.toInstant()) &&
                serviceCalendar.getEndDate().getAsDate().toInstant().isAfter(today.toInstant())) {
//            // GTFS calendar.txt says there is service - check for exceptions from calendar_dates.txt
//            if (serviceCalendarDate != null) {
//                Instant serviceCalendarDateInstant = serviceCalendarDate.getDate().getAsDate().toInstant();
//                if (isSameDayMonthYear(today, serviceCalendarDateInstant.atZone(today.getZone()))
//                        && serviceCalendarDate.getExceptionType() == ServiceCalendarDate.EXCEPTION_TYPE_REMOVE) {
//                    // calendar_dates.txt removed service for this day - return false
//                    return false;
//                }
//            }
            return true;
        }
        if (serviceCalendarDate != null) {
            Instant serviceCalendarDateInstant = serviceCalendarDate.getDate().getAsDate().toInstant();
            if (isSameDayMonthYear(today, serviceCalendarDateInstant.atZone(today.getZone()))) {
                // Dates match - check whether service is added or removed on this day
                if (serviceCalendarDate.getExceptionType() == ServiceCalendarDate.EXCEPTION_TYPE_ADD) {
                    return true;
                }
                if (serviceCalendarDate.getExceptionType() == ServiceCalendarDate.EXCEPTION_TYPE_REMOVE) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the two dates have the same day, month, and year, false if they do not
     *
     * @param date1
     * @param date2
     * @return true if the two dates have the same day, month, and year, false if they do not
     */
    private static boolean isSameDayMonthYear(ZonedDateTime date1, ZonedDateTime date2) {
        if (date1.getDayOfMonth() == date2.getDayOfMonth() &&
                date1.getMonthValue() == date2.getMonthValue() &&
                date1.getYear() == date2.getYear()) {
            return true;
        } else {
            return false;
        }
    }
}
