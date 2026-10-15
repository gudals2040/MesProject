package com.minimes.common.equipment;

/**
 * 설비 상태. 설비(시뮬레이터, 현장에서는 PLC·SCADA)가 보내는 상태 신호로만 바뀐다.
 */
public enum EquipmentStatus {

    /** 가동 중 */
    RUNNING,

    /** 가동 가능하지만 생산하지 않음 */
    IDLE,

    /** 고장·정지 */
    DOWN;

    public boolean isStopped() {
        return this != RUNNING;
    }
}
