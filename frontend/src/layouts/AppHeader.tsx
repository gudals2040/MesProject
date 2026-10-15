import { NavLink } from 'react-router'

type Role = 'admin' | 'worker'

const MENUS: Record<Role, { to: string; label: string; end?: boolean }[]> = {
  admin: [
    { to: '/admin', label: '대시보드', end: true },
    { to: '/admin/equipments/1', label: '설비' },
    { to: '/admin/work-orders', label: '작업지시 관리' },
  ],
  worker: [
    { to: '/worker', label: '작업지시', end: true },
    { to: '/worker/downtimes', label: '비가동 사유' },
  ],
}

/** 상단 바. 흰색 = 관리자, 검은색 = 작업자 (화면 설계 공통 사항) */
export default function AppHeader({ role }: { role: Role }) {
  return (
    <header className={`app-header app-header--${role}`}>
      <span className="app-header__brand">미니 MES</span>
      <span className="app-header__role">{role === 'admin' ? '관리자' : '작업자'}</span>
      <nav className="app-header__nav">
        {MENUS[role].map((m) => (
          <NavLink key={m.to} to={m.to} end={m.end}>
            {m.label}
          </NavLink>
        ))}
      </nav>
      {/* TODO: 사용자 선택(X-Member-Id), SSE 연결 상태 */}
    </header>
  )
}
