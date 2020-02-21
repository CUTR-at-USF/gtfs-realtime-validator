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

import org.apache.commons.io.FilenameUtils;

import java.text.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
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
    private static String dateFormat = "yyyyMMdd";
    private static String timeFormat = "HH:mm:ss";
    private static DateTimeFormatter mDateFormat = new DateTimeFormatterBuilder().parseStrict()
            .parseCaseInsensitive().appendPattern(dateFormat).toFormatter();
    private static ThreadLocal<DateFormat> mTimeFormatTLocal = ThreadLocal.withInitial(() -> new SimpleDateFormat(timeFormat));
    private static Pattern mTimePattern = Pattern.compile("^[0-2]?[0-9]:[0-5][0-9]:[0-5][0-9]$"); // Up to 29 hrs
    private static ThreadLocal<DecimalFormat> mDecimalFormatTLocal= ThreadLocal.withInitial(() -> new DecimalFormat("0.0##",
            new DecimalFormatSymbols(Locale.US)));

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
     * Returns the age of the feed timestamp in milliseconds, based on the provided current time (in milliseconds) and provided feed timestamp (in POSIX time)
     *
     * @param currentTimeMillis  current time in milliseconds
     * @param timestampSec the timestamp from the GTFS-realtime feed (header, vehicle, or trip), in SECONDS (POSIX time)
     * @return the age of the provided timestamp in milliseconds, based on the provided current time (in milliseconds) and timestampSec (in milliseconds)
     */
    public static long getAge(long currentTimeMillis, long timestampSec) {
        long headerTimeMillis = TimeUnit.SECONDS.toMillis(timestampSec);
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
    public static String posixToClock(long posixTime, TimeZone timeZone) {
        DateFormat mTimeFormat = mTimeFormatTLocal.get();
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
        if (startTime.length() != 7 && startTime.length() != 8) {
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
        if (startDate.length() != 8) {
            // SimpleDateFormat doesn't catch 2017011 as bad format, so check length first
            return false;
        }
        java.text.ParsePosition position = new ParsePosition(0);
        position.setIndex(0);
        position.setErrorIndex(-1);
        try{
            mDateFormat.parse(startDate, position);
        }catch(DateTimeParseException e) {
            return false;
        }
        return true;
    }

    /**
     * Returns the elapsed time between the provided start and end times
     *
     * @param startTimeNanos the starting time in nanoseconds
     * @param endTimeNanos   the ending time in nanoseconds
     * @return the elapsed time as the difference between endTimeNanos and startTimeNanos, in seconds as a decimal (0.22)
     */
    public static double getElapsedTime(long startTimeNanos, long endTimeNanos) {
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(endTimeNanos - startTimeNanos);
        return durationMillis / 1000f;
    }

    /**
     * Returns the elapsed time as a human readable string, like "2.5 seconds"
     *
     * @param elapsedTimeSeconds elapsed time as decimal seconds (e.g., 2.5)
     * @return a the elapsed time as a human readable string, like "2.5 seconds"
     */
    public static String getElapsedTimeString(double elapsedTimeSeconds) {
        DecimalFormat mDecimalFormat = mDecimalFormatTLocal.get();
        return mDecimalFormat.format(elapsedTimeSeconds) + " seconds";
    }

    /**
     * Logs the amount of time that a particular activity took, based on the given start time, in the format
     * of:
     * <p>
     * prefix + "0.22 seconds"
     *
     * @param log            the log to write to
     * @param prefix         text to write to log before the amount of time that the activity took
     * @param startTimeNanos the starting time of this iteration, in nanoseconds (e.g., System.nanoTime())
     */
    public static void logDuration(org.slf4j.Logger log, String prefix, long startTimeNanos) {
        double elapsedTime = getElapsedTime(startTimeNanos, System.nanoTime());
        log.info(prefix + getElapsedTimeString(elapsedTime));
    }

    /**
     * Returns a timestamp in UTC time extracted from a file name in the format of "TripUpdates-2017-02-18T20-01-08Z.pb".
     * The last 20 characters of the file name, prior to the file extension, must be the time in the above format.
     * The prefix prior to the last 20 characters and the file extension can be any characters.
     *
     * @param fileName name of the file containing the time and date in the format of "TripUpdates-2017-02-18T20-01-08Z.pb"
     * @return time, measured in milliseconds, between the date/time in the file name and midnight, January 1, 1970 UTC
     */
    public static long getTimestampFromFileName(String fileName) {
        String fileNameNoExtension = FilenameUtils.removeExtension(fileName);
        String date = fileNameNoExtension.substring(fileNameNoExtension.length() - 20, fileNameNoExtension.length() - 10);
        String time = fileNameNoExtension.substring(fileNameNoExtension.length() - 10, fileNameNoExtension.length()).replaceAll("-", ":");
        ZonedDateTime zdt = ZonedDateTime.parse(date + time, DateTimeFormatter.ISO_DATE_TIME);
        return zdt.toInstant().toEpochMilli();
    }

    /**
     * Returns true if the timestampSec is too far in the future - if the difference between the provided current time
     * (in milliseconds) and the provided current timestampSec (in POSIX time) is greater than the provided tolerance in seconds, or false
     * if it's not too far in the future (difference is less than or equal to tolerance).
     *
     * @param currentTimeMillis current time, in milliseconds
     * @param timestampSec the provided timestampSec to examine, in POSIX time (seconds)
     * @param toleranceSec the provided tolerance to use when checking the timestamp in seconds - if the provided
     *                     timestampSec age is negative and greater than or equal to this value, it will return true
     * @return true if the timestampSec is too far in the future - if the difference between the provided current time
     * (in milliseconds) and the provided current timestampSec (in POSIX time) is greater than the provided tolerance in seconds, or false
     * if it's not too far in the future (difference is less than or equal to tolerance).
     */
    public static boolean isInFuture(long currentTimeMillis, long timestampSec, long toleranceSec) {
        long ageMillis = getAge(currentTimeMillis, timestampSec);
        return ageMillis < 0 && (TimeUnit.MILLISECONDS.toSeconds(Math.abs(ageMillis)) > toleranceSec);
    }
}