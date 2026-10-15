-- =============================================================
-- 미니 MES V2: 작업자·관리자 구분 (Flyway: db/migration/V2__member.sql)
-- ADR-010, 규칙 R1~R3 (Notion "상태 전이 · 비즈니스 규칙")
-- 인증은 MVP 범위 밖: 비밀번호 없이 X-Member-Id 헤더로 식별, role로 접근 구분
-- V1은 이미 적용됐으므로 수정하지 않고 이 파일로 변경한다 (체크섬 불일치 방지)
-- =============================================================

CREATE TABLE member (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       varchar(50)  NOT NULL,
    role       varchar(20)  NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_member_role CHECK (role IN ('ADMIN', 'WORKER'))
);

-- 기준 데이터 (화면 상단에서 사용자를 고르는 용도)
INSERT INTO member (name, role) VALUES
    ('관리자', 'ADMIN'),
    ('작업자1', 'WORKER'),
    ('작업자2', 'WORKER');

-- 처리자 컬럼 추가
ALTER TABLE work_order        ADD COLUMN created_by      bigint REFERENCES member (id);
ALTER TABLE production_result ADD COLUMN confirmed_by    bigint REFERENCES member (id);
ALTER TABLE equipment_alarm   ADD COLUMN acknowledged_by bigint REFERENCES member (id);
ALTER TABLE downtime          ADD COLUMN classified_by   bigint REFERENCES member (id);  -- NULL = 미분류·자동 분류

-- 기존 데이터가 있으면 기본 사용자로 채운 뒤 NOT NULL 적용 (R3)
UPDATE work_order
   SET created_by = (SELECT id FROM member WHERE role = 'ADMIN' ORDER BY id LIMIT 1)
 WHERE created_by IS NULL;
UPDATE production_result
   SET confirmed_by = (SELECT id FROM member WHERE role = 'WORKER' ORDER BY id LIMIT 1)
 WHERE confirmed_by IS NULL;
UPDATE equipment_alarm
   SET acknowledged_by = (SELECT id FROM member WHERE role = 'ADMIN' ORDER BY id LIMIT 1)
 WHERE acknowledged_at IS NOT NULL AND acknowledged_by IS NULL;

ALTER TABLE work_order        ALTER COLUMN created_by   SET NOT NULL;
ALTER TABLE production_result ALTER COLUMN confirmed_by SET NOT NULL;

-- R3: 알람 확인 시각과 확인자는 함께 기록
ALTER TABLE equipment_alarm
    ADD CONSTRAINT ck_equipment_alarm_ack_by
        CHECK ((acknowledged_at IS NULL) = (acknowledged_by IS NULL));

-- 처리자별 조회
CREATE INDEX ix_work_order_created_by ON work_order (created_by);
CREATE INDEX ix_production_result_confirmed_by ON production_result (confirmed_by);
