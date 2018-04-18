/*
 * Copyright (C) 2011-2018 Nipuna Gunathilake, University of South Florida (sjbarbeau@gmail.com)
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
package edu.usf.cutr.gtfsrtvalidator.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Utility class to get the file location for the GTFS (schedule data) validation output.
 */
public class FileUtil {

    public static final String GTFS_VALIDATOR_OUTPUT_FILE_PATH = "classes" + File.separator + "webroot";

    /**
     * Returns the JAR location on disk
     * @param o any instantiated class
     * @return the JAR location on disk
     */
    public static File getJarLocation(Object o) {
        URL jarLocation = o.getClass().getProtectionDomain().getCodeSource().getLocation();
        File f = null;
        try {
            f = new File(jarLocation.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return f;
    }

    /**
     * Returns the file name for a provided GTFS data URL, or null if one couldn't be created
     * @param url URL of the GTFS zip file
     * @return the file name for a provided GTFS data URL, or null if one couldn't be created
     */
    public static String getGtfsFileName(String url) {
        String fileName = null;
        try {
            fileName = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return fileName;
    }

    /**
     * Returns a reference to the file for the GTFS schedule validation output
     * @param o any instantiated class, used to get the JAR file location
     * @param gtfsFileName the file name for a GTFS zip file (can be derived from URL using getGtfsFileName())
     * @return a reference to the file for the GTFS schedule validation output
     */
    public static File getGtfsValidationOutputFile(Object o, String gtfsFileName) {
        String saveDir = FileUtil.getJarLocation(o).getParentFile().getAbsolutePath();
        String validationFileName = saveDir + File.separator + GTFS_VALIDATOR_OUTPUT_FILE_PATH + File.separator + gtfsFileName + "_out.json";
        return new File(validationFileName);
    }
}
