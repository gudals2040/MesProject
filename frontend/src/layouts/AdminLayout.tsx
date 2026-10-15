import { Outlet } from 'react-router'
import AppHeader from './AppHeader.tsx'

export default function AdminLayout() {
  return (
    <>
      <AppHeader role="admin" />
      <main className="page">
        <Outlet />
      </main>
    </>
  )
}
