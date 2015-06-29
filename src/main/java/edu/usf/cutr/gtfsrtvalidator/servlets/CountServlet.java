/*
 * Copyright (C) 2015 Nipuna Gunathilake.
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

package edu.usf.cutr.gtfsrtvalidator.servlets;

import com.google.gson.Gson;
import edu.usf.cutr.gtfsrtvalidator.db.Database;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

public class CountServlet extends HttpServlet {

    public URL _tripUpdatesUrl;
    public URL _vehiclePositionsUrl;

    public CountServlet() {
        try {
            _vehiclePositionsUrl = new URL("http://developer.mbta.com/lib/GTRTFS/Alerts/VehiclePositions.pb");
            _tripUpdatesUrl = new URL("http://developer.mbta.com/lib/GTRTFS/Alerts/TripUpdates.pb");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{

        response.setContentType("application/json");
        PrintWriter pw = response.getWriter();

        Gson gson = new Gson();

        String json = gson.toJson(Database.getCount());

        pw.write(json);
    }
}
