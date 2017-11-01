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

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
@Entity
@NamedNativeQuery(name = "ErrorLogByrtfeedID",
        query = // Retrieve the remaining columns, title and severity from Error and FinalResult tables on matching errorIds.
                "SELECT rowIdentifier, ? AS rtFeedID, errorId AS id, " +
                    "Error.title, Error.severity, iterationId, occurrence, loggingTime " +
                "FROM Error " +
                "INNER JOIN " +
                    // Retrieve the other required column errorId on matching iterationId from MessageLog and UniqueRowIdResult tables.
                    "(SELECT rowIdentifier, errorId, iterationId, " +
                        "occurrence, loggingTime " +
                    "FROM MessageLog " +
                        "INNER JOIN " +
                        // Retrieve ROWNUM here.
                        "(SELECT ROWNUM() AS rowIdentifier, " +
                            "IterationID AS iterationId, " +
                            "feedTimestamp AS occurrence, " +
                            "IterationTimestamp AS loggingTime " +
                        "FROM " +
                            // Retrieve unique IteraionID and IterationTimestamp to get ROWNUM in sequential order.
                            "(SELECT DISTINCT errorLog.IterationID, errorLog.IterationTimestamp, errorLog.feedTimestamp " +
                            "FROM " +
                                "(SELECT GtfsRtFeedIDIteration.IterationID, " +
                                    "GtfsRtFeedIDIteration.IterationTimestamp, " +
                                    "GtfsRtFeedIDIteration.feedTimestamp " +
                                "FROM MessageLog " +
                                    "INNER JOIN " +
                                    "(SELECT  IterationID, IterationTimestamp, feedTimestamp " +
                                    "FROM GtfsRtFeedIteration " +
                                    "WHERE rtFeedID = ?) GtfsRtFeedIDIteration " +
                                    "ON MessageLog.iterationID = GtfsRtFeedIDIteration.IterationID " +
                                        "AND IterationTimestamp >= ? AND IterationTimestamp <= ? " +
                                ") errorLog " +
                            "ORDER BY IterationID " +
                            ") " +
                        ") UniqueRowIdResult " +
                        "ON MessageLog.iterationId = UniqueRowIdResult.iterationId " +
                    ") FinalResult " +
                "ON Error.errorID = FinalResult.errorId " +
                "WHERE Error.errorID NOT IN (:errorIds) " +
                "ORDER BY iterationId DESC, id ",
        resultClass = ViewErrorLogModel.class)
public class ViewErrorLogModel implements Serializable {

    @Column(name = "rowIdentifier")
    private int rowId;
    @Column(name = "rtFeedID")
    private int gtfsRtId;
    @Id
    @Column(name = "iterationId")
    private int iterationId;
    @Column(name = "occurrence")
    private long occurrence;
    @Column(name = "loggingTime")
    private long loggingTime;
    @Id
    @Column(name = "id")
    private String id; // error or warning ID
    @Column(name = "severity")
    private String severity;
    @Column(name = "title")
    private String title;
    @Transient
    private String formattedTimestamp;
    @Transient
    private String timeZone;

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public int getGtfsRtId() {
        return gtfsRtId;
    }

    public int getIterationId() {
        return iterationId;
    }

    public void setIterationId(int iterationId) {
        this.iterationId = iterationId;
    }

    public void setGtfsRtId(int gtfsRtId) {
        this.gtfsRtId = gtfsRtId;
    }

    public long getOccurrence() {
        return occurrence;
    }

    public void setOccurrence(long occurrence) {
        this.occurrence = occurrence;
    }

    public long getLoggingTime() {
        return loggingTime;
    }

    public void setLoggingTime(long loggingTime) {
        this.loggingTime = loggingTime;
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

    public String getFormattedTimestamp() {
        return formattedTimestamp;
    }

    public void setFormattedTimestamp(String formattedTimestamp) {
        this.formattedTimestamp = formattedTimestamp;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ViewErrorLogModel that = (ViewErrorLogModel) o;
        return this.id == null ? that.id == null : this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}