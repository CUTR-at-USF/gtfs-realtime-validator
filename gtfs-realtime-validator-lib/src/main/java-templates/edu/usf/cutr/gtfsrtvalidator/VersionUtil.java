/*
 * Copyright (C) 2018 University of South Florida (sjbarbeau@gmail.com)
 *
 * Licensed under the Apache License, VersionModel 2.0 (the "License");
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
package edu.usf.cutr.gtfsrtvalidator;

import edu.usf.cutr.gtfsrtvalidator.lib.model.VersionModel;

/**
 * Class used with the templating-maven-plugin to get Maven version numbers at runtime. See https://stackoverflow.com/a/36628755/937715
 */
public class VersionUtil {

    private static final String VERSION = "${project.version}";
    private static final String GROUPID = "${project.groupId}";
    private static final String ARTIFACTID = "${project.artifactId}";
    private static final String REVISION = "${buildNumber}";

    public static VersionModel getVersion() {
        return new VersionModel(VERSION, GROUPID, ARTIFACTID, REVISION);
    }
}