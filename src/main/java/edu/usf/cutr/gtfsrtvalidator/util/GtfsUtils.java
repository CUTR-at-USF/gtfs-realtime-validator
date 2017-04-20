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

/**
 * Utilities for working with GTFS and GTFS-realtime objects
 */
public class GtfsUtils {

    /**
     * Returns true if this tripDescriptor has a schedule_relationship of ADDED, false if it does not
     *
     * @param tripDescriptor TripDescriptor to examine
     * @return true if this tripDescriptor has a schedule_relationship of ADDED, false if it does not
     */
    public static boolean isAddedTrip(GtfsRealtime.TripDescriptor tripDescriptor) {
        return tripDescriptor.hasScheduleRelationship() && tripDescriptor.getScheduleRelationship() == GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED;
    }
}
