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
@Table(name = "GtfsRtFeed")
public class GtfsRtFeedModel implements Serializable {

    @Column(name="feedURL")
    private String gtfsRtUrl;
    @ManyToOne
    @JoinColumn(name = "gtfsFeedID")
    private GtfsFeedModel gtfsFeedModel;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="rtFeedID")
    private int gtfsRtId;
    @Transient
    private boolean enableShapes;

    public GtfsRtFeedModel(){}

    public String getGtfsRtUrl () {
        return gtfsRtUrl;
    }

    public void setGtfsRtUrl (String gtfsRtUrl) {
        this.gtfsRtUrl = gtfsRtUrl;
    }

    public GtfsFeedModel getGtfsFeedModel() {
        return gtfsFeedModel;
    }

    public void setGtfsFeedModel(GtfsFeedModel gtfsFeedModel) {
        this.gtfsFeedModel = gtfsFeedModel;
    }

    public int getGtfsRtId() {
        return gtfsRtId;
    }

    public void setGtfsRtId(int gtfsRtId) {
        this.gtfsRtId = gtfsRtId;
    }

    public boolean getEnableShapes() {
        return enableShapes;
    }

    public void setEnableShapes (boolean enableShapes) {
        this.enableShapes = enableShapes;
    }

    @Override
    public String toString() {
        return "GtfsRtFeedModel{" +
                "gtfsRtUrl='" + gtfsRtUrl + '\'' +
                ", gtfsId=" + gtfsFeedModel.getFeedId() +
                ", gtfsRtId=" + gtfsRtId +
                ", enableShapes=" + enableShapes +
                '}';
    }
}
