package com.minimes.common.message;

import com.minimes.common.equipment.Metric;

import java.time.Instant;
import java.util.UUID;

/**
 * 설비 → MES 센서 측정 메시지. 측정값 1건이 메시지 1건이다 (ADR-002 세로형 저장과 1:1).
 *
 * @param messageId     시뮬레이터(현장에서는 수집 게이트웨이)가 만든 UUID. 재전송해도 그대로 유지해
 *                      서버가 중복 수신을 구분한다 (ADR-003, 규칙 S1)
 * @param equipmentCode 설비 코드(EQ-01). 현장 설비는 MES 내부 id를 모르므로 업무 키로 보낸다.
 *                      서버가 equipment.code로 찾고, 없으면 거부한다 (ADR-011, 규칙 S2)
 * @param metric        측정 항목
 * @param value         측정값
 * @param measuredAt    설비에서 측정한 시각(원천 시각). 서버 수신 시각(received_at)은 서버가 따로 기록한다
 */
public record SensorMessage(
        String messageId,
        String equipmentCode,
        Metric metric,
        double value,
        Instant measuredAt
) {

    /** 새 측정값을 보낼 때 사용한다. 재전송은 만든 객체를 그대로 다시 보낸다. */
    public static SensorMessage of(String equipmentCode, Metric metric, double value, Instant measuredAt) {
        return new SensorMessage(UUID.randomUUID().toString(), equipmentCode, metric, value, measuredAt);
    }
}
