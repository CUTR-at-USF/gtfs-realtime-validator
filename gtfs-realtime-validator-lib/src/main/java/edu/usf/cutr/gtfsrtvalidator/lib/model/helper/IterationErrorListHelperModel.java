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
package edu.usf.cutr.gtfsrtvalidator.lib.model.helper;

import edu.usf.cutr.gtfsrtvalidator.lib.model.ViewIterationErrorsModel;

import java.util.ArrayList;
import java.util.List;

public class IterationErrorListHelperModel {

    private List<ViewIterationErrorsModel> viewIterationErrorsModelList;
    private String errorId;
    private String title;
    private int errorOccurrences;

    public IterationErrorListHelperModel() {
        this.viewIterationErrorsModelList = new ArrayList<>();
    }

    public List<ViewIterationErrorsModel> getViewIterationErrorsModelList() {
        return viewIterationErrorsModelList;
    }

    public void setViewIterationErrorsModelList(List<ViewIterationErrorsModel> viewIterationErrorsModelList) {
        this.viewIterationErrorsModelList = viewIterationErrorsModelList;
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

    public int getErrorOccurrences() {
        return errorOccurrences;
    }

    public void setErrorOccurrences(int errorOccurrences) {
        this.errorOccurrences = errorOccurrences;
    }
}
