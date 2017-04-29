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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods that help in processing timestamps
 */
public class TimestampUtils {

    public static long MIN_POSIX_TIME = 1104537600L;  // Minimum valid time for a timestamp to be POSIX (Jan 1, 2005)
    public static long MAX_POSIX_TIME = 1991620134L;  // Maximum valid time for a timestamp to be POSIX (Feb 10, 2033)

    private static DateFormat mDateFormat = new SimpleDateFormat("yyyyMMdd");
    private static DateFormat mTimeFormat = new SimpleDateFormat("HH:mm:ss");
    private static Pattern mTimePattern = Pattern.compile("^[0-2][0-9]:[0-5][0-9]:[0-5][0-9]$"); // Up to 29 hrs

    /**
     * Returns true if the timestamp is a valid POSIX time, false if it is not
     *
     * @param timestamp time to validate
     * @return true if the timestamp is a valid POSIX time, false if it is not
     */
    public static boolean isPosix(long timestamp) {
        return timestamp >= MIN_POSIX_TIME && timestamp <= MAX_POSIX_TIME;
    }

    /**
     * Returns the age of the GTFS-realtime feed, based on the provided current time and GTFS-realtime header time, in milliseconds
     *
     * @param currentTimeMillis  current time in milliseconds
     * @param headerTimestampSec the timestamp of the GTFS-realtime header, in SECONDS (POSIX time)
     * @return the age of the GTFS-realtime feed, based on the provided current time and GTFS-realtime header time, in milliseconds
     */
    public static long getAge(long currentTimeMillis, long headerTimestampSec) {
        long headerTimeMillis = TimeUnit.SECONDS.toMillis(headerTimestampSec);
        return currentTimeMillis - headerTimeMillis;
    }

    /**
     * Convert seconds after midnight to 24hr clock time like "06:00:00"
     *
     * @param secondsAfterMidnight number of seconds after midnight
     * @return A converted version of time in 24hr clock time like "06:00:00"
     */
    public static String secondsAfterMidnightToClock(int secondsAfterMidnight) {
        return String.format("%02d:%02d:%02d", secondsAfterMidnight / 3600, (secondsAfterMidnight / 60) % 60, secondsAfterMidnight % 60);
    }

    /**
     * Convert POSIX time to 24hr clock time like "06:00:00"
     *
     * @param posixTime POSIX time
     * @param timeZone the timezone used to generate the clock time, or null if the current time zone should be used.  Please refer to http://en.wikipedia.org/wiki/List_of_tz_zones for a list of valid values.
     * @return A converted version of time in 24hr clock time like "06:00:00"
     */
    public static String posixToClock(int posixTime, TimeZone timeZone) {
        if (timeZone != null) {
            mTimeFormat.setTimeZone(timeZone);
        }
        return mTimeFormat.format(TimeUnit.SECONDS.toMillis(posixTime));
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
