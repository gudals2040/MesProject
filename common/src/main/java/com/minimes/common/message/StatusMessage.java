package com.minimes.common.message;

import com.minimes.common.equipment.EquipmentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 설비 → MES 상태 변경 메시지.
 *
 * <p>상태 메시지는 따로 저장하지 않는다. 같은 메시지가 다시 오거나 늦게 와도 {@code changedAt}이
 * 설비의 마지막 상태 시각보다 이르면 무시하고, 같은 상태면 전이가 일어나지 않으므로
 * 결과가 달라지지 않는다 (규칙 T3, ADR-011). 상태 구간 이력은 downtime이 남긴다.
 *
 * @param messageId     시뮬레이터가 만든 UUID. 저장하지 않고 로그 추적용으로 쓴다
 * @param equipmentCode 설비 코드(EQ-01)
 * @param status        바뀐 상태
 * @param changedAt     설비에서 상태가 바뀐 시각(원천 시각). T3 판단 기준
 */
public record StatusMessage(
        String messageId,
        String equipmentCode,
        EquipmentStatus status,
        Instant changedAt
) {

    public static StatusMessage of(String equipmentCode, EquipmentStatus status, Instant changedAt) {
        return new StatusMessage(UUID.randomUUID().toString(), equipmentCode, status, changedAt);
    }
}
