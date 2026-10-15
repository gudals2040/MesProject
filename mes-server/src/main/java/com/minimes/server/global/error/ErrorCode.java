package com.minimes.server.global.error;

/**
 * 도메인 규칙 위반 코드. 괄호 안 번호는 Notion "상태 전이 · 비즈니스 규칙" 문서의 규칙 번호.
 */
public enum ErrorCode {

    // 사용자·역할
    MEMBER_REQUIRED("처리자를 식별할 수 없습니다. (R3)"),
    FORBIDDEN_ROLE("이 작업을 할 수 있는 역할이 아닙니다. (R1, R2)"),

    // 작업지시
    INVALID_PLANNED_QTY("계획 수량은 1 이상이어야 합니다. (W4)"),
    INVALID_WORK_ORDER_TRANSITION("허용되지 않는 작업지시 상태 전이입니다. (W3)"),
    EQUIPMENT_DOWN("설비가 정지(DOWN) 상태라 작업을 시작할 수 없습니다. (W2)"),
    WORK_ORDER_NOT_IN_PROGRESS("진행 중인 작업지시에만 실적을 등록할 수 있습니다. (P1)"),
    FINAL_RESULT_NOT_CONFIRMED("마지막 구간 실적을 확정해야 작업을 완료할 수 있습니다. (P6)"),

    // 생산 실적·불량
    INVALID_RESULT_PERIOD("실적 구간이 올바르지 않습니다. 구간 끝은 시작 이후이고 현재 시각을 넘을 수 없습니다. (P3)"),
    INVALID_RESULT_QTY("실적 수량은 0 이상이어야 합니다. (P5)"),
    INVALID_DEFECT_QTY("불량 내역 수량은 1 이상이어야 합니다. (D2)"),
    DUPLICATE_DEFECT_TYPE("같은 불량 유형은 한 줄로 입력해야 합니다."),

    // 비가동
    INVALID_DOWNTIME_STATUS("비가동은 IDLE 또는 DOWN 상태에서만 열 수 있습니다."),
    DOWNTIME_ALREADY_CLOSED("이미 종료된 비가동입니다."),
    INVALID_DOWNTIME_PERIOD("비가동 종료 시각은 시작 시각 이후여야 합니다. (T2)"),
    DOWNTIME_REASON_REQUIRED("비가동 사유를 선택해야 합니다."),

    // 알람
    ALARM_ALREADY_ACKNOWLEDGED("이미 확인된 알람입니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
