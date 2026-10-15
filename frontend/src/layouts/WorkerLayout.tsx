import { Outlet } from 'react-router'
import AppHeader from './AppHeader.tsx'

export default function WorkerLayout() {
  return (
    <>
      <AppHeader role="worker" />
      <main className="page">
        <Outlet />
      </main>
    </>
  )
}
