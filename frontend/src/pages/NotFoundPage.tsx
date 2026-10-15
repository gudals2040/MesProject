import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <main className="page">
      <h1>페이지를 찾을 수 없어요</h1>
      <Link to="/">처음으로</Link>
    </main>
  )
}
