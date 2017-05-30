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

package edu.usf.cutr.gtfsrtvalidator.helper;

/**
* QueryHelper contains queries that are used for database retrievals without any need to persist in database.
*/
public class QueryHelper {

    // Queries all the errors and warnings occurred in a session for a particular 'gtfsRtId'
    public static final String sessionErrorsAndWarnings =
            " SELECT DISTINCT(validationRule.errorId) " +
            " FROM MessageLogModel " +
            " WHERE gtfsRtFeedIterationModel.IterationId IN " +
            " (SELECT IterationId " +
            " FROM GtfsRtFeedIterationModel " +
            " WHERE gtfsRtFeedModel.gtfsRtId = :gtfsRtId AND timeStamp >= :startTime AND timeStamp <= :endTime) ";
}
