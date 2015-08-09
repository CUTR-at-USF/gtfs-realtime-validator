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
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HeaderValidation {

    public static final String START_DATE = "2012/01/01";

    public static ErrorListHelperModel validate(GtfsRealtime.FeedHeader header) {
        long timestamp = header.getTimestamp();

        //w001: Check if timestamp is populated
        if (timestamp == 0) {
            System.out.println("Timestamp not present");
            
            ErrorListHelperModel errorMessage = new ErrorListHelperModel();

            MessageLogModel messageLogModel = new MessageLogModel();
            messageLogModel.setErrorId("w001");

            List<OccurrenceModel> errorOccurrenceList = new ArrayList<>();
            OccurrenceModel errorOccurrence = new OccurrenceModel();
            errorOccurrence.setElementPath("$.header.timestamp");
            errorOccurrence.setElementValue(String.valueOf(timestamp));
            errorOccurrenceList.add(errorOccurrence);

            errorMessage.setErrorMessage(messageLogModel);
            errorMessage.setOccurrenceList(errorOccurrenceList);

            return errorMessage; //the method returns as checking the POSIX isn't needed if there is no timestamp
        }

        //e001: Check if the timestamp is in POSIX format
        if (isPosix(timestamp)) {
            //System.out.println("Valid timestamp");
        } else {
            System.out.println("Timestamp not in Unix format timestamp");
            //TODO: add record to database
        }

        return null;
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
