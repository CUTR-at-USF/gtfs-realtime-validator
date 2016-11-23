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
@Table(name = "GtfsRtFeed") 
public class GtfsRtFeedModel implements Serializable {
    @Column(name="feedURL")
    private String gtfsUrl;
    @Column(name="gtfsFeedID")    
    private int gtfsId;
    @Column(name="startTime")
    private long startTime;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="rtFeedID")
    private int gtfsRtId;

    public GtfsRtFeedModel(){}

    public String getGtfsUrl() {
        return gtfsUrl;
    }

    public void setGtfsUrl(String gtfsUrl) {
        this.gtfsUrl = gtfsUrl;
    }

    public int getGtfsId() {
        return gtfsId;
    }

    public void setGtfsId(int gtfsId) {
        this.gtfsId = gtfsId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getGtfsRtId() {
        return gtfsRtId;
    }

    public void setGtfsRtId(int gtfsRtId) {
        this.gtfsRtId = gtfsRtId;
    }

    @Override
    public String toString() {
        return "GtfsRtFeedModel{" +
                "gtfsUrl='" + gtfsUrl + '\'' +
                ", gtfsId=" + gtfsId +
                ", startTime=" + startTime +
                ", gtfsRtId=" + gtfsRtId +
                '}';
    }
}
