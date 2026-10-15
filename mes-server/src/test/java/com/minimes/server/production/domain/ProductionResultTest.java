package com.minimes.server.production.domain;

import com.minimes.server.global.error.ErrorCode;
import com.minimes.server.global.error.MesException;
import com.minimes.server.workorder.domain.WorkOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.minimes.server.support.Fixtures.at;
import static com.minimes.server.support.Fixtures.defectType;
import static com.minimes.server.support.Fixtures.equipment;
import static com.minimes.server.support.Fixtures.startedWorkOrder;
import static com.minimes.server.support.Fixtures.workOrder;
import static com.minimes.server.support.Fixtures.worker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionResultTest {

    private final DefectType dimension = defectType("DIMENSION");
    private final DefectType crack = defectType("CRACK");

    @Test
    @DisplayName("첫 실적: 구간은 작업 시작 시각부터, 불량 수량은 불량 내역 합계")
    void confirmFirstResult() {
        WorkOrder workOrder = startedWorkOrder();   // at(0)에 시작

        ProductionResult result = ProductionResult.confirm(
                workOrder, null, at(240), 52, 48,
                List.of(new DefectLine(dimension, 3), new DefectLine(crack, 1)), worker(), at(241));

        assertEquals(at(0), result.getPeriodStart());
        assertEquals(at(240), result.getPeriodEnd());
        assertEquals(52, result.getCountedQty());
        assertEquals(48, result.getGoodQty());
        assertEquals(4, result.getDefectQty());       // D1: 3 + 1
        assertEquals(2, result.getDefects().size());
        assertEquals(at(241), result.getConfirmedAt());
    }

    @Test
    @DisplayName("P2: 두 번째 실적은 직전 구간의 끝에서 시작한다")
    void confirmNextResultContinuesPeriod() {
        WorkOrder workOrder = startedWorkOrder();

        ProductionResult second = ProductionResult.confirm(
                workOrder, at(240), at(480), 50, 50, List.of(), worker(), at(480));

        assertEquals(at(240), second.getPeriodStart());
        assertEquals(0, second.getDefectQty());
    }

    @Test
    @DisplayName("P1: 진행 중이 아닌 작업지시에는 실적을 확정할 수 없다")
    void requiresInProgressWorkOrder() {
        WorkOrder waiting = workOrder(equipment(), 100);

        MesException e = assertThrows(MesException.class, () -> ProductionResult.confirm(
                waiting, null, at(60), 10, 10, List.of(), worker(), at(60)));

        assertEquals(ErrorCode.WORK_ORDER_NOT_IN_PROGRESS, e.getErrorCode());
    }

    @Test
    @DisplayName("P3: 구간 끝이 시작보다 늦지 않으면 거부한다")
    void periodEndMustBeAfterStart() {
        WorkOrder workOrder = startedWorkOrder();

        MesException e = assertThrows(MesException.class, () -> ProductionResult.confirm(
                workOrder, at(240), at(240), 0, 0, List.of(), worker(), at(300)));

        assertEquals(ErrorCode.INVALID_RESULT_PERIOD, e.getErrorCode());
    }

    @Test
    @DisplayName("P3: 구간 끝이 현재 시각보다 미래면 거부한다")
    void periodEndCannotBeInFuture() {
        WorkOrder workOrder = startedWorkOrder();

        MesException e = assertThrows(MesException.class, () -> ProductionResult.confirm(
                workOrder, null, at(300), 10, 10, List.of(), worker(), at(299)));

        assertEquals(ErrorCode.INVALID_RESULT_PERIOD, e.getErrorCode());
    }

    @Test
    @DisplayName("P5: 양품 수량이 음수면 거부한다")
    void qtyCannotBeNegative() {
        WorkOrder workOrder = startedWorkOrder();

        MesException e = assertThrows(MesException.class, () -> ProductionResult.confirm(
                workOrder, null, at(60), 10, -1, List.of(), worker(), at(60)));

        assertEquals(ErrorCode.INVALID_RESULT_QTY, e.getErrorCode());
    }

    @Test
    @DisplayName("D2: 불량 내역 수량이 1 미만이면 거부한다")
    void defectQtyMustBePositive() {
        WorkOrder workOrder = startedWorkOrder();

        MesException e = assertThrows(MesException.class, () -> ProductionResult.confirm(
                workOrder, null, at(60), 10, 10, List.of(new DefectLine(dimension, 0)), worker(), at(60)));

        assertEquals(ErrorCode.INVALID_DEFECT_QTY, e.getErrorCode());
    }

    @Test
    @DisplayName("같은 불량 유형을 두 줄로 입력하면 거부한다")
    void duplicateDefectTypeRejected() {
        WorkOrder workOrder = startedWorkOrder();

        MesException e = assertThrows(MesException.class, () -> ProductionResult.confirm(
                workOrder, null, at(60), 10, 8,
                List.of(new DefectLine(dimension, 1), new DefectLine(dimension, 1)), worker(), at(60)));

        assertEquals(ErrorCode.DUPLICATE_DEFECT_TYPE, e.getErrorCode());
    }

    @Test
    @DisplayName("센서 집계 수량과 확정 수량(양품 + 불량)의 차이를 계산한다")
    void countDifference() {
        WorkOrder workOrder = startedWorkOrder();

        ProductionResult result = ProductionResult.confirm(
                workOrder, null, at(240), 55, 48, List.of(new DefectLine(crack, 4)), worker(), at(240));

        assertEquals(3, result.countDifference());   // 55 - (48 + 4)
    }
}
