package com.minimes.server.equipment.domain;

/**
 * 비가동 구분. OEE 가동률 계산 시 PLANNED는 계획 가동 시간에서 제외하고 UNPLANNED만 손실로 센다.
 */
public enum DowntimeCategory {
    PLANNED,
    UNPLANNED
}
