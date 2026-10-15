package com.minimes.server.collection.domain;

import com.minimes.common.equipment.Metric;
import com.minimes.server.equipment.domain.Equipment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 설비 측정 데이터 (ADR-002: 세로형, ADR-003: message_id로 중복 수신 방지).
 * 저장은 2단계에서 INSERT ... ON CONFLICT (message_id) DO NOTHING 쿼리로 한다 (S1).
 */
@Entity
@Table(name = "sensor_reading")
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 64)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric", nullable = false, length = 20)
    private Metric metric;

    @Column(name = "value", nullable = false)
    private double value;

    /** 설비가 측정한 시각. 집계·순서 판단의 기준 */
    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    /** 서버가 받은 시각. 지연 분석용 */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected SensorReading() {
    }

    public SensorReading(String messageId, Equipment equipment, Metric metric,
                         double value, Instant measuredAt, Instant receivedAt) {
        this.messageId = messageId;
        this.equipment = equipment;
        this.metric = metric;
        this.value = value;
        this.measuredAt = measuredAt;
        this.receivedAt = receivedAt;
    }

    public Long getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public Metric getMetric() {
        return metric;
    }

    public double getValue() {
        return value;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
