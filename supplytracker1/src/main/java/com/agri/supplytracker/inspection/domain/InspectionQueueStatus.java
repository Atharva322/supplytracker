package com.agri.supplytracker.inspection.domain;

public enum InspectionQueueStatus {
    READY,
    IN_FLIGHT,
    RETRY,
    ACKED,
    DLQ
}
