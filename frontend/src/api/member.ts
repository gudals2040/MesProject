// 인증은 MVP 범위 밖(ADR-010). 상단에서 고른 사용자 ID를 저장해 두고 요청마다 X-Member-Id로 보낸다
const KEY = 'mini-mes.memberId'

export function getMemberId(): string | null {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

export function setMemberId(id: string | null): void {
  try {
    if (id) localStorage.setItem(KEY, id)
    else localStorage.removeItem(KEY)
  } catch {
    // 저장소를 쓸 수 없는 환경이면 새로고침 때 다시 고르면 된다
  }
}
