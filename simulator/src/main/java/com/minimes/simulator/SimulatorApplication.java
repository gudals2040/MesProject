package com.minimes.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 가상 설비 시뮬레이터 (ADR-001).
 * 2단계에서 @Scheduled로 센서값·설비 상태를 생성해 MES 서버로 전송한다.
 */
@EnableScheduling
@SpringBootApplication
public class SimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }
}
