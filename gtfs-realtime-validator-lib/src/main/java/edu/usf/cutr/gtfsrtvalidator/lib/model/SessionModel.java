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
@Table(name = "Session")
public class SessionModel implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "sessionId")
    private int sessionId;

    @ManyToOne
    @JoinColumn(name = "rtFeedId")
    private GtfsRtFeedModel gtfsRtFeedModel;

    @Column(name = "startTime")
    private long sessionStartTime;

    @Column(name = "endTime")
    private long sessionEndTime;

    @Column(name = "clientId")
    private String clientId;

    @Column(name = "errorCount")
    private int errorCount = 0;

    @Column(name = "warningCount")
    private int warningCount = 0;

    // To retrieve records sequentially starting from 1
    @Transient
    private int rowId;

    // Holds the required date format of 'sessionStartTime'
    @Transient
    private String startTimeFormat;

    // Holds the required date format of 'sessionEndTime'
    @Transient
    private String endTimeFormat;

    // Holds the elapsed time(in Xh Xm Xs format) from start of a session to end of a session
    @Transient
    private String totalTime;

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public GtfsRtFeedModel getGtfsRtFeedModel() {
        return gtfsRtFeedModel;
    }

    public void setGtfsRtFeedModel(GtfsRtFeedModel gtfsRtFeedModel) {
        this.gtfsRtFeedModel = gtfsRtFeedModel;
    }

    public long getSessionStartTime() {
        return sessionStartTime;
    }

    public void setSessionStartTime(long sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }

    public long getSessionEndTime() {
        return sessionEndTime;
    }

    public void setSessionEndTime(long sessionEndTime) {
        this.sessionEndTime = sessionEndTime;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getStartTimeFormat() {
        return startTimeFormat;
    }

    public void setStartTimeFormat(String startTimeFormat) {
        this.startTimeFormat = startTimeFormat;
    }

    public String getEndTimeFormat() {
        return endTimeFormat;
    }

    public void setEndTimeFormat(String endTimeFormat) {
        this.endTimeFormat = endTimeFormat;
    }

    public String getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(String totalTime) {
        this.totalTime = totalTime;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }
}
