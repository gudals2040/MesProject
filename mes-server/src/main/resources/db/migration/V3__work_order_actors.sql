-- =============================================================
-- 미니 MES V3: 작업지시 시작·완료·취소 처리자 (Flyway: db/migration/V3__work_order_actors.sql)
-- ADR-010, 규칙 R3. V2가 이미 적용됐을 수 있으므로 V2를 고치지 않고 새 파일로 추가한다
-- =============================================================

ALTER TABLE work_order ADD COLUMN started_by   bigint REFERENCES member (id);
ALTER TABLE work_order ADD COLUMN completed_by bigint REFERENCES member (id);
ALTER TABLE work_order ADD COLUMN canceled_by  bigint REFERENCES member (id);
ALTER TABLE work_order ADD COLUMN canceled_at  timestamptz;

-- 기존 데이터가 있으면 기본 사용자로 채운다 (취소 시각은 알 수 없어 등록 시각으로 둔다)
UPDATE work_order
   SET started_by = (SELECT id FROM member WHERE role = 'WORKER' ORDER BY id LIMIT 1)
 WHERE started_at IS NOT NULL AND started_by IS NULL;
UPDATE work_order
   SET completed_by = (SELECT id FROM member WHERE role = 'WORKER' ORDER BY id LIMIT 1)
 WHERE status = 'COMPLETED' AND completed_by IS NULL;
UPDATE work_order
   SET canceled_by = (SELECT id FROM member WHERE role = 'ADMIN' ORDER BY id LIMIT 1),
       canceled_at = COALESCE(canceled_at, created_at)
 WHERE status = 'CANCELED' AND canceled_by IS NULL;

-- R3: 시각과 처리자는 함께 기록한다
ALTER TABLE work_order
    ADD CONSTRAINT ck_work_order_started_by
        CHECK ((started_at IS NULL) = (started_by IS NULL)),
    ADD CONSTRAINT ck_work_order_completed_by
        CHECK ((completed_at IS NULL) = (completed_by IS NULL)),
    -- 취소 시각·취소자는 CANCELED 상태에서만, 둘 다 있어야 함
    ADD CONSTRAINT ck_work_order_canceled
        CHECK ((status = 'CANCELED') = (canceled_at IS NOT NULL)
               AND (canceled_at IS NULL) = (canceled_by IS NULL));
