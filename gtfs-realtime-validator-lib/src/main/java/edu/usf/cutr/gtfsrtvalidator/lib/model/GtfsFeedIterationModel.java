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
@Table(name="GtfsFeedIteration")
public class GtfsFeedIterationModel implements Serializable {

    public GtfsFeedIterationModel() {}

    @Id
    @Column(name="IterationID")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int IterationId;
    @Column(name="IterationTimestamp")
    private long timeStamp;
    @ManyToOne
    @JoinColumn(name = "feedID")
    private GtfsFeedModel gtfsFeedModel;

    public GtfsFeedModel getGtfsFeedModel() {
        return gtfsFeedModel;
    }

    public void setGtfsFeedModel(GtfsFeedModel gtfsFeedModel) {
        this.gtfsFeedModel = gtfsFeedModel;
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

}
