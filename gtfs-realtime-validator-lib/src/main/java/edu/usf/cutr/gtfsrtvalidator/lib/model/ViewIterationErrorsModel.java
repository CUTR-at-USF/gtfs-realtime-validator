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
@NamedNativeQuery(name = "IterationIdErrors",
        query = "SELECT ROWNUM() AS rowId, occurrenceId, " +
                    "errorId, title, occurrencePrefix, occurrenceSuffix " +
                "FROM Error " +
                "INNER JOIN " +
                    "(SELECT messageId, errorId, prefix AS occurrencePrefix, occurrenceId " +
                    "FROM " +
                    "Occurrence " +
                    "INNER JOIN " +
                        "(SELECT messageId, errorId " +
                        "FROM MessageLog " +
                        "WHERE iterationId = :iterationId) MessageLogIteration " +
                    "ON Occurrence.messageId = MessageLogIteration.messageId " +
                    "WHERE messageId = :messageId ) OccurrenceList " +
                "ON Error.errorId = OccurrenceList.errorId " +
                "ORDER BY occurrenceId ",
        resultClass = ViewIterationErrorsModel.class)
public class ViewIterationErrorsModel {

    @Id
    @Column(name = "rowId")
    private int rowId;

    @Column(name = "occurrenceId")
    private int occurrenceId;

    @Column(name = "errorId")
    private String errorId;

    // Title of issue/warning from Error table matching errorId
    @Column(name = "title")
    private String title;

    // prefix of issue/warning from Occurrence table
    @Column(name = "occurrencePrefix")
    private String occurrencePrefix;

    @Column(name = "occurrenceSuffix")
    private String occurrenceSuffix;

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public int getOccurrenceId() {
        return occurrenceId;
    }

    public void setOccurrenceId(int occurrenceId) {
        this.occurrenceId = occurrenceId;
    }

    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOccurrencePrefix() {
        return occurrencePrefix;
    }

    public void setOccurrencePrefix(String occurrencePrefix) {
        this.occurrencePrefix = occurrencePrefix;
    }

    public String getOccurrenceSuffix() {
        return occurrenceSuffix;
    }

    public void setOccurrenceSuffix(String occurrenceSuffix) {
        this.occurrenceSuffix = occurrenceSuffix;
    }
}
