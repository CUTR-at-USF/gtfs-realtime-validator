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
@Table(name="Occurrence")
public class OccurrenceModel implements Serializable {

    public OccurrenceModel(String prefix) {
        this.prefix = prefix;
    }

    public OccurrenceModel() {
    }

    @Id
    @Column(name="occurrenceID")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int occurrenceId;

    @ManyToOne
    @JoinColumn(name = "messageID")
    private MessageLogModel messageLogModel;

    /**
     * This is used along with ValidationRule.occurrenceSuffix to create a description of this occurrence of an error/warning.
     * <p>
     * For example, for E004 "GTFS-rt trip_ids must appear in GTFS data", the error message we want to show the user could be
     * "trip_id 6234 doesn't appear in the GTFS data"
     * <p>
     * For this message, the prefix would be:
     * "trip_id 6234"
     * <p>
     * And the second part of the text (stored in ValidationRule.occurrenceSuffix) would be:
     * "doesn't appear in the GTFS data"
     *
     * @see ValidationRule
     */
    @Column(name = "prefix", length = 1000)
    private String prefix;

    public int getOccurrenceId() {
        return occurrenceId;
    }

    public void setOccurrenceId(int occurrenceId) {
        this.occurrenceId = occurrenceId;
    }

    public MessageLogModel getMessageLogModel() {
        return messageLogModel;
    }

    public void setMessageLogModel(MessageLogModel messageLogModel) {
        this.messageLogModel = messageLogModel;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
