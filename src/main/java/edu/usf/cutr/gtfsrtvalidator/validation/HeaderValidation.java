/*
 * Copyright (C) 2011 Nipuna Gunathilake.
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

package edu.usf.cutr.gtfsrtvalidator.validation;

import com.google.transit.realtime.GtfsRealtime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HeaderValidation {

    public static final String START_DATE = "2012/01/01";

    public static void validate(GtfsRealtime.FeedHeader header) {
        long timestamp = header.getTimestamp();
        if (isPosix(timestamp)) {
            System.out.println("Valid timestamp");
        } else {
            System.out.println("Invalid timestamp");
            //TODO: add record to database
        }
    }

    //Checks if the value is a valid Unix date object
    public static boolean isPosix(long timestamp){
        long min_time;
        long max_time;
        try {
            min_time = new SimpleDateFormat("yyyy/MM/dd").parse(START_DATE).getTime()/1000;
            max_time = (long)Math.ceil((double)new Date().getTime()/1000);

        } catch (ParseException e) {
            return false;
        }

        return min_time < timestamp && timestamp <= max_time;

    }
}
