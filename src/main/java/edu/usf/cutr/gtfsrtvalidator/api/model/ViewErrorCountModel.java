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

package edu.usf.cutr.gtfsrtvalidator.api.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Entity
@NamedNativeQuery(name = "ErrorCountByrtfeedID",
    query="SELECT GtfsRtFeedIteration.IterationID, GtfsRtFeedIteration.IterationTimestamp, GtfsRtFeed.rtFeedID,GtfsRtFeed.feedURL, GtfsRtFeed.gtfsFeedID, IFNULL(errorCount, 0) as errorCount FROM (GtfsRtFeedIteration JOIN GtfsRtFeed ON GtfsRtFeed.rtFeedID = GtfsRtFeedIteration.rtFeedID LEFT JOIN (SELECT iterationID, COUNT(*) AS errorCount FROM MessageLog GROUP BY iterationID) iterationErrors ON iterationErrors.iterationID = GtfsRtFeedIteration.IterationID) WHERE GtfsRtFeed.rtFeedID = ? ORDER BY GtfsRtFeedIteration.IterationID DESC LIMIT ?",
    resultClass = ViewErrorCountModel.class)
public class ViewErrorCountModel implements Serializable {

    public static final String RT_FEED_ID = "rtFeedId";
    public static final String FEED_URL = "feedUrl";
    public static final String ITERATION_TIME = "iterationTimestamp";
    public static final String ITERATION_ID = "iterationId";
    public static final String GTFS_ID = "gtfsFeedId";
    public static final String ERROR_COUNT = "errorCount";
    
    @Column(name="rtFeedID")
    private int gtfsRtId;
    @Column(name="feedURL")
    private String feedUrl;
    @Column(name="IterationTimestamp")
    private long iterationTime;
    @Column(name="gtfsFeedID")
    private int gtfsId;
    @Id
    @Column(name="IterationID")
    private int iterationId;
    @Column(name="errorCount")
    private int errorCount;

    public int getGtfsRtId() {
        return gtfsRtId;
    }

    public void setGtfsRtId(int gtfsRtId) {
        this.gtfsRtId = gtfsRtId;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    public long getIterationTime() {
        return iterationTime;
    }

    public void setIterationTime(long iterationTime) {
        this.iterationTime = iterationTime;
    }

    public int getGtfsId() {
        return gtfsId;
    }

    public void setGtfsId(int gtfsId) {
        this.gtfsId = gtfsId;
    }

    public int getIterationId() {
        return iterationId;
    }

    public void setIterationId(int iterationId) {
        this.iterationId = iterationId;
    }

    public int getErrorCount() {
        return errorCount;
    }
    
    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    @Override
    public String toString() {
        return "ViewErrorCountModel{" +
                "gtfsRtId=" + gtfsRtId +
                ", feedUrl='" + feedUrl + '\'' +
                ", iterationTime=" + iterationTime +
                ", gtfsId=" + gtfsId +
                ", iterationId=" + iterationId +
                ", errorCount=" + errorCount +
                '}';
    }
}
