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

package edu.usf.cutr.gtfsrtvalidator.lib.validation.rules;

import com.google.common.collect.Ordering;
import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.lib.model.MessageLogModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.lib.model.helper.ErrorListHelperModel;
import edu.usf.cutr.gtfsrtvalidator.lib.util.GtfsUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.util.RuleUtils;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.GtfsMetadata;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.ValidationRules;
import edu.usf.cutr.gtfsrtvalidator.lib.validation.interfaces.FeedEntityValidator;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA;
import static com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED;

/**
 * E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
 * E009 - GTFS-rt stop_sequence isn't provided for trip that visits same stop_id more than once
 * E036 - Sequential stop_time_updates have the same stop_sequence
 * E037 - Sequential stop_time_updates have the same stop_id
 * E040 - stop_time_update doesn't contain stop_id or stop_sequence
 * E041 - trip doesn't have any stop_time_updates
 * E042 - arrival or departure provided for NO_DATA stop_time_update
 * E043 - stop_time_update doesn't have arrival or departure
 * E044 - stop_time_update arrival/departure doesn't have delay or time
 * E045 - GTFS-rt stop_time_update stop_sequence and stop_id do not match GTFS
 * E046 - GTFS-rt stop_time_update without time doesn't have arrival/departure_time in GTFS
 * E051 - GTFS-rt stop_sequence not found in GTFS data
 */
public class StopTimeUpdateValidator implements FeedEntityValidator {

    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(StopTimeUpdateValidator.class);

