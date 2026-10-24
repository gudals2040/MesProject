package com.minimes.simulator.equipment;

/**
 * 시뮬레이터 한 번의 주기(tick)마다 설비 상태가 바뀔 확률. 값은 0.0 ~ 1.0.
 *
 * <p>가동 → 대기 → 가동, 가동 → 정지 → 가동처럼 현장에서 흔한 흐름이 나오도록 정한다.
 * 정지(DOWN)는 고장이라 가동으로만 복구한다고 가정한다.
 */
public record TransitionRule(
        double runToIdle,
        double runToDown,
        double idleToRun,
        double idleToDown,
        double downToRun
) {

    /** 상태가 절대 바뀌지 않는 규칙 (테스트용) */
    public static TransitionRule none() {
        return new TransitionRule(0, 0, 0, 0, 0);
    }
}
