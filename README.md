# Mini MES

설비 데이터 수집부터 작업지시 · 생산 실적 · 불량 · 비가동 관리까지 이어지는 MES 핵심 흐름을
Java / Spring Boot로 구현하는 1인 프로젝트입니다.

## 모듈 구조

| 모듈 | 역할 |
|---|---|
| `common` | 시뮬레이터와 서버가 함께 쓰는 코드 (설비 상태, 측정 항목 enum). Spring 의존성 없음 |
| `simulator` | 가상 설비. 센서값과 설비 상태를 생성해 서버로 전송 (2단계) |
| `mes-server` | 수집 · 작업지시 · 실적 · 불량 · 비가동 · 알람 · 대시보드 API |
| `frontend` | React + TypeScript (Vite). 관리자 · 작업자 화면. Gradle 모듈이 아니며 npm으로 실행 ([frontend/README.md](frontend/README.md)) |

`mes-server` 패키지는 MES 기능 단위로 나눕니다.

```
com.minimes.server
├─ equipment    설비, 상태 전이, 비가동 이력
├─ item         품목
├─ workorder    작업지시와 상태 전이
├─ production   생산 실적, 불량
├─ collection   설비 측정 데이터 수집
├─ alarm        설비 이상 알람
└─ global       공통 (에러 코드 등)
```

## 기술 스택

- Java 21, Spring Boot 3.5, Spring Data JPA
- PostgreSQL 16, Flyway (스키마 관리), Hibernate `ddl-auto: validate`
- Gradle 멀티모듈
- 프론트엔드: React 19, TypeScript, Vite, React Router

## 실행 방법

0. (처음 한 번) Gradle Wrapper 생성 — IntelliJ에서 프로젝트를 연 뒤
   Gradle 탭 → `mini-mes` → Tasks → build setup → `wrapper` 실행.
   `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`가 만들어집니다.

1. PostgreSQL 실행

   ```bash
   docker compose up -d
   ```

2. 서버 실행 — 기동 시 Flyway가 `V1__init.sql`을 적용하고, Hibernate가 엔티티와 스키마가 일치하는지 검증합니다.

   ```bash
   ./gradlew :mes-server:bootRun
   ```

3. 테스트

   ```bash
   ./gradlew test
   ```

4. 프론트엔드 (Node.js 20.19 이상) — `/api` 요청은 8080 서버로 프록시됩니다.

   ```bash
   cd frontend
   npm install
   npm run dev      # http://localhost:5173
   ```

## 설계 원칙

- **규칙은 가장 낮은 곳에서 막는다** — DB 제약으로 막을 수 있는 규칙(설비당 진행 중 작업지시 1건 등)은 DB에서,
  상태 전이는 엔티티 메서드에서, 여러 테이블에 걸친 규칙은 서비스 트랜잭션에서 보장합니다.
- **상태 전이는 엔티티 메서드로만** — 상태를 바꾸는 setter를 두지 않습니다.
- **확정된 실적은 바꾸지 않는다** — 실적은 확정 시점에 생성되고 수정 메서드가 없습니다.

## 설계 결정 기록 (ADR)

| ADR | 결정 |
|---|---|
| 001 | 설비 시뮬레이터를 Java(Spring Boot 별도 모듈)로 구현 |
| 002 | 센서 데이터는 세로형(metric + value)으로 저장 |
| 003 | message_id 유니크 키로 설비 데이터 중복 수신 방지 |
| 004 | 작업지시와 생산 실적은 1:N |
| 005 | 비가동은 상태 변화로 자동 기록, 사유는 분류 |
| 006 | 프론트엔드는 React, 백엔드 API 이후 개발 |
| 007 | 생산 실적은 센서 COUNT 자동 집계 + 작업자 확정 |
| 008 | 작업지시 1건 = 설비 1대 |
| 009 | 확정 실적은 수정 불가, 계획 초과 생산은 허용하되 경고 |
| 010 | 인증 없이 사용자(member)와 역할(ADMIN/WORKER)을 두고 X-Member-Id로 처리자 식별 |

상세 문서(배경 · 대안 · 근거 · 트레이드 오프)는 Notion "미니 MES 1인 프로젝트" 페이지의 의사결정 기록에 있습니다.

## 진행 단계

- [x] 1단계: 도메인 모델링 (ERD, 상태 전이, 비즈니스 규칙, DDL, 엔티티)
- [ ] 2단계: 수집 파이프라인 (시뮬레이터 → 서버 → 저장)
- [ ] 3단계: MES 핵심 흐름 API (작업지시 → 실적 → 불량)
- [ ] 프론트엔드: React 작업자 화면 · 관리자 대시보드
- [ ] 4단계: 이상 감지와 알림
- [ ] 5단계: 신뢰성 보강 (Outbox, 멱등성, 재시도 · DLQ)
- [ ] 6단계: 배포와 측정 (Docker, AWS, k6)
