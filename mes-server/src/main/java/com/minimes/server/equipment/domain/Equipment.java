package com.minimes.server.equipment.domain;

import com.minimes.common.equipment.EquipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;

/**
 * 설비 마스터. 상태(status)는 설비 상태 신호로만 바뀐다.
 */
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EquipmentStatus status;

    /** 마지막 상태 변경 시각. 이보다 오래된 상태 메시지는 무시한다 (T3) */
    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Equipment() {
    }

    public Equipment(String code, String name, Instant now) {
        this.code = code;
        this.name = name;
        this.status = EquipmentStatus.IDLE;
        this.statusChangedAt = now;
        this.createdAt = now;
    }

    /**
     * 설비 상태 신호를 반영한다.
     *
     * @return 실제로 상태가 바뀐 경우에만 전이 정보. 오래된 메시지(T3)나 같은 상태면 비어 있음
     */
    public Optional<StatusTransition> changeStatus(EquipmentStatus newStatus, Instant occurredAt) {
        if (occurredAt.isBefore(statusChangedAt)) {
            return Optional.empty();
        }
        if (newStatus == status) {
            return Optional.empty();
        }
        StatusTransition transition = new StatusTransition(status, newStatus, occurredAt);
        this.status = newStatus;
        this.statusChangedAt = occurredAt;
        return Optional.of(transition);
    }

    public boolean isDown() {
        return status == EquipmentStatus.DOWN;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
