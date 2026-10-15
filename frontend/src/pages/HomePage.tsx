import { Link } from 'react-router'

export default function HomePage() {
  return (
    <main className="page home">
      <h1>미니 MES</h1>
      <p className="muted">역할을 고르세요</p>
      <div className="home__links">
        <Link to="/admin">관리자 화면</Link>
        <Link to="/worker">작업자 화면</Link>
      </div>
    </main>
  )
}
