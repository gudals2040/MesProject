/** 화면 구현 전 자리 표시. 화면 ID는 노션 "화면 설계 (와이어프레임)" 기준 */
export default function ScreenPlaceholder({ id, title }: { id: string; title: string }) {
  return (
    <section>
      <p className="screen-id">{id}</p>
      <h1>{title}</h1>
      <p className="muted">구현 예정</p>
    </section>
  )
}
