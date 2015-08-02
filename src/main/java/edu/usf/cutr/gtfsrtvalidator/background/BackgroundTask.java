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

import com.google.transit.realtime.GtfsRealtime;
import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsFeedIterationModel;
import edu.usf.cutr.gtfsrtvalidator.api.model.GtfsRtFeedModel;
import edu.usf.cutr.gtfsrtvalidator.db.GTFSDB;
import edu.usf.cutr.gtfsrtvalidator.helper.TimeStampHelper;
import edu.usf.cutr.gtfsrtvalidator.validation.HeaderValidation;
import edu.usf.cutr.gtfsrtvalidator.validation.EntityValidation;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class BackgroundTask implements Runnable {

    private GtfsRtFeedModel currentFeed = null;

    public BackgroundTask(String url) {
        GtfsRtFeedModel searchFeed = new GtfsRtFeedModel();
        searchFeed.setGtfsUrl(url);
        currentFeed = GTFSDB.getGtfsRtFeed(searchFeed);
    }

    @Override
    public void run() {
        URL _feedUrl = null;
        System.out.println(currentFeed.getGtfsUrl());

        try {
            _feedUrl = new URL(currentFeed.getGtfsUrl());
            System.out.println(_feedUrl.toString());
        } catch (Exception e) {
            System.out.println("Malformed Url");
            e.printStackTrace();
        }

        GtfsRealtime.FeedMessage feedMessage = null;
        byte[] gtfsRtProtobuf = null;

        try {
            assert _feedUrl != null;
            InputStream in = _feedUrl.openStream();
            gtfsRtProtobuf = IOUtils.toByteArray(in);
            InputStream is = new ByteArrayInputStream(gtfsRtProtobuf);
            feedMessage = GtfsRealtime.FeedMessage.parseFrom(is);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        //get the header of the feed
        assert feedMessage != null;
        GtfsRealtime.FeedHeader header = feedMessage.getHeader();

        //validation rules for all headers
        HeaderValidation.validate(header);

        List<GtfsRealtime.FeedEntity> entityList = feedMessage.getEntityList();

        EntityValidation entityValidation = new EntityValidation();
        entityValidation.validate(entityList);

        GtfsFeedIterationModel feedIteration = new GtfsFeedIterationModel();
        feedIteration.setFeedprotobuf(gtfsRtProtobuf);
        feedIteration.setTimeStamp(TimeStampHelper.getCurrentTimestamp());
        feedIteration.setRtFeedId(currentFeed.getGtfsRtId());
        GTFSDB.setRtFeedInfo(feedIteration);
    }
}