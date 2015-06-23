/*
 ***********************************************************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***********************************************************************************************************************
*/

package edu.usf.cutr.gtfsrtvalidator.background;

import com.google.protobuf.Descriptors;
import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.db.Database;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class RefreshCountTask implements Runnable {

    public static boolean urlRecived = true;

    public URL _tripUpdatesUrl;
    public URL _vehiclePositionsUrl;

    public RefreshCountTask(){
        try {
            //TODO: URLs should be taken from the users input
            _vehiclePositionsUrl = new URL("http://developer.mbta.com/lib/GTRTFS/Alerts/VehiclePositions.pb");
            _tripUpdatesUrl = new URL("http://developer.mbta.com/lib/GTRTFS/Alerts/TripUpdates.pb");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public RefreshCountTask(String url){
        try {
            _vehiclePositionsUrl = new URL(url);
        } catch (MalformedURLException e) {
        }
    }

    @Override
    public void run() {
        try {
            if (urlRecived) {
                //fetch();
                System.out.println(_vehiclePositionsUrl);

            }

        } catch (Exception ex) {
            System.out.println("Error here");
        }
    }

    public void fetch() throws IOException {

        int vehicleCount = 0, tripCount = 0;

        if (_tripUpdatesUrl != null) {
            InputStream in = _tripUpdatesUrl.openStream();
            GtfsRealtime.FeedMessage message = GtfsRealtime.FeedMessage.parseFrom(in);
            Descriptors.FieldDescriptor fieldDesc = GtfsRealtime.FeedEntity.getDescriptor().findFieldByName(
                    "trip_update");
            int count = countEntities(message, fieldDesc);
            System.out.println("trips.value " + count);
            tripCount = count;
        }
        if (_vehiclePositionsUrl != null) {
            InputStream in = _vehiclePositionsUrl.openStream();
            GtfsRealtime.FeedMessage message = GtfsRealtime.FeedMessage.parseFrom(in);
            Descriptors.FieldDescriptor fieldDesc = GtfsRealtime.FeedEntity.getDescriptor().findFieldByName(
                    "vehicle");
            int count = countEntities(message, fieldDesc);
            System.out.println("vehicles.value " + count);
            vehicleCount = count;
        }

        Database.setCount(vehicleCount, tripCount);
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