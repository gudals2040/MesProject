package com.minimes.server.member.domain;

import com.minimes.common.equipment.EquipmentStatus;
import com.minimes.server.alarm.domain.EquipmentAlarm;
import com.minimes.server.equipment.domain.Downtime;
import com.minimes.server.equipment.domain.DowntimeReason;
import com.minimes.server.global.error.ErrorCode;
import com.minimes.server.global.error.MesException;
import com.minimes.server.production.domain.ProductionResult;
import com.minimes.server.workorder.domain.WorkOrder;
import com.minimes.server.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static com.minimes.server.support.Fixtures.admin;
import static com.minimes.server.support.Fixtures.at;
import static com.minimes.server.support.Fixtures.breakdownReason;
import static com.minimes.server.support.Fixtures.equipment;
import static com.minimes.server.support.Fixtures.item;
import static com.minimes.server.support.Fixtures.noOrderReason;
import static com.minimes.server.support.Fixtures.startedWorkOrder;
import static com.minimes.server.support.Fixtures.workOrder;
import static com.minimes.server.support.Fixtures.worker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 역할 규칙 R1~R3 (ADR-010).
 *   R1: 작업지시 등록·취소, 알람 확인은 관리자만
 *   R2: 작업 시작·완료, 실적 확정, 비가동 사유 입력은 작업자만
 *       (T4: 작업 없이 멈춘 DOWN 비가동의 사유는 관리자도 입력)
 *   R3: 등록·확정·확인·사유 입력 시 처리자를 기록
 */
class RoleRuleTest {

    private static void assertForbidden(Executable action) {
        MesException e = assertThrows(MesException.class, action);
        assertEquals(ErrorCode.FORBIDDEN_ROLE, e.getErrorCode());
    }

    // ---------- R1: 관리자만 ----------

    @Test
    @DisplayName("R1: 작업자는 작업지시를 등록할 수 없다")
    void workerCannotCreateWorkOrder() {
        assertForbidden(() -> new WorkOrder("WO-002", item(), equipment(), 10, worker(), at(0)));
    }

    @Test
    @DisplayName("R1: 작업자는 작업지시를 취소할 수 없고, 상태도 바뀌지 않는다")
    void workerCannotCancelWorkOrder() {
        WorkOrder workOrder = workOrder(equipment(), 100);

        assertForbidden(() -> workOrder.cancel(worker(), at(90)));
        assertEquals(WorkOrderStatus.WAITING, workOrder.getStatus());
    }

    @Test
    @DisplayName("R1: 작업자는 알람을 확인할 수 없고, 알람은 미확인으로 남는다")
    void workerCannotAcknowledgeAlarm() {
        EquipmentAlarm alarm = new EquipmentAlarm(equipment(), null, "TEMP_HIGH", 210, 200, at(0));

        assertForbidden(() -> alarm.acknowledge(worker(), at(1)));
        assertTrue(alarm.isOpen());
        assertNull(alarm.getAcknowledgedBy());
    }

    // ---------- R2: 작업자만 ----------

    @Test
    @DisplayName("R2: 관리자는 작업을 시작할 수 없다")
    void adminCannotStartWorkOrder() {
        WorkOrder workOrder = workOrder(equipment(EquipmentStatus.RUNNING), 100);

        assertForbidden(() -> workOrder.start(admin(), at(0)));
        assertEquals(WorkOrderStatus.WAITING, workOrder.getStatus());
    }

    @Test
    @DisplayName("R2: 관리자는 작업을 완료할 수 없다")
    void adminCannotCompleteWorkOrder() {
        WorkOrder workOrder = startedWorkOrder();

        assertForbidden(() -> workOrder.complete(admin(), at(60)));
        assertTrue(workOrder.isInProgress());
    }

    @Test
    @DisplayName("R2: 관리자는 실적을 확정할 수 없다")
    void adminCannotConfirmResult() {
        WorkOrder workOrder = startedWorkOrder();

        assertForbidden(() -> ProductionResult.confirm(
                workOrder, null, at(60), 10, 10, List.of(), admin(), at(60)));
    }

    @Test
    @DisplayName("R2: 관리자는 비가동 사유를 입력할 수 없고, 미분류로 남는다")
    void adminCannotClassifyDowntime() {
        WorkOrder workOrder = startedWorkOrder();
        Downtime downtime = Downtime.openDuringWork(
                workOrder.getEquipment(), EquipmentStatus.DOWN, workOrder, at(30));

        assertForbidden(() -> downtime.classify(breakdownReason(), admin()));
        assertTrue(downtime.isUnclassified());
    }

