package com.agri.supplytracker.inspection.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InspectionJobWorker {
    private final InspectionJobService jobs;
    private final boolean enabled;

    public InspectionJobWorker(InspectionJobService jobs,
                               @Value("${inspection.worker.enabled:false}") boolean enabled) {
        this.jobs = jobs;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${inspection.worker.fixed-delay-ms:5000}")
    public void processQueuedJob() {
        if (enabled) jobs.processNextQueuedJob();
    }
}
