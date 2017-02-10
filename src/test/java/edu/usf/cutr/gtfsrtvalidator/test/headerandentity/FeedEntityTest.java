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
package edu.usf.cutr.gtfsrtvalidator.test.headerandentity;

import edu.usf.cutr.gtfsrtvalidator.test.feeds.AlertFeedTest;
import edu.usf.cutr.gtfsrtvalidator.test.feeds.TripUpdateFeedTest;
import edu.usf.cutr.gtfsrtvalidator.test.feeds.VehiclePositionFeedTest;
import edu.usf.cutr.gtfsrtvalidator.test.feeds.combined.TripUpdateVehiclePositionTest;
import edu.usf.cutr.gtfsrtvalidator.test.feeds.combined.TripUpdateVehiclePostionAlertTest;

/*
 * contains TripUpdate, VehiclePosiiton and Alert feed tests. Also contains tests on combination of feeds.
*/
public class FeedEntityTest {

    public FeedEntityTest() throws Exception {}
    
    // Initialize all the classes that contains tests on feeds or combination of feeds.
    TripUpdateFeedTest tripUpdateFeedTest = new TripUpdateFeedTest();
    VehiclePositionFeedTest vehiclePositionFeedTest = new VehiclePositionFeedTest();
    AlertFeedTest alertFeedTest = new AlertFeedTest();
    
    TripUpdateVehiclePositionTest tripUpdateVehiclePositionTest = new TripUpdateVehiclePositionTest();
    TripUpdateVehiclePostionAlertTest tripUpdateVehiclePostionAlert = new TripUpdateVehiclePostionAlertTest();    
}
