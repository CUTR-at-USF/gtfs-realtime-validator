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
import edu.usf.cutr.gtfsrtvalidator.background.RefreshCountTask;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.json.GtfsRtFeeds;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TriggerBackgroundServlet extends HttpServlet {

    private static HashMap<String, ScheduledExecutorService> runningTasks = new HashMap<>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        String gtfsRtFeeds = request.getParameter("gtfsRtFeeds");
        String updateInterval = request.getParameter("updateInterval");

        int interval = 10;

        if (updateInterval != null) {
            try {
                interval = Integer.parseInt(updateInterval);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }

        System.out.println("UPDATE INTERVAL"+ updateInterval);

        Gson gson = new Gson();

        List<GtfsRtFeeds> feeds = Arrays.asList(gson.fromJson(gtfsRtFeeds, GtfsRtFeeds[].class));

        GTFSDB.InitializeDB();

        for (GtfsRtFeeds feed : feeds) {
            startBackgroundTask(feed.getUrl(), interval);
        }
        response.getWriter().println(gtfsRtFeeds);
    }

    public static ScheduledExecutorService startBackgroundTask(String url, int updateInterval) {

        if (!runningTasks.containsKey(url)) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(new RefreshCountTask(url), 0, updateInterval, TimeUnit.SECONDS);
            runningTasks.put(url, scheduler);
            return scheduler;
        }else {
            return runningTasks.get(url);
        }
    }

}
