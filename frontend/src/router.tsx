import { createBrowserRouter } from 'react-router'
import AdminLayout from './layouts/AdminLayout.tsx'
import WorkerLayout from './layouts/WorkerLayout.tsx'
import HomePage from './pages/HomePage.tsx'
import NotFoundPage from './pages/NotFoundPage.tsx'
import DashboardPage from './pages/admin/DashboardPage.tsx'
import EquipmentDetailPage from './pages/admin/EquipmentDetailPage.tsx'
import WorkOrderAdminPage from './pages/admin/WorkOrderAdminPage.tsx'
import WorkOrdersPage from './pages/worker/WorkOrdersPage.tsx'
import ResultEntryPage from './pages/worker/ResultEntryPage.tsx'
import DowntimePage from './pages/worker/DowntimePage.tsx'

// 화면 설계(와이어프레임)의 화면 ID와 URL을 1:1로 맞춘다. 역할이 다른 화면으로 가는 링크는 두지 않는다
export const router = createBrowserRouter([
  { path: '/', element: <HomePage /> },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: <DashboardPage /> }, // ADM-01
      { path: 'equipments/:equipmentId', element: <EquipmentDetailPage /> }, // ADM-02
      { path: 'work-orders', element: <WorkOrderAdminPage /> }, // ADM-03
    ],
  },
  {
    path: '/worker',
    element: <WorkerLayout />,
    children: [
      { index: true, element: <WorkOrdersPage /> }, // WRK-01
      { path: 'work-orders/:workOrderId/results', element: <ResultEntryPage /> }, // WRK-02
      { path: 'downtimes', element: <DowntimePage /> }, // WRK-03
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
