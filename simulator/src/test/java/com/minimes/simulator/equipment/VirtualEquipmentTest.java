package com.minimes.simulator.equipment;

import com.minimes.common.equipment.EquipmentStatus;
import com.minimes.common.equipment.Metric;
import com.minimes.common.message.SensorMessage;
import com.minimes.common.message.StatusMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualEquipmentTest {

    private static final Instant NOW = Instant.parse("2026-10-25T01:00:00Z");

    private static double valueOf(List<SensorMessage> readings, Metric metric) {
        return readings.stream().filter(r -> r.metric() == metric).findFirst().orElseThrow().value();
    }

    @Test
    @DisplayName("가동 중이면 온도·진동·생산 수량 3건을 보내고, 메시지마다 다른 message_id를 붙인다")
    void runningSendsThreeReadings() {
        VirtualEquipment equipment = new VirtualEquipment("EQ-01", EquipmentStatus.RUNNING);

        List<SensorMessage> readings = equipment.nextReadings(NOW, new Random(1), 0.0);

        assertEquals(3, readings.size());
        Set<String> ids = readings.stream().map(SensorMessage::messageId).collect(Collectors.toSet());
        assertEquals(3, ids.size());
        assertTrue(readings.stream().allMatch(r -> r.equipmentCode().equals("EQ-01") && r.measuredAt().equals(NOW)));
        double count = valueOf(readings, Metric.COUNT);
        assertTrue(count >= 1 && count <= 3);
    }

    @Test
    @DisplayName("P4: 생산 수량(COUNT)은 가동 중일 때만 보낸다")
    void stoppedSendsNoCount() {
        VirtualEquipment idle = new VirtualEquipment("EQ-01", EquipmentStatus.IDLE);
        VirtualEquipment down = new VirtualEquipment("EQ-02", EquipmentStatus.DOWN);

        List<SensorMessage> idleReadings = idle.nextReadings(NOW, new Random(1), 0.0);
        List<SensorMessage> downReadings = down.nextReadings(NOW, new Random(1), 0.0);

        assertEquals(2, idleReadings.size());
        assertEquals(2, downReadings.size());
        assertFalse(idleReadings.stream().anyMatch(r -> r.metric() == Metric.COUNT));
    }

    @Test
    @DisplayName("이상값 확률이 1이면 알람 기준(85℃, 6.0mm/s)을 넘는 값을 보낸다")
    void anomalyExceedsThresholds() {
        VirtualEquipment equipment = new VirtualEquipment("EQ-01", EquipmentStatus.RUNNING);

        List<SensorMessage> readings = equipment.nextReadings(NOW, new Random(1), 1.0);

        assertTrue(valueOf(readings, Metric.TEMPERATURE) > 85.0);
        assertTrue(valueOf(readings, Metric.VIBRATION) > 6.0);
    }

    @Test
    @DisplayName("이상값 확률이 0이면 오래 돌려도 알람 기준을 넘지 않는다")
    void normalStaysBelowThresholds() {
        VirtualEquipment equipment = new VirtualEquipment("EQ-01", EquipmentStatus.RUNNING);
        Random random = new Random(42);

        for (int i = 0; i < 500; i++) {
            List<SensorMessage> readings = equipment.nextReadings(NOW.plusSeconds(i * 5L), random, 0.0);

            assertTrue(valueOf(readings, Metric.TEMPERATURE) < 85.0);
            assertTrue(valueOf(readings, Metric.VIBRATION) < 6.0);
        }
    }

    @Test
    @DisplayName("멈추면 온도가 정지 온도 쪽으로 내려간다")
    void temperatureCoolsDownWhenStopped() {
        VirtualEquipment equipment = new VirtualEquipment("EQ-01", EquipmentStatus.RUNNING);
        Random random = new Random(7);
        equipment.nextStatus(NOW, random, new TransitionRule(1, 0, 0, 0, 0));

        double last = 0;
        for (int i = 0; i < 30; i++) {
            last = valueOf(equipment.nextReadings(NOW.plusSeconds(i * 5L), random, 0.0), Metric.TEMPERATURE);
        }

        assertTrue(last < 35.0);
    }

    @Test
    @DisplayName("전이 확률이 1이면 상태가 바뀌고, 바뀐 시각을 원천 시각으로 보낸다 (T3 판단 기준)")
    void statusChanges() {
        VirtualEquipment equipment = new VirtualEquipment("EQ-01", EquipmentStatus.RUNNING);

        Optional<StatusMessage> down = equipment.nextStatus(NOW, new Random(1), new TransitionRule(0, 1, 0, 0, 0));
        Optional<StatusMessage> running = equipment.nextStatus(NOW.plusSeconds(5), new Random(1),
                new TransitionRule(0, 0, 0, 0, 1));

        assertEquals(EquipmentStatus.DOWN, down.orElseThrow().status());
        assertEquals(NOW, down.orElseThrow().changedAt());
        assertEquals(EquipmentStatus.RUNNING, running.orElseThrow().status());
        assertEquals(EquipmentStatus.RUNNING, equipment.getStatus());
    }

    @Test
    @DisplayName("상태가 그대로면 상태 메시지를 보내지 않는다")
    void noMessageWithoutChange() {
        VirtualEquipment equipment = new VirtualEquipment("EQ-01", EquipmentStatus.IDLE);

        Optional<StatusMessage> message = equipment.nextStatus(NOW, new Random(1), TransitionRule.none());

        assertTrue(message.isEmpty());
        assertEquals(EquipmentStatus.IDLE, equipment.getStatus());
    }

    @Test
    @DisplayName("시작할 때 보내는 현재 상태 메시지에는 설비 코드와 현재 상태가 담긴다")
    void currentStatus() {
        VirtualEquipment equipment = new VirtualEquipment("EQ-03", EquipmentStatus.RUNNING);

        StatusMessage message = equipment.currentStatus(NOW);

        assertEquals("EQ-03", message.equipmentCode());
        assertEquals(EquipmentStatus.RUNNING, message.status());
        assertEquals(NOW, message.changedAt());
    }
}
