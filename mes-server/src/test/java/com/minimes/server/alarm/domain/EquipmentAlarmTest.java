package com.minimes.server.alarm.domain;

import com.minimes.server.global.error.ErrorCode;
import com.minimes.server.global.error.MesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.minimes.server.support.Fixtures.admin;
import static com.minimes.server.support.Fixtures.at;
import static com.minimes.server.support.Fixtures.equipment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentAlarmTest {

    @Test
    @DisplayName("새 알람은 미확인 상태이고, 확인하면 확인 시각이 기록된다")
    void acknowledge() {
        EquipmentAlarm alarm = new EquipmentAlarm(equipment(), null, "TEMP_HIGH", 210, 200, at(0));
        assertTrue(alarm.isOpen());

        alarm.acknowledge(admin(), at(3));

        assertFalse(alarm.isOpen());
        assertEquals(at(3), alarm.getAcknowledgedAt());
    }

    @Test
    @DisplayName("이미 확인한 알람은 다시 확인할 수 없다")
    void cannotAcknowledgeTwice() {
        EquipmentAlarm alarm = new EquipmentAlarm(equipment(), null, "TEMP_HIGH", 210, 200, at(0));
        alarm.acknowledge(admin(), at(1));

        MesException e = assertThrows(MesException.class, () -> alarm.acknowledge(admin(), at(2)));

        assertEquals(ErrorCode.ALARM_ALREADY_ACKNOWLEDGED, e.getErrorCode());
    }

    @Test
    @DisplayName("서버 시계가 늦어도 확인 시각은 발생 시각보다 앞서지 않는다 (DB CHECK와 일치)")
    void acknowledgedAtNotBeforeOccurredAt() {
        EquipmentAlarm alarm = new EquipmentAlarm(equipment(), null, "TEMP_HIGH", 210, 200, at(10));

        alarm.acknowledge(admin(), at(9));

        assertEquals(at(10), alarm.getAcknowledgedAt());
    }
}
