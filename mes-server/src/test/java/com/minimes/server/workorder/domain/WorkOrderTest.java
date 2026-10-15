package com.minimes.server.workorder.domain;

import com.minimes.common.equipment.EquipmentStatus;
import com.minimes.server.global.error.ErrorCode;
import com.minimes.server.global.error.MesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.minimes.server.support.Fixtures.admin;
import static com.minimes.server.support.Fixtures.at;
import static com.minimes.server.support.Fixtures.equipment;
import static com.minimes.server.support.Fixtures.startedWorkOrder;
import static com.minimes.server.support.Fixtures.workOrder;
import static com.minimes.server.support.Fixtures.worker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkOrderTest {

    @Test
    @DisplayName("새 작업지시는 WAITING 상태다")
    void newWorkOrderIsWaiting() {
        WorkOrder workOrder = workOrder(equipment(), 100);

        assertEquals(WorkOrderStatus.WAITING, workOrder.getStatus());
    }

    @Test
    @DisplayName("W4: 계획 수량이 1 미만이면 만들 수 없다")
    void plannedQtyMustBePositive() {
        MesException e = assertThrows(MesException.class, () -> workOrder(equipment(), 0));

        assertEquals(ErrorCode.INVALID_PLANNED_QTY, e.getErrorCode());
    }

    @Test
    @DisplayName("작업 시작: WAITING → IN_PROGRESS, 시작 시각 기록")
    void start() {
        WorkOrder workOrder = workOrder(equipment(EquipmentStatus.RUNNING), 100);

        workOrder.start(worker(), at(3));

        assertEquals(WorkOrderStatus.IN_PROGRESS, workOrder.getStatus());
        assertEquals(at(3), workOrder.getStartedAt());
    }

    @Test
    @DisplayName("설비가 IDLE이어도 작업을 시작할 수 있다")
    void startWhenEquipmentIdle() {
        WorkOrder workOrder = workOrder(equipment(EquipmentStatus.IDLE), 100);

        workOrder.start(worker(), at(0));

        assertTrue(workOrder.isInProgress());
    }

    @Test
    @DisplayName("W2: 설비가 DOWN이면 작업을 시작할 수 없다")
    void cannotStartWhenEquipmentDown() {
        WorkOrder workOrder = workOrder(equipment(EquipmentStatus.DOWN), 100);

        MesException e = assertThrows(MesException.class, () -> workOrder.start(worker(), at(0)));

        assertEquals(ErrorCode.EQUIPMENT_DOWN, e.getErrorCode());
        assertEquals(WorkOrderStatus.WAITING, workOrder.getStatus());
    }

    @Test
    @DisplayName("W3: 이미 진행 중인 작업지시는 다시 시작할 수 없다")
    void cannotStartTwice() {
        WorkOrder workOrder = startedWorkOrder();

        MesException e = assertThrows(MesException.class, () -> workOrder.start(worker(), at(1)));

        assertEquals(ErrorCode.INVALID_WORK_ORDER_TRANSITION, e.getErrorCode());
    }

    @Test
    @DisplayName("작업 완료: 완료 시각은 마지막 확정 실적 구간의 끝")
    void complete() {
        WorkOrder workOrder = startedWorkOrder();

        workOrder.complete(worker(), at(120));

        assertEquals(WorkOrderStatus.COMPLETED, workOrder.getStatus());
        assertEquals(at(120), workOrder.getCompletedAt());
    }

    @Test
    @DisplayName("P6: 확정된 실적이 없으면 완료할 수 없다")
    void cannotCompleteWithoutConfirmedResult() {
        WorkOrder workOrder = startedWorkOrder();

        MesException e = assertThrows(MesException.class, () -> workOrder.complete(worker(), null));

        assertEquals(ErrorCode.FINAL_RESULT_NOT_CONFIRMED, e.getErrorCode());
        assertTrue(workOrder.isInProgress());
    }

    @Test
    @DisplayName("W3: 시작하지 않은 작업지시는 완료할 수 없다")
    void cannotCompleteWaiting() {
        WorkOrder workOrder = workOrder(equipment(), 100);

        MesException e = assertThrows(MesException.class, () -> workOrder.complete(worker(), at(10)));

        assertEquals(ErrorCode.INVALID_WORK_ORDER_TRANSITION, e.getErrorCode());
    }

    @Test
    @DisplayName("WAITING, IN_PROGRESS 작업지시는 취소할 수 있다")
    void cancel() {
        WorkOrder waiting = workOrder(equipment(), 100);
        WorkOrder inProgress = startedWorkOrder();

        waiting.cancel(admin(), at(90));
        inProgress.cancel(admin(), at(90));

        assertEquals(WorkOrderStatus.CANCELED, waiting.getStatus());
        assertEquals(WorkOrderStatus.CANCELED, inProgress.getStatus());
    }

    @Test
    @DisplayName("W3: 종료 상태(COMPLETED, CANCELED)는 되돌릴 수 없다")
    void terminalStatesAreFinal() {
        WorkOrder completed = startedWorkOrder();
        completed.complete(worker(), at(60));
        WorkOrder canceled = workOrder(equipment(), 100);
        canceled.cancel(admin(), at(90));

        assertThrows(MesException.class, () -> completed.cancel(admin(), at(90)));
        assertThrows(MesException.class, () -> completed.start(worker(), at(70)));
        assertThrows(MesException.class, () -> canceled.cancel(admin(), at(90)));
        assertThrows(MesException.class, () -> canceled.start(worker(), at(70)));
        assertEquals(WorkOrderStatus.COMPLETED, completed.getStatus());
        assertEquals(WorkOrderStatus.CANCELED, canceled.getStatus());
    }

    @Test
    @DisplayName("P2: 첫 실적 구간은 작업 시작 시각부터, 이후 구간은 직전 구간의 끝부터")
    void nextPeriodStart() {
        WorkOrder workOrder = startedWorkOrder();   // at(0)에 시작

        assertEquals(at(0), workOrder.nextPeriodStart(null));
        assertEquals(at(240), workOrder.nextPeriodStart(at(240)));
    }

    @Test
    @DisplayName("P1: 진행 중이 아닌 작업지시에는 실적 구간을 만들 수 없다")
    void nextPeriodStartRequiresInProgress() {
        WorkOrder workOrder = workOrder(equipment(), 100);

        MesException e = assertThrows(MesException.class, () -> workOrder.nextPeriodStart(null));

        assertEquals(ErrorCode.WORK_ORDER_NOT_IN_PROGRESS, e.getErrorCode());
    }

    @Test
    @DisplayName("P8: 누적 양품이 계획 수량을 넘으면 초과 생산으로 판단한다")
    void overProduction() {
        WorkOrder workOrder = startedWorkOrder();   // 계획 100

        assertFalse(workOrder.isOverProduced(100));
        assertTrue(workOrder.isOverProduced(101));
    }
}
