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

package edu.usf.cutr.gtfsrtvalidator.lib.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Entity
@NamedNativeQuery(name = "feedErrorCount",
        query = "SELECT  Error.errorID AS id, errorCount.totalCount " +
                "FROM Error " +
                "INNER JOIN " +
                "(SELECT errorID, count(*) AS totalCount, " +
                "MAX(IterationTimestamp)  AS IterationTimestamp, " +
                "MAX(IterationID) AS lastIterationId " +
                "FROM MessageLog " +
                "INNER JOIN " +
                "(SELECT IterationID, IterationTimestamp " +
                "FROM GtfsRtFeedIteration " +
                "WHERE rtFeedID = :gtfsRtId) GtfsRtFeedIDIteration " +
                "ON MessageLog.iterationID = GtfsRtFeedIDIteration.IterationID " +
                    "AND IterationTimestamp >= :sessionStartTime AND IterationTimestamp <= :sessionEndTime " +
                "GROUP BY errorID) errorCount " +
                "ON Error.errorID = errorCount.errorID ",
        resultClass = ViewGtfsRtFeedErrorCountModel.class)
public class ViewGtfsRtFeedErrorCountModel {

    @Id
    @Column(name = "id")
    private String id;
    @Column(name = "totalCount")
    private int count;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
