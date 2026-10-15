package com.minimes.server.equipment.domain;

import com.minimes.common.equipment.EquipmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.minimes.server.support.Fixtures.at;
import static com.minimes.server.support.Fixtures.equipment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentTest {

    @Test
    @DisplayName("새 설비는 IDLE 상태로 등록된다")
    void newEquipmentIsIdle() {
        Equipment equipment = equipment();

        assertEquals(EquipmentStatus.IDLE, equipment.getStatus());
    }

    @Test
    @DisplayName("상태 신호가 오면 상태와 변경 시각이 바뀌고 전이 정보를 돌려준다")
    void changeStatus() {
        Equipment equipment = equipment();

        Optional<StatusTransition> transition = equipment.changeStatus(EquipmentStatus.RUNNING, at(5));

        assertTrue(transition.isPresent());
        assertEquals(EquipmentStatus.IDLE, transition.get().from());
        assertEquals(EquipmentStatus.RUNNING, transition.get().to());
        assertEquals(EquipmentStatus.RUNNING, equipment.getStatus());
        assertEquals(at(5), equipment.getStatusChangedAt());
    }

    @Test
    @DisplayName("T3: 마지막 상태 시각보다 오래된 상태 메시지는 무시한다")
    void ignoreStaleStatusMessage() {
        Equipment equipment = equipment();
        equipment.changeStatus(EquipmentStatus.RUNNING, at(10));

        Optional<StatusTransition> transition = equipment.changeStatus(EquipmentStatus.DOWN, at(9));

        assertFalse(transition.isPresent());
        assertEquals(EquipmentStatus.RUNNING, equipment.getStatus());
        assertEquals(at(10), equipment.getStatusChangedAt());
    }

    @Test
    @DisplayName("같은 상태 신호가 반복되면 전이가 일어나지 않는다")
    void sameStatusIsNotTransition() {
        Equipment equipment = equipment();
        equipment.changeStatus(EquipmentStatus.RUNNING, at(1));

        Optional<StatusTransition> transition = equipment.changeStatus(EquipmentStatus.RUNNING, at(2));

        assertFalse(transition.isPresent());
        assertEquals(at(1), equipment.getStatusChangedAt());
    }

    @Test
    @DisplayName("전이 종류: RUNNING→정지는 stopped, 정지→RUNNING은 resumed, IDLE↔DOWN은 stopKindChanged")
    void transitionKinds() {
        StatusTransition stopped = new StatusTransition(EquipmentStatus.RUNNING, EquipmentStatus.DOWN, at(0));
        StatusTransition resumed = new StatusTransition(EquipmentStatus.IDLE, EquipmentStatus.RUNNING, at(0));
        StatusTransition switched = new StatusTransition(EquipmentStatus.IDLE, EquipmentStatus.DOWN, at(0));

        assertTrue(stopped.stopped());
        assertFalse(stopped.resumed());
        assertFalse(stopped.stopKindChanged());

        assertTrue(resumed.resumed());
        assertFalse(resumed.stopped());

        assertTrue(switched.stopKindChanged());
        assertFalse(switched.stopped());
        assertFalse(switched.resumed());
    }
}
