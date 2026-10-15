package com.minimes.server.equipment.domain;

import com.minimes.common.equipment.EquipmentStatus;
import com.minimes.server.global.error.ErrorCode;
import com.minimes.server.global.error.MesException;
import com.minimes.server.workorder.domain.WorkOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.minimes.server.support.Fixtures.at;
import static com.minimes.server.support.Fixtures.breakdownReason;
import static com.minimes.server.support.Fixtures.equipment;
import static com.minimes.server.support.Fixtures.noOrderReason;
import static com.minimes.server.support.Fixtures.startedWorkOrder;
import static com.minimes.server.support.Fixtures.worker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DowntimeTest {

    @Test
    @DisplayName("작업이 없을 때 멈추면 사유를 '작업 없음'으로 자동 분류한다")
    void openWithoutWork() {
        DowntimeReason noOrder = noOrderReason();

        Downtime downtime = Downtime.openWithoutWork(equipment(), EquipmentStatus.IDLE, noOrder, at(0));

        assertTrue(downtime.isOpen());
        assertSame(noOrder, downtime.getReason());
        assertNull(downtime.getWorkOrder());
        assertEquals(DowntimeSource.AUTO, downtime.getSource());
        assertEquals(EquipmentStatus.IDLE, downtime.getEquipmentStatus());
    }

    @Test
    @DisplayName("T4: 작업이 없을 때 DOWN이 되면 고장일 수 있으므로 미분류로 연다")
    void openDownWithoutWorkIsUnclassified() {
        Downtime downtime = Downtime.openWithoutWork(equipment(), EquipmentStatus.DOWN, noOrderReason(), at(0));

        assertTrue(downtime.isOpen());
        assertTrue(downtime.isUnclassified());
        assertNull(downtime.getWorkOrder());
        assertEquals(DowntimeSource.AUTO, downtime.getSource());
        assertEquals(EquipmentStatus.DOWN, downtime.getEquipmentStatus());
    }

    @Test
    @DisplayName("작업 중에 멈추면 작업지시를 연결하고 사유는 미분류로 연다")
    void openDuringWork() {
        WorkOrder workOrder = startedWorkOrder();

        Downtime downtime = Downtime.openDuringWork(
                workOrder.getEquipment(), EquipmentStatus.DOWN, workOrder, at(30));

        assertSame(workOrder, downtime.getWorkOrder());
        assertTrue(downtime.isUnclassified());
        assertEquals(DowntimeSource.AUTO, downtime.getSource());
    }

    @Test
    @DisplayName("RUNNING 상태로는 비가동을 열 수 없다")
    void cannotOpenWithRunningStatus() {
        MesException e = assertThrows(MesException.class, () -> Downtime.openWithoutWork(
                equipment(), EquipmentStatus.RUNNING, noOrderReason(), at(0)));

        assertEquals(ErrorCode.INVALID_DOWNTIME_STATUS, e.getErrorCode());
    }

    @Test
    @DisplayName("비가동을 닫으면 종료 시각이 기록된다")
    void close() {
        Downtime downtime = Downtime.openWithoutWork(equipment(), EquipmentStatus.IDLE, noOrderReason(), at(0));

        downtime.close(at(15));

        assertFalse(downtime.isOpen());
        assertEquals(at(15), downtime.getEndedAt());
    }

    @Test
    @DisplayName("T2: 종료 시각이 시작보다 빠르면 거부한다")
    void cannotCloseBeforeStart() {
        Downtime downtime = Downtime.openWithoutWork(equipment(), EquipmentStatus.IDLE, noOrderReason(), at(10));

        MesException e = assertThrows(MesException.class, () -> downtime.close(at(9)));

        assertEquals(ErrorCode.INVALID_DOWNTIME_PERIOD, e.getErrorCode());
        assertTrue(downtime.isOpen());
    }

    @Test
    @DisplayName("이미 닫힌 비가동은 다시 닫을 수 없다")
    void cannotCloseTwice() {
        Downtime downtime = Downtime.openWithoutWork(equipment(), EquipmentStatus.IDLE, noOrderReason(), at(0));
        downtime.close(at(5));

        MesException e = assertThrows(MesException.class, () -> downtime.close(at(6)));

        assertEquals(ErrorCode.DOWNTIME_ALREADY_CLOSED, e.getErrorCode());
    }

    @Test
    @DisplayName("작업자가 사유를 입력하면 MANUAL로 바뀌고, 종료된 비가동에도 입력할 수 있다")
    void classify() {
        WorkOrder workOrder = startedWorkOrder();
        Downtime downtime = Downtime.openDuringWork(
                workOrder.getEquipment(), EquipmentStatus.DOWN, workOrder, at(30));
        downtime.close(at(50));
        DowntimeReason breakdown = breakdownReason();

        downtime.classify(breakdown, worker());

        assertSame(breakdown, downtime.getReason());
        assertEquals(DowntimeSource.MANUAL, downtime.getSource());
        assertFalse(breakdown.isPlanned());
    }

    @Test
    @DisplayName("사유 없이 분류할 수 없다")
    void classifyRequiresReason() {
        Downtime downtime = Downtime.openDuringWork(
                equipment(), EquipmentStatus.DOWN, startedWorkOrder(), at(30));

        MesException e = assertThrows(MesException.class, () -> downtime.classify(null, worker()));

        assertEquals(ErrorCode.DOWNTIME_REASON_REQUIRED, e.getErrorCode());
    }
}
