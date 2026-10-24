# Code Convention

## 1. 패키지 구조

기능 단위로 나누고, 그 안을 계층으로 나눕니다.

```
com.minimes.server
├─ {feature}            equipment, item, workorder, production, collection, alarm, member
│  ├─ domain            엔티티, enum, 도메인 규칙
│  ├─ repository        Spring Data JPA, 네이티브 쿼리
│  ├─ service           트랜잭션, 여러 엔티티에 걸친 규칙
│  ├─ controller        REST API
│  └─ dto               요청·응답 객체
└─ global               error, config, common (공통 응답, 인자 리졸버 등)
```

- `common` 모듈에는 시뮬레이터와 서버가 함께 쓰는 코드(enum, 메시지 DTO)만 둡니다. Spring 의존성을 넣지 않습니다.
- 다른 기능의 `repository`를 직접 쓰지 않고 그 기능의 `service`를 거칩니다. 엔티티 연관관계는 예외로 합니다.

## 2. 네이밍

| 대상 | 규칙 | 예 |
| --- | --- | --- |
| 클래스 | PascalCase, 명사 | `WorkOrder`, `StatusTransition` |
| 메서드 | camelCase, 동사로 시작 | `start`, `changeStatus`, `nextPeriodStart` |
| boolean | `is`, `has`, `can`으로 시작 | `isDown`, `isOverProduced` |
| 상수, enum 값 | UPPER_SNAKE_CASE | `IN_PROGRESS`, `EQUIPMENT_DOWN` |
| 요청·응답 DTO | `{동작}{대상}Request` / `{대상}Response` | `CreateWorkOrderRequest`, `WorkOrderResponse` |
| 수집 메시지 (common) | `{대상}Message` | `SensorMessage`, `StatusMessage` |
| 서비스·컨트롤러 | `{대상}Service`, `{역할}{대상}Controller` | `WorkOrderService`, `AdminWorkOrderController` |
| 테스트 | `{대상}Test` | `WorkOrderTest` |
| 테이블·컬럼 | snake_case, 테이블은 단수 | `work_order`, `planned_qty` |

- 컬럼 접미사: 시각은 `_at`, 처리자는 `_by`(member FK), 수량은 `_qty`
- 약어는 단어처럼 씁니다: `MesException`, `ApiResponse` (`MESException` ❌)

## 3. 엔티티

- setter를 두지 않습니다. 상태는 의미 있는 이름의 메서드로만 바꿉니다. (`start`, `complete`, `cancel`)
- 규칙 검사는 상태를 바꾸는 메서드 안에서 하고, 어기면 `MesException(ErrorCode)`를 던집니다.
- JPA용 기본 생성자는 `protected`로 둡니다.
- enum은 `@Enumerated(EnumType.STRING)`, `@ManyToOne`은 모두 `FetchType.LAZY`
- `@Table`, `@Column`에 DB 이름을 명시해 스키마와 1:1로 대응시킵니다.
- 시각 타입은 `Instant`. 현재 시각은 엔티티가 직접 구하지 않고 인자(`Instant now`)로 받습니다. 테스트에서 시각을 고정하기 위해서입니다.
- 엔티티는 Lombok 없이 getter를 직접 작성합니다. DTO는 `record`로 만듭니다.

## 4. 서비스 · 트랜잭션

- 의존성은 생성자 주입, 필드는 `private final`
- 조회 메서드는 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional`
- 외부 호출(HTTP, 알림 전송)은 트랜잭션 안에 넣지 않습니다. 트랜잭션이 길어지면 DB 커넥션을 오래 잡습니다.
- 규칙을 지키는 위치: DB 제약 → 엔티티 메서드 → 서비스 트랜잭션 순으로, 가장 낮은 곳에서 막습니다.

## 5. 예외 · 에러 코드

- 도메인 규칙 위반은 `MesException(ErrorCode)` 하나로 던집니다.
- `ErrorCode`는 기능별로 묶고, 메시지 끝에 규칙 번호를 적습니다. 예: `"계획 수량은 1 이상이어야 합니다. (W4)"`
- HTTP 상태 코드 매핑과 공통 응답 형식은 3단계에서 `@RestControllerAdvice`와 함께 정합니다.

## 6. API

- 경로는 복수 명사, kebab-case: `/api/admin/work-orders`, `/api/worker/work-orders/{id}/start`
- 역할별로 경로를 나눕니다: `/api/admin/**`, `/api/worker/**`
- 처리자는 `X-Member-Id` 헤더로 받습니다.
- 상태 변경은 동사 하위 경로 + POST: `.../{id}/start`, `.../{id}/cancel`

## 7. 주석

- 클래스·메서드 Javadoc에 지키는 규칙 번호(W1, P3 …)를 적습니다. 규칙 정의는 [비즈니스 규칙](business-rules.md)에 있습니다.
- "무엇"보다 "왜"를 씁니다. 코드로 드러나는 내용은 반복하지 않습니다.
- 다음 단계로 미룬 작업은 `// TODO(5단계): Outbox로 전환`처럼 단계를 적습니다.

## 8. 테스트

- 메서드 이름은 영어 camelCase, `@DisplayName`은 한글로 쓰고 규칙 번호로 시작합니다.
  예: `"W2: 설비가 DOWN이면 작업을 시작할 수 없다"`
- 본문은 준비 / 실행 / 검증을 빈 줄로 구분합니다.
- 테스트 데이터는 `support/Fixtures`로 만듭니다.
- 예외 테스트는 예외 타입뿐 아니라 `ErrorCode`까지 확인하고, 상태가 바뀌지 않았는지도 확인합니다.
- DB 동작(중복 저장 방지, 제약 조건)은 실제 PostgreSQL로 테스트합니다.

## 9. 포맷

- [`.editorconfig`](../.editorconfig) 기준: Java 4칸, TS·YAML·JSON 2칸, 한 줄 120자, 줄바꿈 LF
- import에 와일드카드(`*`)를 쓰지 않습니다. 테스트의 assert static import는 허용합니다.
- IntelliJ 기본 Java 스타일을 쓰고, 커밋 전 Reformat Code / Optimize Imports를 실행합니다.

## 10. 프론트엔드

- 화면 컴포넌트는 `pages/{admin|worker}/XxxPage.tsx`, 레이아웃은 `layouts/`
- API 호출은 `src/api/{대상}.ts`에만 둡니다. 컴포넌트에서 직접 fetch하지 않습니다.
- 컴포넌트·파일은 PascalCase, 함수·변수는 camelCase
