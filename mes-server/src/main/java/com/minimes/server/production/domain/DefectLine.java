package com.minimes.server.production.domain;

/**
 * 실적 확정 시 작업자가 입력하는 불량 한 줄 (불량 유형 + 수량).
 */
public record DefectLine(DefectType defectType, int qty) {
}
