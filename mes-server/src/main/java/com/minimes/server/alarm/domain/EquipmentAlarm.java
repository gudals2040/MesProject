package com.minimes.server.alarm.domain;

import com.minimes.server.collection.domain.SensorReading;
import com.minimes.server.equipment.domain.Equipment;
import com.minimes.server.global.error.ErrorCode;
import com.minimes.server.global.error.MesException;
import com.minimes.server.member.domain.Member;
import com.minimes.server.member.domain.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 설비 이상 알림.
 * A1(같은 설비·규칙의 미확인 알람은 1건)은 DB 부분 유니크 인덱스가 보장한다.
 * 관리자만 확인할 수 있고 (R1), 확인 시각과 확인자를 함께 기록한다 (R3, DB CHECK로도 보장).
 */
@Entity
@Table(name = "equipment_alarm")
public class EquipmentAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    /** 알람을 일으킨 측정값 (있을 때만) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_reading_id")
    private SensorReading sensorReading;

    @Column(name = "rule", nullable = false, length = 50)
    private String rule;

    @Column(name = "value", nullable = false)
    private double value;

    @Column(name = "threshold", nullable = false)
    private double threshold;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    /** 확인한 관리자. null = 미확인 (R3) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private Member acknowledgedBy;

    protected EquipmentAlarm() {
    }

    public EquipmentAlarm(Equipment equipment, SensorReading sensorReading, String rule,
                          double value, double threshold, Instant occurredAt) {
        this.equipment = equipment;
        this.sensorReading = sensorReading;
        this.rule = rule;
        this.value = value;
        this.threshold = threshold;
        this.occurredAt = occurredAt;
    }

    /** 관리자가 알람을 확인한다. 확인 후에는 같은 규칙의 알람이 다시 발생할 수 있다 */
    public void acknowledge(Member admin, Instant now) {
        Member.requireRole(admin, MemberRole.ADMIN);                                  // R1
        if (acknowledgedAt != null) {
            throw new MesException(ErrorCode.ALARM_ALREADY_ACKNOWLEDGED);
        }
        this.acknowledgedAt = now.isBefore(occurredAt) ? occurredAt : now;
        this.acknowledgedBy = admin;                                                  // R3
    }

    public boolean isOpen() {
        return acknowledgedAt == null;
    }

    public Long getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public SensorReading getSensorReading() {
        return sensorReading;
    }

    public String getRule() {
        return rule;
    }

    public double getValue() {
        return value;
    }

    public double getThreshold() {
        return threshold;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public Member getAcknowledgedBy() {
        return acknowledgedBy;
    }
}
