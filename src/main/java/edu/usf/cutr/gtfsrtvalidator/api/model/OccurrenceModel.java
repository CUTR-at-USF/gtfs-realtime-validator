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
@Table(name="GtfsOccurrence")
public class OccurrenceModel implements Serializable {
    public OccurrenceModel(String elementPath, String elementValue) {
        this.elementPath = elementPath;
        this.elementValue = elementValue;
    }

    public OccurrenceModel() {
    }

    public static final String OCCURRENCE_ID = "occurrenceId";
    public static final String MESSAGE_ID = "messageId";
    public static final String ELEMENT_PATH = "elementPath";
    public static final String ELEMENT_VALUE = "elementValue";

    @Id
    @Column(name="occurrenceID")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int occurrenceId;
    @Column(name="messageID")
    private int messageId;
    @Column(name="elementPath")
    private String elementPath;
    @Column(name="elementValue")
    private String elementValue;

    public int getOccurrenceId() {
        return occurrenceId;
    }

    public void setOccurrenceId(int occurrenceId) {
        this.occurrenceId = occurrenceId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getElementPath() {
        return elementPath;
    }

    public void setElementPath(String elementPath) {
        this.elementPath = elementPath;
    }

    public String getElementValue() {
        return elementValue;
    }

    public void setElementValue(String elementValue) {
        this.elementValue = elementValue;
    }
}
