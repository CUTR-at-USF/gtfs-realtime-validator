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

/**
 * Utility methods that help in processing timestamps
 */
public class TimestampUtils {

    public static long MIN_POSIX_TIME = 1104537600L;  // Minimum valid time for a timestamp to be POSIX (Jan 1, 2005)
    public static long MAX_POSIX_TIME = 1991620134L;  // Maximum valid time for a timestamp to be POSIX (Feb 10, 2033)

    /**
     * Returns true if the timestamp is a valid POSIX time, false if it is not
     *
     * @param timestamp time to validate
     * @return true if the timestamp is a valid POSIX time, false if it is not
     */
    public static boolean isPosix(long timestamp) {
        return timestamp >= MIN_POSIX_TIME && timestamp <= MAX_POSIX_TIME;
    }
}