    @Test
    @DisplayName("T4: 작업 없이 멈춘 DOWN 비가동은 관리자도 사유를 입력할 수 있다")
    void adminCanClassifyDownWithoutWork() {
        Member admin = admin();
        Downtime downtime = Downtime.openWithoutWork(equipment(), EquipmentStatus.DOWN, noOrderReason(), at(0));

        downtime.classify(breakdownReason(), admin);

        assertSame(admin, downtime.getClassifiedBy());
    }

    @Test
    @DisplayName("T4: 작업 없이 멈춘 DOWN 비가동은 작업자도 사유를 입력할 수 있다")
    void workerCanClassifyDownWithoutWork() {
        Member worker = worker();
        Downtime downtime = Downtime.openWithoutWork(equipment(), EquipmentStatus.DOWN, noOrderReason(), at(0));

        downtime.classify(breakdownReason(), worker);

        assertSame(worker, downtime.getClassifiedBy());
    }

    @Test
    @DisplayName("처리자가 없으면 MEMBER_REQUIRED")
    void memberRequired() {
        WorkOrder workOrder = workOrder(equipment(EquipmentStatus.RUNNING), 100);

        MesException e = assertThrows(MesException.class, () -> workOrder.start(null, at(0)));

        assertEquals(ErrorCode.MEMBER_REQUIRED, e.getErrorCode());
    }

    // ---------- R3: 처리자 기록 ----------

    @Test
    @DisplayName("R3: 작업지시에 등록한 관리자가 기록된다")
    void workOrderRecordsCreator() {
        Member admin = admin();

        WorkOrder workOrder = new WorkOrder("WO-002", item(), equipment(), 10, admin, at(0));

        assertSame(admin, workOrder.getCreatedBy());
    }

    @Test
    @DisplayName("R3: 작업을 시작·완료한 작업자가 각각 기록된다")
    void workOrderRecordsStarterAndCompleter() {
        Member starter = worker();
        Member completer = new Member("작업자2", MemberRole.WORKER, at(0));
        WorkOrder workOrder = workOrder(equipment(EquipmentStatus.RUNNING), 100);

        workOrder.start(starter, at(0));
        workOrder.complete(completer, at(480));

        assertSame(starter, workOrder.getStartedBy());
        assertSame(completer, workOrder.getCompletedBy());
        assertNull(workOrder.getCanceledBy());
    }

    @Test
    @DisplayName("R3: 작업지시를 취소한 관리자와 취소 시각이 기록된다")
    void workOrderRecordsCanceler() {
        Member admin = admin();
        WorkOrder workOrder = startedWorkOrder();

        workOrder.cancel(admin, at(30));

        assertSame(admin, workOrder.getCanceledBy());
        assertEquals(at(30), workOrder.getCanceledAt());
        assertNull(workOrder.getCompletedBy());
    }

    @Test
    @DisplayName("R3: 역할이 틀려 거부되면 처리자가 기록되지 않는다")
    void rejectedActionLeavesNoActor() {
        WorkOrder workOrder = workOrder(equipment(EquipmentStatus.RUNNING), 100);

        assertForbidden(() -> workOrder.start(admin(), at(0)));

        assertNull(workOrder.getStartedBy());
        assertNull(workOrder.getStartedAt());
    }

    @Test
    @DisplayName("R3: 실적에 확정한 작업자가 기록된다")
    void resultRecordsConfirmer() {
        Member worker = worker();

        ProductionResult result = ProductionResult.confirm(
                startedWorkOrder(), null, at(60), 10, 10, List.of(), worker, at(60));

        assertSame(worker, result.getConfirmedBy());
    }

    @Test
    @DisplayName("R3: 알람을 확인하면 확인 시각과 확인자가 함께 기록된다")
    void alarmRecordsAcknowledger() {
        Member admin = admin();
        EquipmentAlarm alarm = new EquipmentAlarm(equipment(), null, "TEMP_HIGH", 210, 200, at(0));

        alarm.acknowledge(admin, at(5));

        assertEquals(at(5), alarm.getAcknowledgedAt());
        assertSame(admin, alarm.getAcknowledgedBy());
    }

    @Test
    @DisplayName("R3: 작업자가 사유를 입력하면 입력자가 기록되고, 자동 분류는 처리자가 없다")
    void downtimeRecordsClassifier() {
        Member worker = worker();
        WorkOrder workOrder = startedWorkOrder();
        Downtime manual = Downtime.openDuringWork(
                workOrder.getEquipment(), EquipmentStatus.DOWN, workOrder, at(30));
        DowntimeReason noOrder = noOrderReason();
        Downtime automatic = Downtime.openWithoutWork(equipment(), EquipmentStatus.IDLE, noOrder, at(0));

        manual.classify(breakdownReason(), worker);

        assertSame(worker, manual.getClassifiedBy());
        assertSame(noOrder, automatic.getReason());
        assertNull(automatic.getClassifiedBy());
    }
}
