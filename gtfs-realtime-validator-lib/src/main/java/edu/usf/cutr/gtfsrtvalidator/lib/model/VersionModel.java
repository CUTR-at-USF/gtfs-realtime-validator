/*
 * Copyright (C) 2018 University of South Florida (sjbarbeau@gmail.com)
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
package edu.usf.cutr.gtfsrtvalidator.lib.model;

/**
 * A model class to hold the information generated at build time in VersionUtil. See https://stackoverflow.com/a/36628755/937715
 */
public class VersionModel {

    private String mVersion;
    private String mGroupId;
    private String mArtifactId;
    private String mRevision;

    public VersionModel() {

    }

    public VersionModel(String version, String groupId, String artifactId, String revision) {
        mVersion = version;
        mGroupId = groupId;
        mArtifactId = artifactId;
        mRevision = revision;
    }

    public String getVersion() {
        return mVersion;
    }

    public String getGroupId() {
        return mGroupId;
    }

    public String getArtifactId() {
        return mArtifactId;
    }

    public String getRevision() {
        return mRevision;
    }

    public void setVersion(String version) {
        this.mVersion = version;
    }

    public void setGroupId(String groupId) {
        this.mGroupId = mGroupId;
    }

    public void setArtifactId(String artifactId) {
        this.mArtifactId = mArtifactId;
    }

    public void setRevision(String revision) {
        this.mRevision = revision;
    }

    @Override
    public String toString() {
        return "VersionModel{" +
                "version='" + mVersion + '\'' +
                ", groupId='" + mGroupId + '\'' +
                ", artifactId='" + mArtifactId + '\'' +
                ", revision='" + mRevision + '\'' +
                '}';
    }
}
