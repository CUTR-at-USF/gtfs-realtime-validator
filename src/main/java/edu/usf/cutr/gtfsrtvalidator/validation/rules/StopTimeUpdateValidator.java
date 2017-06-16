/*
 * Copyright (C) 2011-2017 Nipuna Gunathilake, University of South Florida
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

package edu.usf.cutr.gtfsrtvalidator.validation.rules;

import com.google.common.collect.Ordering;
import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.background.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA;
import static com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED;
import static edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils.getStopTimeUpdateId;
import static edu.usf.cutr.gtfsrtvalidator.util.GtfsUtils.getTripId;
import static edu.usf.cutr.gtfsrtvalidator.validation.ValidationRules.*;

/**
 * E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
 * E036 - Sequential stop_time_updates have the same stop_sequence
 * E037 - Sequential stop_time_updates have the same stop_id
 * E040 - stop_time_update doesn't contain stop_id or stop_sequence
 * E041 - trip doesn't have any stop_time_updates
 * E042 - arrival or departure provided for NO_DATA stop_time_update
 * E043 - stop_time_update doesn't have arrival or departure
 */
public class StopTimeUpdateValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(StopTimeUpdateValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsDaoImpl gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage) {
        List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();
        List<OccurrenceModel> e002List = new ArrayList<>();
        List<OccurrenceModel> e036List = new ArrayList<>();
        List<OccurrenceModel> e037List = new ArrayList<>();
        List<OccurrenceModel> e040List = new ArrayList<>();
        List<OccurrenceModel> e041List = new ArrayList<>();
        List<OccurrenceModel> e042List = new ArrayList<>();
        List<OccurrenceModel> e043List = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                checkE041(entity, tripUpdate, e041List);
                List<GtfsRealtime.TripUpdate.StopTimeUpdate> stopTimeUpdateList = tripUpdate.getStopTimeUpdateList();