    @Override
    public List<ErrorListHelperModel> validate(long currentTimeMillis, GtfsMutableDao gtfsData, GtfsMetadata gtfsMetadata, GtfsRealtime.FeedMessage feedMessage, GtfsRealtime.FeedMessage previousFeedMessage, GtfsRealtime.FeedMessage combinedFeedMessage) {
        List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();
        List<OccurrenceModel> e002List = new ArrayList<>();
        List<OccurrenceModel> e009List = new ArrayList<>();
        List<OccurrenceModel> e036List = new ArrayList<>();
        List<OccurrenceModel> e037List = new ArrayList<>();
        List<OccurrenceModel> e040List = new ArrayList<>();
        List<OccurrenceModel> e041List = new ArrayList<>();
        List<OccurrenceModel> e042List = new ArrayList<>();
        List<OccurrenceModel> e043List = new ArrayList<>();
        List<OccurrenceModel> e044List = new ArrayList<>();
        List<OccurrenceModel> e045List = new ArrayList<>();
        List<OccurrenceModel> e046List = new ArrayList<>();
        List<OccurrenceModel> e051List = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : entityList) {
            if (entity.hasTripUpdate()) {
                GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                checkE041(entity, tripUpdate, e041List);
                List<StopTime> gtfsStopTimes = null;
                int gtfsStopTimeIndex = 0;
                String tripId = null;
                if (tripUpdate.hasTrip() && tripUpdate.getTrip().hasTripId()) {
                    tripId = tripUpdate.getTrip().getTripId();
                    gtfsStopTimes = gtfsMetadata.getTripStopTimes().get(tripId);
                }

                List<GtfsRealtime.TripUpdate.StopTimeUpdate> rtStopTimeUpdateList = tripUpdate.getStopTimeUpdateList();

                List<Integer> rtStopSequenceList = new ArrayList<>();
                List<String> rtStopIdList = new ArrayList<>();
                Integer previousRtStopSequence = null;
                String previousRtStopId = null;
                boolean foundE009error = false;
                boolean addedStopSequenceFromStopId = false;
                Map<String, List<String>> tripWithMultiStop = gtfsMetadata.getTripsWithMultiStops();
                boolean unknownRtStopSequence = false;
                for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : rtStopTimeUpdateList) {
                    if (!foundE009error && tripId != null && tripWithMultiStop.containsKey(tripId) && !stopTimeUpdate.hasStopSequence()) {
                        // E009 - GTFS-rt stop_sequence isn't provided for trip that visits same stop_id more than once
                        List<String> stopIds = tripWithMultiStop.get(tripId);
                        RuleUtils.addOccurrence(ValidationRules.E009, "trip_id " + tripId + " visits stop_id " + stopIds.toString(), e009List, _log);
                        foundE009error = true;  // Only log error once for this trip
                    }
                    if (previousRtStopSequence != null) {
                        checkE036(entity, previousRtStopSequence, stopTimeUpdate, e036List);
                    }
                    if (previousRtStopId != null) {
                        checkE037(entity, previousRtStopId, stopTimeUpdate, e037List);
                    }
                    previousRtStopSequence = stopTimeUpdate.getStopSequence();
                    previousRtStopId = stopTimeUpdate.getStopId();
                    if (stopTimeUpdate.hasStopSequence()) {
                        rtStopSequenceList.add(stopTimeUpdate.getStopSequence());
                    }
                    if (stopTimeUpdate.hasStopId()) {
                        rtStopIdList.add(stopTimeUpdate.getStopId());
                    }
                    if (gtfsStopTimes != null) {
                        // Loop through GTFS stop_time.txt to try and find a matching GTFS stop
                        while (gtfsStopTimeIndex < gtfsStopTimes.size()) {
                            int gtfsStopSequence = gtfsStopTimes.get(gtfsStopTimeIndex).getStopSequence();
                            Stop gtfsStop = gtfsStopTimes.get(gtfsStopTimeIndex).getStop();
                            boolean foundStopSequence = false;
                            boolean foundStopId = false;
                            if (stopTimeUpdate.hasStopSequence()) {
                                if (gtfsStopSequence == stopTimeUpdate.getStopSequence()) {
                                    // Found a matching stop_sequence from GTFS stop_times.txt
                                    checkE045(entity, tripUpdate, stopTimeUpdate, gtfsStopSequence, gtfsStop, e045List);
                                    checkE046(entity, tripUpdate, stopTimeUpdate, gtfsStopTimes.get(gtfsStopTimeIndex), e046List);
                                    foundStopSequence = true;
                                }
                            }
                            if (stopTimeUpdate.hasStopId()) {
                                if (gtfsStop.getId().getId().equals(stopTimeUpdate.getStopId())) {
                                    /**
                                     * Found a matching stop_id - note that there could be loops in routes, so unlike
                                     * stop_sequence this isn't a definitive match between this stopTimeUpdate and a GTFS stop_times.txt entry
                                     */
                                    foundStopId = true;
                                }
                            }
                            gtfsStopTimeIndex++;
                            if (foundStopSequence) {
                                // We caught up with the stop_sequence in GTFS data - stop so we can pick up from here in next WHILE loop
                                break;
                            } else {
                                if (stopTimeUpdate.hasStopSequence() && gtfsStopTimeIndex == gtfsStopTimes.size()) {
                                    // For E051 - we've reached the last GTFS stop_times.txt record for the GTFS-rt stop_time_update and haven't found stop_sequence (#261)
                                    unknownRtStopSequence = true;
                                }
                                if (!stopTimeUpdate.hasStopSequence() && foundStopId) {
                                    // For E002 - in the case when stop_sequence is missing from the GTFS-rt feed, add the GTFS stop_sequence (See #159)
                                    if (!stopTimeUpdate.hasStopSequence()) {
                                        rtStopSequenceList.add(gtfsStopSequence);
                                        addedStopSequenceFromStopId = true;
                                    }

                                    // E046 hasn't been checked yet if a stop_sequence doesn't exist - check now
                                    checkE046(entity, tripUpdate, stopTimeUpdate, gtfsStopTimes.get(gtfsStopTimeIndex - 1), e046List);
                                    // We caught up with a matching stop_id in GTFS data - stop so we can pick up from here in next WHILE loop
                                    // Note that for routes with loops, we could potentially be stopping prematurely
                                    break;
                                }
                            }
                        }
                    }
                    checkE040(entity, tripUpdate, stopTimeUpdate, e040List);
                    checkE042(entity, tripUpdate, stopTimeUpdate, e042List);
                    checkE043(entity, tripUpdate, stopTimeUpdate, e043List);
                    checkE044(entity, tripUpdate, stopTimeUpdate, e044List);

                    if (unknownRtStopSequence) {
                        // E051 - GTFS-rt stop_sequence not found in GTFS data
                        RuleUtils.addOccurrence(ValidationRules.E051, "GTFS-rt " + GtfsUtils.getTripId(entity, tripUpdate) + " contains stop_sequence " + stopTimeUpdate.getStopSequence(), e051List, _log);
                        // We couldn't find this stopTimeUpdate.stop_sequence in the GTFS stop_times.txt for this trip (E051). To keep validator running complexity
                        // at O(n) for evaluating TripUpdates w/ GTFS stop_times.txt (i.e., don't loop through the entire GTFS stop_times.txt for each GTFS-rt stop_time_update, which would be O(n*m)), we
                        // will skip validating the stop_time_updates for the rest of this trip.  When the producer fixes this erroneous stop_time_update.stop_sequence,
                        // the remaining stop_time_updates for this GTFS-rt trip will be validated.
                        break;
                    }
                }

                boolean sorted = Ordering.natural().isStrictlyOrdered(rtStopSequenceList);
                if (!sorted) {
                    // E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
                    String id = GtfsUtils.getTripId(entity, tripUpdate);
                    RuleUtils.addOccurrence(ValidationRules.E002, id + " stop_sequence " + rtStopSequenceList.toString(), e002List, _log);
                } else if (addedStopSequenceFromStopId) {
                    // TripUpdate was missing at least one stop_sequence
                    if (rtStopSequenceList.size() < rtStopTimeUpdateList.size()) {
                        // We didn't find all of the stop_time_updates in GTFS using stop_id, so stop_time_updates are
                        // out of sequence
                        // E002 - stop_time_updates for a given trip_id must be sorted by increasing stop_sequence
                        String id = GtfsUtils.getTripId(entity, tripUpdate);
                        RuleUtils.addOccurrence(ValidationRules.E002, id + " stop_sequence for stop_ids " + rtStopIdList.toString(), e002List, _log);
                    }
                }
            }
        }

        List<ErrorListHelperModel> errors = new ArrayList<>();
        if (!e002List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E002), e002List));
        }
        if (!e009List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E009), e009List));
        }
        if (!e036List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E036), e036List));
        }
        if (!e037List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E037), e037List));
        }
        if (!e040List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E040), e040List));
        }
        if (!e041List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E041), e041List));
        }
        if (!e042List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E042), e042List));
        }
        if (!e043List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E043), e043List));
        }
        if (!e044List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E044), e044List));
        }
        if (!e045List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E045), e045List));
        }
        if (!e046List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E046), e046List));
        }
        if (!e051List.isEmpty()) {
            errors.add(new ErrorListHelperModel(new MessageLogModel(ValidationRules.E051), e051List));
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
            String id = GtfsUtils.getTripId(entity, entity.getTripUpdate());
            RuleUtils.addOccurrence(ValidationRules.E036, id + " has repeating stop_sequence " + previousStopSequence, errors, _log);
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
            String id = GtfsUtils.getTripId(entity, entity.getTripUpdate());
            StringBuilder prefix = new StringBuilder();
            prefix.append(id);
            prefix.append(" has repeating stop_id ");
            prefix.append(previousStopId);
            if (stopTimeUpdate.hasStopSequence()) {
                prefix.append(" at stop_sequence ");
                prefix.append(stopTimeUpdate.getStopSequence());
            }
            RuleUtils.addOccurrence(ValidationRules.E037, prefix.toString(), errors, _log);
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
            RuleUtils.addOccurrence(ValidationRules.E040, GtfsUtils.getTripId(entity, tripUpdate), errors, _log);
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
            RuleUtils.addOccurrence(ValidationRules.E041, GtfsUtils.getTripId(entity, tripUpdate), errors, _log);
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
            String id = GtfsUtils.getTripId(entity, tripUpdate) + " " + GtfsUtils.getStopTimeUpdateId(stopTimeUpdate);

            if (stopTimeUpdate.hasArrival()) {
                RuleUtils.addOccurrence(ValidationRules.E042, id + " has arrival", errors, _log);
            }
            if (stopTimeUpdate.hasDeparture()) {
                RuleUtils.addOccurrence(ValidationRules.E042, id + " has departure", errors, _log);
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
            String id = GtfsUtils.getTripId(entity, tripUpdate) + " " + GtfsUtils.getStopTimeUpdateId(stopTimeUpdate);
            RuleUtils.addOccurrence(ValidationRules.E043, id, errors, _log);
        }
    }

    /**
     * Checks E044 "stop_time_update arrival/departure doesn't have delay or time", and adds any errors to the provided error list.
     *
     * @param entity         entity that the trip_update is from
     * @param tripUpdate     the trip_update to examine
     * @param stopTimeUpdate the stop_time_update to examine
     * @param errors         the list to add the errors to
     */
    private void checkE044(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripUpdate tripUpdate, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, List<OccurrenceModel> errors) {
        if (stopTimeUpdate.hasScheduleRelationship() && stopTimeUpdate.getScheduleRelationship().equals(SKIPPED)) {
            // SKIPPED stop_time_updates aren't required to have delay or time (arrival/departure are optional) - see #243
            return;
        }
        String id = GtfsUtils.getTripId(entity, tripUpdate) + " " + GtfsUtils.getStopTimeUpdateId(stopTimeUpdate);
        if (stopTimeUpdate.hasArrival()) {
            checkE044StopTimeEvent(stopTimeUpdate.getArrival(), id + " arrival", errors);
        }
        if (stopTimeUpdate.hasDeparture()) {
            checkE044StopTimeEvent(stopTimeUpdate.getDeparture(), id + " departure", errors);
        }
    }

    /**
     * Checks StopTimeEvent for rule E044 - "stop_time_update arrival/departure doesn't have delay or time" and adds any errors to the provided errors list
     *
     * @param stopTimeEvent    the arrival or departure to examine
     * @param occurrencePrefix prefix to use for the OccurrenceModel constructor
     * @param errors           list to add occurrence for E044 to
     */
    private void checkE044StopTimeEvent(GtfsRealtime.TripUpdate.StopTimeEvent stopTimeEvent, String occurrencePrefix, List<OccurrenceModel> errors) {
        if (!stopTimeEvent.hasDelay() && !stopTimeEvent.hasTime()) {
            RuleUtils.addOccurrence(ValidationRules.E044, occurrencePrefix, errors, _log);
        }
    }

    /**
     * Checks E045 "GTFS-rt stop_time_update stop_sequence and stop_id do not match GTFS", and adds any errors to the provided error list.
     *
     * @param entity           entity that the trip_update is from
     * @param tripUpdate       the trip_update to examine
     * @param stopTimeUpdate   the stop_time_update to examine
     * @param gtfsStopSequence the stop_sequence from the GTFS stop_times.txt data
     * @param stop             the GTFS stop that is paired with the provided gtfsStopSequence, using stop_id from the same record in stop_times.txt
     * @param errors           the list to add the errors to
     */
    private void checkE045(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripUpdate tripUpdate, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, int gtfsStopSequence, Stop stop, List<OccurrenceModel> errors) {
        if (stopTimeUpdate.hasStopId() && !stop.getId().getId().equals(stopTimeUpdate.getStopId())) {
            String tripId = "GTFS-rt " + GtfsUtils.getTripId(entity, tripUpdate) + " ";
            String stopSequence = "stop_sequence " + stopTimeUpdate.getStopSequence();
            String stopId = "stop_id " + stopTimeUpdate.getStopId();
            String gtfsSummary = " but GTFS stop_sequence " + gtfsStopSequence + " has stop_id " + stop.getId().getId();
            RuleUtils.addOccurrence(ValidationRules.E045, tripId + stopSequence + " has " + stopId + gtfsSummary, errors, _log);
        }
    }

    /**
     * Checks E046 "GTFS-rt stop_time_update without time doesn't have arrival/departure_time in GTFS", and adds any errors to the provided error list.
     *
     * @param entity         entity that the trip_update is from
     * @param tripUpdate     the trip_update to examine
     * @param stopTimeUpdate the stop_time_update to examine
     * @param gtfsStopTime   the entry from GTFS stop_times.txt that corresponds to the provided GTFS stopTimeUpdate
     * @param errors         the list to add the errors to
     */
    private void checkE046(GtfsRealtime.FeedEntity entity, GtfsRealtime.TripUpdate tripUpdate, GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate, StopTime gtfsStopTime, List<OccurrenceModel> errors) {
        StringBuilder prefixBuilder = new StringBuilder();
        prefixBuilder.append("GTFS-rt " + GtfsUtils.getTripId(entity, tripUpdate) + " ");
        prefixBuilder.append(GtfsUtils.getStopTimeUpdateId(stopTimeUpdate) + " ");
        if (stopTimeUpdate.hasArrival()) {
            if (!stopTimeUpdate.getArrival().hasTime() && !gtfsStopTime.isArrivalTimeSet()) {
                String prefix = prefixBuilder.toString() + "arrival.time";
                RuleUtils.addOccurrence(ValidationRules.E046, prefix, errors, _log);
            }
        }
        if (stopTimeUpdate.hasDeparture()) {
            if (!stopTimeUpdate.getDeparture().hasTime() && !gtfsStopTime.isDepartureTimeSet()) {
                String prefix = prefixBuilder.toString() + "departure.time";
                RuleUtils.addOccurrence(ValidationRules.E046, prefix, errors, _log);
            }
        }
    }
}
