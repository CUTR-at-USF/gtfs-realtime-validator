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

package edu.usf.cutr.gtfsrtvalidator.validation.entity;

import com.google.common.collect.Ordering;
import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ID: e002
 * Description: stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
 */
public class StopTimeSequenceValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(StopTimeSequenceValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(GtfsDaoImpl gtfsData, GtfsRealtime.FeedMessage feedMessage) {
        List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();

        MessageLogModel messageLogModel = new MessageLogModel(ValidationRules.E002);
        List<OccurrenceModel> errorOccurrenceList = new ArrayList<>();

        List<GtfsRealtime.FeedEntity> tripUpdateList = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasTripUpdate()) {
                tripUpdateList.add(entity);
            }
        }

        for (GtfsRealtime.FeedEntity tripUpdateEntity : tripUpdateList) {
            List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdateList = tripUpdateEntity.getTripUpdate().getStopTimeUpdateList();

            List<Integer> stopSequenceList = new ArrayList<>();
            for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdateList) {
                stopTimeUpdate.getStopSequence();
                stopSequenceList.add(stopTimeUpdate.getStopSequence());
            }

            boolean sorted = Ordering.natural().isOrdered(stopSequenceList);
            if (!sorted) {
                _log.debug("StopSequenceList is not in order");
                String feedId = tripUpdateEntity.getId();
                OccurrenceModel occurrenceModel = new OccurrenceModel("$.entity[?(@.id == \"" + feedId + "\")]", stopSequenceList.toString());
                errorOccurrenceList.add(occurrenceModel);
            }
        }

        if (!errorOccurrenceList.isEmpty()) {
            return Arrays.asList(new ErrorListHelperModel(messageLogModel, errorOccurrenceList));
        }
        return null;
    }
}
