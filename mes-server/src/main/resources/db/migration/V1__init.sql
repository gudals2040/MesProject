-- =============================================================
-- 미니 MES V1 초기 스키마 (Flyway: db/migration/V1__init.sql)
-- PostgreSQL 16
-- 규칙 번호(W1, P3 ...)는 Notion "상태 전이 · 비즈니스 규칙" 문서 기준
-- 상태값은 JPA @Enumerated(EnumType.STRING)과 맞추기 위해 varchar + CHECK 사용
-- =============================================================

-- -------------------------------------------------------------
-- 설비 마스터
-- status는 설비 상태 신호로만 변경, status_changed_at은 T3(오래된 메시지 무시) 판단 기준
-- -------------------------------------------------------------
CREATE TABLE equipment (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code              varchar(30)  NOT NULL,
    name              varchar(100) NOT NULL,
    status            varchar(20)  NOT NULL DEFAULT 'IDLE',
    status_changed_at timestamptz  NOT NULL DEFAULT now(),
    created_at        timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_equipment_code UNIQUE (code),
    CONSTRAINT ck_equipment_status CHECK (status IN ('RUNNING', 'IDLE', 'DOWN'))
);

-- -------------------------------------------------------------
-- 품목 마스터
-- -------------------------------------------------------------
CREATE TABLE item (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       varchar(30)  NOT NULL,
    name       varchar(100) NOT NULL,
    unit       varchar(10)  NOT NULL DEFAULT 'EA',
    created_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_item_code UNIQUE (code)
);

-- -------------------------------------------------------------
-- 작업지시 (ADR-008: 작업지시 1건 = 설비 1대)
-- -------------------------------------------------------------
CREATE TABLE work_order (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no     varchar(30) NOT NULL,
    item_id      bigint      NOT NULL REFERENCES item (id),
    equipment_id bigint      NOT NULL REFERENCES equipment (id),
    planned_qty  int         NOT NULL,
    status       varchar(20) NOT NULL DEFAULT 'WAITING',
    started_at   timestamptz,
    completed_at timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_work_order_order_no UNIQUE (order_no),
    CONSTRAINT ck_work_order_status
        CHECK (status IN ('WAITING', 'IN_PROGRESS', 'COMPLETED', 'CANCELED')),
    -- W4: 계획 수량은 1 이상
    CONSTRAINT ck_work_order_planned_qty CHECK (planned_qty >= 1),
    -- 진행 중·완료 상태면 시작 시각이 있어야 함
    CONSTRAINT ck_work_order_started_at
        CHECK (status IN ('WAITING', 'CANCELED') OR started_at IS NOT NULL),
    -- 완료 시각은 완료 상태에서만, 시작 이후
    CONSTRAINT ck_work_order_completed_at
        CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL)
               AND (completed_at IS NULL OR completed_at >= started_at))
);

-- W1: 설비당 진행 중(IN_PROGRESS) 작업지시는 1건만
CREATE UNIQUE INDEX ux_work_order_equipment_in_progress
    ON work_order (equipment_id)
    WHERE status = 'IN_PROGRESS';

CREATE INDEX ix_work_order_status ON work_order (status);

-- -------------------------------------------------------------
-- 생산 실적 (ADR-004: 작업지시 1:N, ADR-007: 센서 집계 + 작업자 확정)
-- 확정 시점에 생성되므로 confirmed_at은 NOT NULL (P7: 생성 후 수정 불가)
-- -------------------------------------------------------------
CREATE TABLE production_result (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    work_order_id bigint      NOT NULL REFERENCES work_order (id),
    period_start  timestamptz NOT NULL,
    period_end    timestamptz NOT NULL,
    counted_qty   int         NOT NULL,
    good_qty      int         NOT NULL,
    defect_qty    int         NOT NULL,
    confirmed_at  timestamptz NOT NULL DEFAULT now(),
    -- P3: 구간 끝 > 구간 시작 (미래 시각 불가는 서비스에서 검증)
    CONSTRAINT ck_production_result_period CHECK (period_end > period_start),
    -- P5: 수량은 0 이상
    CONSTRAINT ck_production_result_qty
        CHECK (counted_qty >= 0 AND good_qty >= 0 AND defect_qty >= 0),
    -- P2 보조: 같은 작업지시에 같은 시작 시각의 구간은 하나뿐 (동시 등록 시 이중 방어)
    CONSTRAINT uk_production_result_period UNIQUE (work_order_id, period_start)
);

-- -------------------------------------------------------------
-- 불량 유형 / 불량 내역
-- -------------------------------------------------------------
CREATE TABLE defect_type (
    id   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(30)  NOT NULL,
    name varchar(100) NOT NULL,
    CONSTRAINT uk_defect_type_code UNIQUE (code)
);

CREATE TABLE defect (
    id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    production_result_id bigint NOT NULL REFERENCES production_result (id),
    defect_type_id       bigint NOT NULL REFERENCES defect_type (id),
    qty                  int    NOT NULL,
    -- D2: 불량 내역 수량은 1 이상
    CONSTRAINT ck_defect_qty CHECK (qty >= 1),
    -- 한 실적 안에서 같은 불량 유형은 한 줄로 합산
    CONSTRAINT uk_defect_result_type UNIQUE (production_result_id, defect_type_id)
);

