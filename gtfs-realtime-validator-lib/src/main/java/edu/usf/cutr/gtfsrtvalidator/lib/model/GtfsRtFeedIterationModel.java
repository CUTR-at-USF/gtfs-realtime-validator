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
@Table(name = "GtfsRtFeedIteration")
public class GtfsRtFeedIterationModel implements Serializable {

    public GtfsRtFeedIterationModel() {}

    public GtfsRtFeedIterationModel(long timeStamp, long feedTimestamp, byte[] feedprotobuf, GtfsRtFeedModel gtfsRtFeedModel, byte[] feedHash) {
        this.timeStamp = timeStamp;
        this.feedTimestamp = feedTimestamp;
        this.feedprotobuf = feedprotobuf;
        this.gtfsRtFeedModel = gtfsRtFeedModel;
        this.feedHash = feedHash;
    }

    @Id
    @Column(name="IterationID")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int IterationId;
    @Column(name="IterationTimestamp")
    private long timeStamp;
    @Column(name = "feedTimestamp")
    private long feedTimestamp;
    @Column(name="feedProtobuf")
    @Lob
    private byte[] feedprotobuf;
    @ManyToOne
    @JoinColumn(name = "rtFeedID")
    private GtfsRtFeedModel gtfsRtFeedModel;
    @Column(name = "feedHash")
    private byte[] feedHash;

    /*
     * '@Transient' does not persist 'dateFormat' to the database i.e., 'dateFormat' is not added as a column in this table.
     * If 'dateFormat' is set, it contains the date format representation of 'feedTimestamp'.
     */
    @Transient
    private String dateFormat;

    public GtfsRtFeedModel getGtfsRtFeedModel() {
        return gtfsRtFeedModel;
    }

    public void setGtfsRtFeedModel(GtfsRtFeedModel gtfsRtFeedModel) {
        this.gtfsRtFeedModel = gtfsRtFeedModel;
    }

    public int getIterationId() {
        return IterationId;
    }

    public void setIterationId(int iterationId) {
        IterationId = iterationId;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getFeedTimestamp() {
        return feedTimestamp;
    }

    public void setFeedTimestamp(long feedTimestamp) {
        this.feedTimestamp = feedTimestamp;
    }

    public byte[] getFeedprotobuf() {
        return feedprotobuf;
    }

    public void setFeedprotobuf(byte[] feedprotobuf) {
        this.feedprotobuf = feedprotobuf;
    }

    public byte[] getFeedHash() {
        return feedHash;
    }

    public void setFeedHash(byte[] feedHash) {
        this.feedHash = feedHash;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
