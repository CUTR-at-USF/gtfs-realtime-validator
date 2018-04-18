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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Entity
@NamedNativeQuery(name = "feedUniqueResponseCount",
        query = "SELECT count(*) AS uniqueFeedCount " +
                "FROM GtfsRtFeedIteration " +
                "WHERE (rtFeedID = ? " +
                    "AND IterationTimestamp >= ? AND IterationTimestamp <= ? " +
                    "AND feedProtobuf IS NOT NULL) ",
        resultClass = ViewFeedUniqueResponseCount.class)
public class ViewFeedUniqueResponseCount {

    @Id
    @Column(name = "uniqueFeedCount")
    private int uniqueFeedCount;

    public int getUniqueFeedCount() {
        return uniqueFeedCount;
    }

    public void setUniqueFeedCount(int uniqueFeedCount) {
        this.uniqueFeedCount = uniqueFeedCount;
    }
}
