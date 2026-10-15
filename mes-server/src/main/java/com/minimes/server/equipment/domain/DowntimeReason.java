package com.minimes.server.equipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 비가동 사유 코드. 기준 데이터는 V1__init.sql에 있다.
 */
@Entity
@Table(name = "downtime_reason")
public class DowntimeReason {

    /** 진행 중인 작업지시가 없을 때 자동으로 붙는 사유 (계획 정지) */
    public static final String NO_ORDER = "NO_ORDER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private DowntimeCategory category;

    protected DowntimeReason() {
    }

    public DowntimeReason(String code, String name, DowntimeCategory category) {
        this.code = code;
        this.name = name;
        this.category = category;
    }

    public boolean isPlanned() {
        return category == DowntimeCategory.PLANNED;
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

    public DowntimeCategory getCategory() {
        return category;
    }
}
