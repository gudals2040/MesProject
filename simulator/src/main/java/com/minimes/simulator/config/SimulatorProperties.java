package com.minimes.simulator.config;

import com.minimes.simulator.equipment.TransitionRule;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 시뮬레이터 설정 (application.yml의 simulator.*).
 *
 * @param serverUrl    MES 서버 주소
 * @param sensorPath   센서 수집 API 경로
 * @param statusPath   상태 수집 API 경로
 * @param interval     전송 주기
 * @param equipments   시뮬레이션할 설비 코드. 서버 equipment.code에 등록돼 있어야 한다 (S2)
 * @param anomalyRate  주기마다 이상값을 섞을 확률 (4단계 이상 감지 테스트용)
 * @param duplicateRate 같은 메시지를 한 번 더 보낼 확률 (중복 수신 테스트용, S1·T3)
 * @param transition   상태 전이 확률
 */
@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(
        String serverUrl,
        String sensorPath,
        String statusPath,
        Duration interval,
        List<String> equipments,
        double anomalyRate,
        double duplicateRate,
        TransitionRule transition
) {
}
