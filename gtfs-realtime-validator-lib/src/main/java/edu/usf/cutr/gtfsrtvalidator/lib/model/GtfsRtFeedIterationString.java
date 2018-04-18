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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import com.googlecode.protobuf.format.JsonFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GtfsRtFeedIterationString {

    public GtfsRtFeedIterationString(GtfsRtFeedIterationModel iterationModel) {
        setFeedprotobuf(iterationModel.getFeedprotobuf());
        setTimeStamp(iterationModel.getTimeStamp());
        setIterationId(iterationModel.getIterationId());
        setRtFeedId(iterationModel.getGtfsRtFeedModel().getGtfsRtId());
    }

    private int IterationId;
    private long timeStamp;
    private String feedprotobuf;
    private int rtFeedId;

    @XmlElement
    public int getRtFeedId() {
        return rtFeedId;
    }

    public void setRtFeedId(int rtFeedId) {
        this.rtFeedId = rtFeedId;
    }

    @XmlElement
    public int getIterationId() {
        return IterationId;
    }

    public void setIterationId(int iterationId) {
        IterationId = iterationId;
    }

    @XmlElement
    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    @XmlElement
    public String getFeedprotobuf() {
        return feedprotobuf;
    }

    public void setFeedprotobuf(byte[] feedprotobuf) {
        String s = "";

        if (feedprotobuf == null) {
            return;
        }

        try {
            GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.parseFrom(feedprotobuf);
            s = JsonFormat.printToString(feedMessage);
            s.replace('\\', ' ');
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        this.feedprotobuf = s;
    }
}
