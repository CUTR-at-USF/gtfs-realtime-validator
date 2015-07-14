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

package edu.usf.cutr.gtfsrtvalidator.prototype;

 import com.google.transit.realtime.GtfsRealtime;
import com.googlecode.protobuf.format.JsonFormat;

import java.io.FileInputStream;

 public class ProtoBuf {

    //Read profbuf in to JSON
    public static String protoToJSON(String path) {

        String workingDir = System.getProperty("user.dir");
        String tripUpdatePath = workingDir + "/target/classes/tripupdate";
        String json = "";

        try {
            FileInputStream fis = new FileInputStream(tripUpdatePath);
            GtfsRealtime.FeedMessage message = GtfsRealtime.FeedMessage.parseFrom(fis);

            json = JsonFormat.printToString(message);

            //System.out.println(message.toString());
            //json = message.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return json;
    }
}