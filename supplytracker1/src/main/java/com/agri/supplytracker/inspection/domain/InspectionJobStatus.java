package com.agri.supplytracker.inspection.domain;

public enum InspectionJobStatus {
    SUBMITTED,
    QUEUED,
    PROCESSING,
    SUCCEEDED,
    REVIEW_REQUIRED,
    REVIEWED,
    FAILED
}
