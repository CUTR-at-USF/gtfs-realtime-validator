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

package edu.usf.cutr.gtfsrtvalidator.background;

import com.google.protobuf.Descriptors;
import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class RefreshCountTask implements Runnable {

    public URL _feedUrl;

    public RefreshCountTask(String url) {
        try {
            _feedUrl = new URL(url);
        } catch (MalformedURLException e) {
            System.out.println("URL Malformed at RefreshCountTask constructors");
        }
    }

    @Override
    public void run() {
        System.out.println(_feedUrl);

        int vehicleCount = 0;
        int tripCount = 0;
        int alertCount = 0;

        GtfsRealtime.FeedMessage feedMessage = null;


        try {
            InputStream in = _feedUrl.openStream();
            feedMessage = GtfsRealtime.FeedMessage.parseFrom(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Loop through the entities in the feed
        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasVehicle()) {
                vehicleCount++;
            }
            if (entity.hasAlert()) {
                alertCount++;
            }
            if (entity.hasTripUpdate()) {
                tripCount++;
            }
        }

        //System.out.println(vehicleCount + " " + tripCount + " " + alertCount);

        //Store details found to the database
        GTFSDB.setFeedDetails(_feedUrl.toString(), vehicleCount, tripCount, alertCount);

    }

    private int countEntities(GtfsRealtime.FeedMessage message, Descriptors.FieldDescriptor desc) {
        int count = 0;
        for (int i = 0; i < message.getEntityCount(); ++i) {

            GtfsRealtime.FeedEntity entity = message.getEntity(i);
            if (!entity.hasField(desc)) {
                continue;
            }
            count++;
        }
        return count;
    }


}