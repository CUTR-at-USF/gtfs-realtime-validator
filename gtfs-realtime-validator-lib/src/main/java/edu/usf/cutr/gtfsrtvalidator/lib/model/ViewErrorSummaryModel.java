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

package edu.usf.cutr.gtfsrtvalidator.lib.model;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
@Entity
@NamedNativeQuery(name = "ErrorSummaryByrtfeedID",
    query = "SELECT :gtfsRtId1 AS rtFeedID, errorID AS id, " +
                "title, severity, totalCount, lastTime, " +
                "lastFeedTime, lastIterationId, lastRowId " +
            "FROM Error " +
                "INNER JOIN " +
                "(SELECT errorID, MAX(rowIdentifier) AS lastRowId, " +
                    "count(*) AS totalCount, MAX(iterationId) AS lastIterationId, " +
                    "MAX(iterationTimestamp) AS lastTime, " +
                    "MAX(feedTimestamp) AS lastFeedTime " +
                "FROM MessageLog " +
                    "INNER JOIN " +
                    // Retrieve rowIdentifier for each of unique (iterationId, iterationTimestamp)
                    "(SELECT ROWNUM() AS rowIdentifier, " +
                        "IterationID AS iterationId, " +
                        "IterationTimestamp AS iterationTimestamp, feedTimestamp " +
                    "FROM " +
                        // Retrieve unique IterationID and IterationTimestamp, so that we can get ROWNUM in sequence
                        "(SELECT DISTINCT errorLog.IterationID, errorLog.IterationTimestamp, " +
                            "errorLog.feedTimestamp " +
                        "FROM " +
                            "(SELECT GtfsRtFeedIDIteration.IterationID, " +
                                "GtfsRtFeedIDIteration.IterationTimestamp, " +
                                "GtfsRtFeedIDIteration.feedTimestamp " +
                            "FROM MessageLog " +
                                "INNER JOIN " +
                                "(SELECT  IterationID, IterationTimestamp, feedTimestamp " +
                                "FROM GtfsRtFeedIteration " +
                                "WHERE rtFeedID = :gtfsRtId2) GtfsRtFeedIDIteration " +
                            "ON MessageLog.iterationID = GtfsRtFeedIDIteration.IterationID " +
                                "AND IterationTimestamp >= :sessionStartTime AND IterationTimestamp <= :sessionEndTime " +
                            ") errorLog " +
                            "ORDER BY iterationId " +
                        ") " +
                    ") UniqueRowIdResult " +
                "ON MessageLog.iterationID = UniqueRowIdResult.iterationId " +
                "GROUP BY errorId " +
                ") ErrorCount " +
            "ON Error.errorID = ErrorCount.errorId " +
            "ORDER BY Error.errorID ",
        resultClass = ViewErrorSummaryModel.class)
public class ViewErrorSummaryModel implements Serializable{

    @Column(name="rtFeedID")
    private int gtfsRtId;
    @Column(name="lastTime")
    private long lastTime;
    @Column(name = "lastFeedTime")
    private long lastFeedTime;
    @Column(name="totalCount")
    private int count; // total number of error or warning count
    @Id
    @Column(name = "id")
    private String id; // error or warning ID
    @Column(name = "severity")
    private String severity;
    @Column (name = "title")
    private String title;
    @Column(name = "lastIterationId")
    private int lastIterationId;
    @Column(name = "lastRowId")
    private int lastRowId;
    @Transient
    private String formattedTimestamp;
    @Transient
    private String timeZone;

    public int getGtfsRtId() {
        return gtfsRtId;
    }

    public void setGtfsRtId(int gtfsRtId) {
        this.gtfsRtId = gtfsRtId;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public long getLastFeedTime() {
        return lastFeedTime;
    }

    public void setLastFeedTime(long lastFeedTime) {
        this.lastFeedTime = lastFeedTime;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setFormattedTimestamp(String formattedTimestamp) {
        this.formattedTimestamp = formattedTimestamp;
    }
    public String getFormattedTimestamp() {
        return formattedTimestamp;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public int getLastIterationId() {
        return lastIterationId;
    }

    public void setLastIterationId(int lastIterationId) {
        this.lastIterationId = lastIterationId;
    }

    public int getLastRowId() {
        return lastRowId;
    }

    public void setLastRowId(int lastRowId) {
        this.lastRowId = lastRowId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ViewErrorSummaryModel that = (ViewErrorSummaryModel) o;
        return this.id == null ? that.id == null : this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
