# Mini MES 프론트엔드

React + TypeScript, Vite로 빌드합니다. 백엔드(`mes-server`)와 같은 레포에서 관리합니다.

## 실행

Node.js 20.19 이상(22 LTS 권장)이 필요합니다.

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
```

개발 서버는 `/api` 요청을 `http://localhost:8080`(mes-server)으로 넘깁니다. 서버를 함께 띄우려면 루트에서 `./gradlew :mes-server:bootRun`.

| 명령 | 설명 |
|---|---|
| `npm run dev` | 개발 서버 |
| `npm run build` | 타입 검사 후 `dist/`로 빌드 |
| `npm run lint` | oxlint 검사 |

## 구조

```
src/
├─ api/        fetch 래퍼(X-Member-Id 헤더, 에러 코드), 사용자 선택 저장
├─ layouts/    역할별 레이아웃과 상단 바 (흰색 = 관리자, 검은색 = 작업자)
├─ pages/
│  ├─ admin/   ADM-01 대시보드, ADM-02 설비 상세, ADM-03 작업지시 관리
│  └─ worker/  WRK-01 작업지시, WRK-02 실적 등록, WRK-03 비가동 사유 입력
└─ router.tsx  화면 ID ↔ URL
```

| 화면 | URL |
|---|---|
| ADM-01 | `/admin` |
| ADM-02 | `/admin/equipments/:equipmentId` |
| ADM-03 | `/admin/work-orders` |
| WRK-01 | `/worker` |
| WRK-02 | `/worker/work-orders/:workOrderId/results` |
| WRK-03 | `/worker/downtimes` |
