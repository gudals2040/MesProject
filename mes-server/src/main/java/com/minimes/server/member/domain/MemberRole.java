package com.minimes.server.member.domain;

/**
 * 사용자 역할 (ADR-010). 화면·API를 역할별로 나눈다.
 *   - ADMIN : 작업지시 등록·취소, 알람 확인 (R1)
 *   - WORKER: 작업 시작·완료, 실적 확정, 비가동 사유 입력 (R2)
 */
public enum MemberRole {
    ADMIN,
    WORKER
}