                List<Integer> stopSequenceList = new ArrayList<>();
                Integer previousStopSequence = null;
                String previousStopId = null;
                for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : stopTimeUpdateList) {
                    if (previousStopSequence != null) {
                        checkE036(entity, previousStopSequence, stopTimeUpdate, e036List);
                    }
                    if (previousStopId != null) {
                        checkE037(entity, previousStopId, stopTimeUpdate, e037List);
                    }
                    previousStopSequence = stopTimeUpdate.getStopSequence();
                    previousStopId = stopTimeUpdate.getStopId();
                    if (stopTimeUpdate.hasStopSequence()) {
                        stopSequenceList.add(stopTimeUpdate.getStopSequence());
                    }
                    checkE040(entity, tripUpdate, stopTimeUpdate, e040List);
                    checkE042(entity, tripUpdate, stopTimeUpdate, e042List);
                    checkE043(entity, tripUpdate, stopTimeUpdate, e043List);
                }

                boolean sorted = Ordering.natural().isOrdered(stopSequenceList);
                if (!sorted) {
                    String id = getTripId(entity, tripUpdate);
                    OccurrenceModel om = new OccurrenceModel(id + " stop_sequence " + stopSequenceList.toString());
                    e002List.add(om);
                    _log.debug(om.getPrefix() + " " + E002.getOccurrenceSuffix());
                }

                // TODO - detect out-of-order stops when stop_sequence isn't provided - see https://github.com/CUTR-at-USF/gtfs-realtime-validator/issues/159
            }
        }

        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!e002List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E002), e002List));
        }
        if (!e036List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E036), e036List));
        }
        if (!e037List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E037), e037List));
        }
        if (!e040List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E040), e040List));
        }
        if (!e041List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E041), e041List));
        }
        if (!e042List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E042), e042List));
        }
        if (!e043List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(E043), e043List));
        }
        return errors;
    }

    /**
     * Checks E036 - if the provided previousStopSequence value is the same as the current stopTimeUpdate stop_sequence
     * it adds an error to the provided error list.
     *
     * @param entity               entity that the stopTimeUpdate is from
     * @param previousStopSequence the stop_sequence for the previous StopTimeUpdate
     * @param stopTimeUpdate       the current stopTimeUpdate
     * @param errors               the list to add the errors to
     */
    private void checkE036(GtfsRealtime.FeedEntity entity, Integer previousStopSequence, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, List<OccurrenceModel> errors) {
        if (stopTimeUpdate.hasStopSequence() &&
                previousStopSequence == stopTimeUpdate.getStopSequence()) {
            String id = getTripId(entity, entity.getTripUpdate());
            OccurrenceModel om = new OccurrenceModel(id + " has repeating stop_sequence " + previousStopSequence);
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E036.getOccurrenceSuffix());
        }
    }

    /**
     * Checks E037 - if the provided previousStopId value is the same as the current stopTimeUpdate stop_id it adds
     * an error to the provided error list.
     *
     * @param entity         entity that the stopTimeUpdate is from
     * @param previousStopId the stop_id for the previous StopTimeUpdate
     * @param stopTimeUpdate the current stopTimeUpdate
     * @param errors         the list to add the errors to
     */
    private void checkE037(GtfsRealtime.FeedEntity entity, String previousStopId, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, List<OccurrenceModel> errors) {
        if (!previousStopId.isEmpty() && stopTimeUpdate.hasStopId() &&
                previousStopId.equals(stopTimeUpdate.getStopId())) {
            String id = getTripId(entity, entity.getTripUpdate());
            StringBuilder prefix = new StringBuilder();
            prefix.append(id);
            prefix.append(" has repeating stop_id ");
            prefix.append(previousStopId);
            if (stopTimeUpdate.hasStopSequence()) {
                prefix.append(" at stop_sequence ");
                prefix.append(stopTimeUpdate.getStopSequence());
            }
            OccurrenceModel om = new OccurrenceModel(prefix.toString());
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E036.getOccurrenceSuffix());
        }
    }

    /**
     * Checks E040 "stop_time_update doesn't contain stop_id or stop_sequence", and adds any errors to the provided error list.
     *
     * @param entity         entity that the stopTimeUpdate is from
     * @param tripUpdate     the trip_update for the StopTimeUpdate
     * @param stopTimeUpdate the stop_time_update to check for E040
     * @param errors         the list to add the errors to
     */
    private void checkE040(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripUpdate tripUpdate, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, List<OccurrenceModel> errors) {
        if (!stopTimeUpdate.hasStopSequence() && !stopTimeUpdate.hasStopId()) {
            OccurrenceModel om = new OccurrenceModel(getTripId(entity, tripUpdate));
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E040.getOccurrenceSuffix());
        }
    }

    /**
     * Checks E041 "trip doesn't have any stop_time_updates", and adds any errors to the provided error list.
     *
     * @param entity     entity that the trip_update is from
     * @param tripUpdate the trip_update to examine
     * @param errors     the list to add the errors to
     */
    private void checkE041(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripUpdate tripUpdate, List<OccurrenceModel> errors) {
        if (tripUpdate.getStopTimeUpdateCount() < 1) {
            if (tripUpdate.hasTrip() &&
                    tripUpdate.getTrip().hasScheduleRelationship() &&
                    tripUpdate.getTrip().getScheduleRelationship().equals(GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED)) {
                // No errors - the trip was canceled, so it doesn't need any stop_time_updates - return
                return;
            }

            OccurrenceModel om = new OccurrenceModel(getTripId(entity, tripUpdate));
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E041.getOccurrenceSuffix());
        }
    }

    /**
     * Checks E042 "arrival or departure provided for NO_DATA stop_time_update", and adds any errors to the provided error list.
     *
     * @param entity         entity that the trip_update is from
     * @param tripUpdate     the trip_update to examine
     * @param stopTimeUpdate the stop_time_update to examine
     * @param errors         the list to add the errors to
     */
    private void checkE042(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripUpdate tripUpdate, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, List<OccurrenceModel> errors) {
        if (stopTimeUpdate.hasScheduleRelationship() &&
                stopTimeUpdate.getScheduleRelationship().equals(NO_DATA)) {
            String id = getTripId(entity, tripUpdate) + " " + getStopTimeUpdateId(stopTimeUpdate);

            if (stopTimeUpdate.hasArrival()) {
                OccurrenceModel om = new OccurrenceModel(id + " has arrival");
                errors.add(om);
                _log.debug(om.getPrefix() + " " + E042.getOccurrenceSuffix());
            }
            if (stopTimeUpdate.hasDeparture()) {
                OccurrenceModel om = new OccurrenceModel(id + " has departure");
                errors.add(om);
                _log.debug(om.getPrefix() + " " + E042.getOccurrenceSuffix());
            }
        }
    }

    /**
     * Checks E043 "stop_time_update doesn't have arrival or departure", and adds any errors to the provided error list.
     *
     * @param entity         entity that the trip_update is from
     * @param tripUpdate     the trip_update to examine
     * @param stopTimeUpdate the stop_time_update to examine
     * @param errors         the list to add the errors to
     */
    private void checkE043(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripUpdate tripUpdate, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, List<OccurrenceModel> errors) {
        if (!stopTimeUpdate.hasArrival() && !stopTimeUpdate.hasDeparture()) {
            if (stopTimeUpdate.hasScheduleRelationship() &&
                    (stopTimeUpdate.getScheduleRelationship().equals(SKIPPED) ||
                            stopTimeUpdate.getScheduleRelationship().equals(NO_DATA))) {
                // stop_time_updates with SKIPPED or NO_DATA aren't required to have arrival or departures - return
                return;
            }

            String id = getTripId(entity, tripUpdate) + " " + getStopTimeUpdateId(stopTimeUpdate);
            OccurrenceModel om = new OccurrenceModel(id);
            errors.add(om);
            _log.debug(om.getPrefix() + " " + E043.getOccurrenceSuffix());
        }
    }
}
