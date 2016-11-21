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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Entity
@Table(name = "GtfsFeed") 
public class GtfsFeedModel implements Serializable {
    @Column(name="feedUrl")
    private String gtfsUrl;
    @Id
    //@GeneratedValue(generator="GtfsFeed")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="feedID")
    private int feedId;
    @Column(name="downloadTimestamp")
    private long startTime;
    @Column(name="fileLocation")
    private String feedLocation;
    @Column(name="fileChecksum")
    private byte[] checksum;

    public static String FEEDID = "feedId";
    public static String FEEDURL = "feedUrl";
    public static String FILELOCATION = "fileLocation";
    public static String TIMESTAMP = "downloadTimestamp";

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
    
    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        return "GtfsFeedModel{" +
                "gtfsUrl='" + gtfsUrl + '\'' +
                ", feedId=" + feedId +
                ", startTime=" + startTime +
                ", feedLocation='" + feedLocation + '\'' +
                '}';
    }
}

