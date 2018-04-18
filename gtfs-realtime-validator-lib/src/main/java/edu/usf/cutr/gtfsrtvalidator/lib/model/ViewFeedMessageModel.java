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

import com.google.transit.realtime.GtfsRealtime;
import com.googlecode.protobuf.format.JsonFormat;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@XmlRootElement
@Entity
@NamedNativeQueries ({
    @NamedNativeQuery (name = "feedMessageByIterationId",
            query = "SELECT feedProtobuf AS feedMessage " +
                    "FROM GtfsRtFeedIteration " +
                    "WHERE IterationID = ? ",
            resultClass = ViewFeedMessageModel.class),
    @NamedNativeQuery (name = "feedMessageByGtfsRtId",
            query = "SELECT feedProtobuf AS feedMessage " +
                    "FROM GtfsRtFeedIteration " +
                    "WHERE rtFeedId = ?  ORDER BY IterationTimestamp DESC",
            resultClass = ViewFeedMessageModel.class)
})
public class ViewFeedMessageModel {

    @Id
    @Column(name = "feedMessage")
    private byte[] byteFeedMessage;

    @Transient
    String jsonFeedMessage;

    public byte[] getByteFeedMessage() {
        return byteFeedMessage;
    }

    public void setByteFeedMessage(byte[] byteFeedMessage) {
        this.byteFeedMessage = byteFeedMessage;
    }

    public String getJsonFeedMessage() {
        return jsonFeedMessage;
    }

    public void setJsonFeedMessage(byte[] byteFeedMessage) {
        InputStream is = new ByteArrayInputStream(byteFeedMessage);
        GtfsRealtime.FeedMessage feedMessage = null;
        try {
            feedMessage = GtfsRealtime.FeedMessage.parseFrom(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.jsonFeedMessage = JsonFormat.printToString(feedMessage);
    }
}
