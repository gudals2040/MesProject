package com.minimes.server.equipment.domain;

/**
 * 비가동 사유가 정해진 방식.
 * AUTO   : 시스템이 기록 (작업 없음 자동 분류, 또는 아직 미분류)
 * MANUAL : 작업자가 사유를 입력
 */
public enum DowntimeSource {
    AUTO,
    MANUAL
}
