/*
 * Copyright (C) 2017 University of South Florida
 *
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
package edu.usf.cutr.gtfsrtvalidator.lib.validation;

import java.util.List;

/**
 * A container class that holds statistics about a particular validation iteration
 */
public class IterationStatistics {

    private double gtfsReadTime;
    private double toByteArrayTime;
    private double decodeProtobufTime;
    private double mTotalIterationTime;
    private List<RuleStatistics> mRuleStatistics;

    public IterationStatistics() {
    }

    /**
     * Returns the amount of time it took to read the GTFS data, in seconds as a decimal (0.22).  Note that GTFS is only read once for each batch process execution, but it is saved to each IterationStatistics object for convenience.
     *
     * @return the amount of time it took to read the GTFS data, in seconds as a decimal (0.22).  Note that GTFS is only read once for each batch process execution, but it is saved to each IterationStatistics object for convenience.
     */
    public double getGtfsReadTime() {
        return gtfsReadTime;
    }

    /**
     * Sets amount of time it took to read the GTFS data, in seconds as a decimal (0.22). Note that GTFS is only read once for each batch process execution, but it is saved to each IterationStatistics object for convenience.
     *
     * @param gtfsReadTime the amount of time it took to read the GTFS data, in seconds as a decimal (0.22).  Note that GTFS is only read once for each batch process execution, but it is saved to each IterationStatistics object for convenience.
     */
    public void setGtfsReadTime(double gtfsReadTime) {
        this.gtfsReadTime = gtfsReadTime;
    }

    /**
     * Returns the amount of time it took to read the GTFS-realtime file into a byte array, in seconds as a decimal (0.22)
     *
     * @return the amount of time it took to read the GTFS-realtime file into a byte array, in seconds as a decimal (0.22)
     */
    public double getToByteArrayTime() {
        return toByteArrayTime;
    }

    /**
     * Sets the amount of time it took to read the GTFS-realtime file into a byte array, in seconds as a decimal (0.22)
     *
     * @param toByteArrayTime the amount of time it took to read the GTFS-realtime file into a byte array, in seconds as a decimal (0.22)
     */
    public void setToByteArrayTime(double toByteArrayTime) {
        this.toByteArrayTime = toByteArrayTime;
    }

    /**
     * Returns the amount of time it took to decode the protocol buffer into a FeedMessage Java object, in seconds as a decimal (0.22)
     *
     * @return the amount of time it took to decode the protocol buffer into a FeedMessage Java object, in seconds as a decimal (0.22)
     */
    public double getDecodeProtobufTime() {
        return decodeProtobufTime;
    }

    /**
     * Sets the amount of time it took to decode the protocol buffer into a FeedMessage Java object, in seconds as a decimal (0.22)
     *
     * @param decodeProtobufTime the amount of time it took to decode the protocol buffer into a FeedMessage Java object, in seconds as a decimal (0.22)
     */
    public void setDecodeProtobufTime(double decodeProtobufTime) {
        this.decodeProtobufTime = decodeProtobufTime;
    }

    /**
     * Returns the amount of time taken to process an entire GTFS-realtime validation interation.  This includes the times benchmarked for all other operations within this iteration.
     *
     * @return the amount of time taken to process an entire GTFS-realtime validation interation.  This includes the times benchmarked for all other operations within this iteration.
     */
    public double getTotalIterationTime() {
        return mTotalIterationTime;
    }

    /**
     * Sets the amount of time taken to process an entire GTFS-realtime validation interation.  This includes the times benchmarked for all other operations within this iteration.
     *
     * @param allRuleTime the amount of time taken to process an entire GTFS-realtime validation interation.  This includes the times benchmarked for all other operations within this iteration.
     */
    public void setTotalIterationTime(double allRuleTime) {
        this.mTotalIterationTime = allRuleTime;
    }

    /**
     * Returns the statistics for the execution of rules, where one element in the list represents a single rule execution
     *
     * @return the statistics for the execution of rules, where one element in the list represents a single rule execution
     */
    public List<RuleStatistics> getRuleStatistics() {
        return mRuleStatistics;
    }

    /**
     * Sets the statistics for the execution of rules, where one element in the list represents a single rule execution
     *
     * @param ruleStatistics the statistics for the execution of rules, where one element in the list represents a single rule execution
     */
    public void setRuleStatistics(List<RuleStatistics> ruleStatistics) {
        this.mRuleStatistics = ruleStatistics;
    }

    @Override
    public String toString() {
        return "IterationStatistics{" +
                "gtfsReadTime=" + gtfsReadTime +
                ", toByteArrayTime=" + toByteArrayTime +
                ", decodeProtobufTime=" + decodeProtobufTime +
                ", mTotalIterationTime=" + mTotalIterationTime +
                ", mRuleStatistics=" + mRuleStatistics +
                '}';
    }
}
