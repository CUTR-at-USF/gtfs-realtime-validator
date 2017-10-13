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
@Table(name = "GtfsFeed") 
public class GtfsFeedModel implements Serializable {

    @Column(name="feedUrl")
    private String gtfsUrl;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="feedID")
    private int feedId;
    @Column(name="downloadTimestamp")
    private long startTime;
    @Column(name="fileLocation")
    private String feedLocation;
    @Column(name = "agency")
    private String agency;
    @Column(name="fileChecksum")
    @Lob
    private byte[] checksum;
    @Column(name = "errorCount")
    private int errorCount;

    public GtfsFeedModel(){}

    public String getGtfsUrl() {
        return gtfsUrl;
    }

    public void setGtfsUrl(String gtfsUrl) {
        this.gtfsUrl = gtfsUrl;
    }

    public int getFeedId() {
        return feedId;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }

    public long getStartTime() {
        return startTime;
    }
    
    public byte[] getChecksum() {
        return checksum;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getFeedLocation() {
        return feedLocation;
    }

    public void setFeedLocation(String feedLocation) {
        this.feedLocation = feedLocation;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    @Override
    public String toString() {
        return "GtfsFeedModel{" +
                "gtfsUrl='" + gtfsUrl + '\'' +
                ", feedId=" + feedId +
                ", startTime=" + startTime +
                ", feedLocation='" + feedLocation + '\'' +
                ", checkSum=" + checksum +
                ", errorCount=" + errorCount +
                '}';
    }
}

