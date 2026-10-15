package com.minimes.server.member.domain;

import com.minimes.server.global.error.ErrorCode;
import com.minimes.server.global.error.MesException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 사용자 (ADR-010).
 * 인증은 MVP 범위 밖이라 비밀번호 없이 식별만 한다. 요청 헤더 X-Member-Id로 찾은 사용자를
 * 도메인 메서드에 처리자로 넘기고, 역할 검사(R1, R2)와 처리자 기록(R3)은 도메인에서 한다.
 */
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Member() {
    }

    public Member(String name, MemberRole role, Instant now) {
        this.name = name;
        this.role = role;
        this.createdAt = now;
    }

    /**
     * 처리자가 있고 역할이 맞는지 확인한다.
     * 처리자가 없으면 MEMBER_REQUIRED, 역할이 다르면 FORBIDDEN_ROLE (R1, R2).
     *
     * @return 검사를 통과한 처리자
     */
    public static Member requireRole(Member actor, MemberRole required) {
        if (actor == null) {
            throw new MesException(ErrorCode.MEMBER_REQUIRED);
        }
        if (actor.role != required) {
            throw new MesException(ErrorCode.FORBIDDEN_ROLE);
        }
        return actor;
    }

    public boolean hasRole(MemberRole role) {
        return this.role == role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MemberRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
