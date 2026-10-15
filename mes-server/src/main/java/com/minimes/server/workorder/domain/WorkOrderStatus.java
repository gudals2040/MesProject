package com.minimes.server.workorder.domain;

public enum WorkOrderStatus {

    WAITING,
    IN_PROGRESS,
    COMPLETED,
    CANCELED;

    /** 종료 상태(COMPLETED, CANCELED)는 다른 상태로 되돌릴 수 없다 (W3) */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELED;
    }
}
