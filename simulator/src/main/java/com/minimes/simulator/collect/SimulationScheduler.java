package com.minimes.simulator.collect;

import com.minimes.common.equipment.EquipmentStatus;
import com.minimes.common.message.SensorMessage;
import com.minimes.simulator.config.SimulatorProperties;
import com.minimes.simulator.equipment.VirtualEquipment;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * 주기마다 설비별로 상태를 바꿔 보고, 센서값을 만들어 보낸다.
 * 일정 확률로 같은 메시지를 한 번 더 보내 서버의 중복 수신 처리를 확인한다 (S1, T3).
 */
@Component
public class SimulationScheduler {

    private final List<VirtualEquipment> equipments;
    private final CollectorClient client;
    private final SimulatorProperties properties;
    private final Clock clock;
    private final RandomGenerator random;

    public SimulationScheduler(CollectorClient client, SimulatorProperties properties,
                               Clock clock, RandomGenerator random) {
        // 모든 설비는 가동 상태로 시작하고, 시작 직후 현재 상태를 서버에 보내 맞춘다
        this.equipments = properties.equipments().stream()
                .map(code -> new VirtualEquipment(code, EquipmentStatus.RUNNING))
                .toList();
        this.client = client;
        this.properties = properties;
        this.clock = clock;
        this.random = random;
    }

    /** 시작하면 설비별 현재 상태를 먼저 보내 서버와 맞춘다 */
    @EventListener(ApplicationReadyEvent.class)
    public void sendInitialStatus() {
        Instant now = clock.instant();
        equipments.forEach(equipment -> client.sendStatus(equipment.currentStatus(now)));
    }

    @Scheduled(fixedRateString = "${simulator.interval}", initialDelayString = "${simulator.interval}")
    public void tick() {
        Instant now = clock.instant();
        for (VirtualEquipment equipment : equipments) {
            equipment.nextStatus(now, random, properties.transition()).ifPresent(status -> {
                client.sendStatus(status);
                if (shouldDuplicate()) {
                    client.sendStatus(status);
                }
            });

            List<SensorMessage> readings = equipment.nextReadings(now, random, properties.anomalyRate());
            client.sendReadings(readings);
            if (shouldDuplicate()) {
                client.sendReadings(readings);
            }
        }
    }

    private boolean shouldDuplicate() {
        return random.nextDouble() < properties.duplicateRate();
    }
}
