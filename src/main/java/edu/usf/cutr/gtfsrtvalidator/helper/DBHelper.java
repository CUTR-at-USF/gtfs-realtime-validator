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

package edu.usf.cutr.gtfsrtvalidator.helper;

import edu.usf.cutr.gtfsrtvalidator.api.model.OccurrenceModel;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import org.hibernate.Session;

public class DBHelper {
    public static void saveError(ErrorListHelperModel errorListHelperModel) {
        Session session = GTFSDB.InitSessionBeginTrans();
        int messageId = (int) session.save(errorListHelperModel.getErrorMessage());
        GTFSDB.commitAndCloseSession(session);

        session = GTFSDB.InitSessionBeginTrans();
        for (OccurrenceModel occurrence : errorListHelperModel.getOccurrenceList()) {
            occurrence.setMessageId(messageId);
            session.save(occurrence);
        }
        GTFSDB.commitAndCloseSession(session);
    }

    public static void saveGtfsError(ErrorListHelperModel errorListHelperModel) {
        Session session = GTFSDB.InitSessionBeginTrans();
        int messageId = (int) session.save(errorListHelperModel.getErrorMessage());

        for (OccurrenceModel occurrence : errorListHelperModel.getOccurrenceList()) {
            occurrence.setMessageId(messageId);
            session.save(occurrence);
        }
        GTFSDB.commitAndCloseSession(session);
    }
}
