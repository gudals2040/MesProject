package com.minimes.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 가상 설비 시뮬레이터 (ADR-001). 설비별 센서값·상태를 만들어 MES 서버로 보낸다 (ADR-011 메시지 규격).
 */
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class SimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }
}
