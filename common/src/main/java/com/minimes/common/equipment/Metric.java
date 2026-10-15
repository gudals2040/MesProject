package com.minimes.common.equipment;

/**
 * 설비 측정 항목 (ADR-002: 센서 데이터는 metric + value 세로형으로 저장)
 */
public enum Metric {

    /** 온도 (℃) */
    TEMPERATURE,

    /** 진동 (mm/s) */
    VIBRATION,

    /** 생산 수량 카운트 (개). 실적의 counted_qty 집계 대상 */
    COUNT
}
