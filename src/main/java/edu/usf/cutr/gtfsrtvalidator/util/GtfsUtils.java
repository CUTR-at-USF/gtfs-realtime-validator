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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for working with GTFS and GTFS-realtime objects
 */
public class GtfsUtils {

    private static DateFormat mDateFormat = new SimpleDateFormat("YYYYMMDD");
    private static Pattern mTimePattern = Pattern.compile("^[0-2][0-9]:[0-5][0-9]:[0-5][0-9]$"); // Up to 29 hrs

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

        log.debug(prefix + durationSeconds + "." + durationMillis + " seconds");
    }

    /**
     * Returns true if the provided GTFS-rt start_time is in 25:15:35 format, false if it is not.  Note that times
     * can exceed 24 hrs if service goes into the next service day, but are currently capped for validation at 29 hrs.
     *
     * @param startTime GTFS-rt start_time to check for formatting
     * @return true if the provided GTFS-rt start_time is in 25:15:35 format, false if it is not
     */
    public static boolean isValidTimeFormat(String startTime) {
        if (startTime.length() != 8) {
            return false;
        }

        Matcher m = mTimePattern.matcher(startTime);
        return m.matches();
    }

    /**
     * Returns true if the provided GTFS-rt start_date is in YYYYMMDD format, false if it is not
     *
     * @param startDate GTFS-rt start_date to check for formatting
     * @return true if the provided GTFS-rt start_date is in YYYYMMDD format, false if it is not
     */
    public static boolean isValidDateFormat(String startDate) {
        mDateFormat.setLenient(false);

        if (startDate.length() != 8) {
            // SimpleDateFormat doesn't catch 2017011 as bad format, so check length first
            return false;
        }

        int months;
        try {
            months = Integer.parseInt(startDate.substring(4, 6));
        } catch (NumberFormatException e) {
            return false;
        }
        if (months > 12) {
            // SimpleDateFormat doesn't catch 20171301 as bad format, so check that months are less than 13
            return false;
        }

        try {
            mDateFormat.parse(startDate);
        } catch (ParseException e) {
            // Date format or value is invalid
            return false;
        }
        return true;
    }
}
