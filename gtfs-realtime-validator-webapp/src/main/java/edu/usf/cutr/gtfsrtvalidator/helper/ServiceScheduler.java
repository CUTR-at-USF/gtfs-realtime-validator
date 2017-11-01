package edu.usf.cutr.gtfsrtvalidator.helper;

import java.util.concurrent.ScheduledExecutorService;

public class ServiceScheduler {
    ScheduledExecutorService scheduler;
    Integer updateInterval;
    Integer parallelClientCount;

    public Integer getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(Integer updateInterval) {
        this.updateInterval = updateInterval;
    }

    public Integer getParallelClientCount() {
        return parallelClientCount;
    }

    public void setParallelClientCount(Integer parallelClientCount) {
        this.parallelClientCount = parallelClientCount;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public void setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }
}
