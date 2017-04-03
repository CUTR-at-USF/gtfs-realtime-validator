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

package edu.usf.cutr.gtfsrtvalidator.validation.gtfs;

import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.GtfsFeedValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * ID: e010
 * Description: If location_type is used in stops.txt, all stops referenced in stop_times.txt must have location_type of 0
 */
public class StopLocationTypeValidator implements GtfsFeedValidator {
    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData) {
        MessageLogModel messageLogModel = new MessageLogModel(ValidationRules.E010);
        List<OccurrenceModel> errorOccurrenceList = new ArrayList<>();

        //Get all StopTime objects from stop_time.txt
        Collection<StopTime> stopTimes = gtfsData.getAllStopTimes();

        List<Stop> checkedStops = new ArrayList<>();

        for (StopTime stopTime : stopTimes) {
            //Create error occurrence if the location type is not equal to 0

            if (!checkedStops.contains(stopTime.getStop())) {
                checkedStops.add(stopTime.getStop());

                if (stopTime.getStop().getLocationType() != 0) {
                    OccurrenceModel occurrenceModel = new OccurrenceModel("location_type is not 0 at ", stopTime.getStop().getName());
                    errorOccurrenceList.add(occurrenceModel);
                }
            }
        }
        return Arrays.asList(new ErrorListHelperModel(messageLogModel, errorOccurrenceList));
    }
}
