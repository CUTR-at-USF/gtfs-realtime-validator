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

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
@Entity
@Table(name = "GtfsRtFeed") 
public class GtfsRtFeedModel implements Serializable {

    @Column(name="feedURL")
    private String gtfsUrl;
    @ManyToOne
    @JoinColumn(name = "gtfsFeedID")
    private GtfsFeedModel gtfsFeedModel;
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

    @Override
    public String toString() {
        return "GtfsRtFeedModel{" +
                "gtfsUrl='" + gtfsUrl + '\'' +
                ", gtfsId=" + gtfsFeedModel.getFeedId() +
                ", gtfsRtId=" + gtfsRtId +
                '}';
    }
}
