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

package edu.usf.cutr.gtfsrtvalidator.helper;

import edu.usf.cutr.gtfsrtvalidator.lib.model.ErrorMessageModel;

import javax.ws.rs.core.Response;

public class HttpMessageHelper {
    /**
     * Method for generating error response given a message title, description and the error status code
     * @param title Title of the error
     * @param message Detailed description of the error
     * @param errorStatus Status code of the error
     * @return Returns a Response generated with the provided details
     */
    public static Response generateError(String title, String message, Response.Status errorStatus) {
        ErrorMessageModel errorMessageModel = new ErrorMessageModel(title, message);
        return Response.status(errorStatus).entity(errorMessageModel).build();
    }
}