-- -------------------------------------------------------------
-- 설비 측정 데이터 (ADR-002: 세로형, ADR-003: message_id 멱등)
-- -------------------------------------------------------------
CREATE TABLE sensor_reading (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    message_id   varchar(64)      NOT NULL,
    equipment_id bigint           NOT NULL REFERENCES equipment (id),
    metric       varchar(20)      NOT NULL,
    value        double precision NOT NULL,
    measured_at  timestamptz      NOT NULL,
    received_at  timestamptz      NOT NULL DEFAULT now(),
    -- S1: 같은 message_id는 한 번만 저장 (INSERT ... ON CONFLICT DO NOTHING)
    CONSTRAINT uk_sensor_reading_message_id UNIQUE (message_id),
    CONSTRAINT ck_sensor_reading_metric
        CHECK (metric IN ('TEMPERATURE', 'VIBRATION', 'COUNT'))
);

-- 설비·항목별 최근 값 조회, P4 구간 COUNT 합계 조회
CREATE INDEX ix_sensor_reading_equipment_metric_time
    ON sensor_reading (equipment_id, metric, measured_at);

-- -------------------------------------------------------------
-- 설비 이상 알림
-- -------------------------------------------------------------
CREATE TABLE equipment_alarm (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    equipment_id      bigint           NOT NULL REFERENCES equipment (id),
    sensor_reading_id bigint           REFERENCES sensor_reading (id),
    rule              varchar(50)      NOT NULL,
    value             double precision NOT NULL,
    threshold         double precision NOT NULL,
    occurred_at       timestamptz      NOT NULL,
    acknowledged_at   timestamptz,
    CONSTRAINT ck_equipment_alarm_ack
        CHECK (acknowledged_at IS NULL OR acknowledged_at >= occurred_at)
);

-- A1: 같은 설비·같은 규칙의 미확인 알람은 1건만 (알람 폭주 방지)
CREATE UNIQUE INDEX ux_equipment_alarm_open
    ON equipment_alarm (equipment_id, rule)
    WHERE acknowledged_at IS NULL;

-- -------------------------------------------------------------
-- 비가동 사유 / 비가동 이력 (ADR-005)
-- -------------------------------------------------------------
CREATE TABLE downtime_reason (
    id       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     varchar(30)  NOT NULL,
    name     varchar(100) NOT NULL,
    category varchar(20)  NOT NULL,
    CONSTRAINT uk_downtime_reason_code UNIQUE (code),
    CONSTRAINT ck_downtime_reason_category CHECK (category IN ('PLANNED', 'UNPLANNED'))
);

CREATE TABLE downtime (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    equipment_id  bigint      NOT NULL REFERENCES equipment (id),
    work_order_id bigint      REFERENCES work_order (id),       -- NULL = 작업 없음
    reason_id     bigint      REFERENCES downtime_reason (id),  -- NULL = 미분류
    source        varchar(10) NOT NULL DEFAULT 'AUTO',
    equipment_status varchar(20) NOT NULL,                      -- 정지 당시 상태(IDLE / DOWN)
    started_at    timestamptz NOT NULL,
    ended_at      timestamptz,                                  -- NULL = 진행 중
    CONSTRAINT ck_downtime_source CHECK (source IN ('AUTO', 'MANUAL')),
    CONSTRAINT ck_downtime_equipment_status CHECK (equipment_status IN ('IDLE', 'DOWN')),
    -- T2: 종료 시각 >= 시작 시각
    CONSTRAINT ck_downtime_period CHECK (ended_at IS NULL OR ended_at >= started_at)
);

-- T1: 설비당 진행 중 비가동은 1건만
CREATE UNIQUE INDEX ux_downtime_equipment_open
    ON downtime (equipment_id)
    WHERE ended_at IS NULL;

-- 기간별 비가동 집계(OEE), 작업지시별 비가동 조회
CREATE INDEX ix_downtime_equipment_started ON downtime (equipment_id, started_at);
CREATE INDEX ix_downtime_work_order ON downtime (work_order_id);

-- -------------------------------------------------------------
-- 기준 데이터
-- -------------------------------------------------------------
INSERT INTO downtime_reason (code, name, category) VALUES
    ('NO_ORDER',      '작업 없음',      'PLANNED'),
    ('BREAK',         '휴식·식사',      'PLANNED'),
    ('PLANNED_MAINT', '계획 보전',      'PLANNED'),
    ('BREAKDOWN',     '설비 고장',      'UNPLANNED'),
    ('MATERIAL_WAIT', '자재 대기',      'UNPLANNED'),
    ('QUALITY_WAIT',  '품질 확인 대기', 'UNPLANNED'),
    ('CHANGEOVER',    '금형·품목 교체', 'UNPLANNED'),
    ('NO_OPERATOR',   '작업자 부재',    'UNPLANNED');

INSERT INTO defect_type (code, name) VALUES
    ('DIMENSION', '치수 불량'),
    ('APPEARANCE', '외관 불량'),
    ('CRACK',      '크랙'),
    ('CONTAMINATION', '이물');
