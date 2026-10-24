package com.minimes.simulator.equipment;

import com.minimes.common.equipment.EquipmentStatus;
import com.minimes.common.equipment.Metric;
import com.minimes.common.message.SensorMessage;
import com.minimes.common.message.StatusMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * 가상 설비 한 대. 상태를 바꾸고 센서값을 만든다 (Spring 의존 없음, 단위 테스트 대상).
 *
 * <ul>
 *   <li>온도는 상태별 목표값(가동 62℃, 그 외 30℃)으로 조금씩 다가가고, 잡음이 섞인다</li>
 *   <li>진동은 가동 중 2.0mm/s 안팎, 멈추면 0.2mm/s 안팎</li>
 *   <li>COUNT는 이번 주기에 만든 개수(누적값이 아님). 가동 중일 때만 보낸다 (실적 counted_qty 집계 대상, P4)</li>
 *   <li>이상값은 4단계 이상 감지 테스트용으로, 알람 기준(85℃, 6.0mm/s)을 넘는 값을 섞는다</li>
 * </ul>
 */
public class VirtualEquipment {

    static final double RUNNING_TEMPERATURE = 62.0;
    static final double STOPPED_TEMPERATURE = 30.0;
    static final double ANOMALY_TEMPERATURE = 92.0;
    static final double ANOMALY_VIBRATION = 7.5;
    /** 한 주기에 목표 온도로 다가가는 비율 */
    private static final double HEATING_RATE = 0.2;
    private static final int MAX_COUNT_PER_TICK = 3;

    private final String code;
    private EquipmentStatus status;
    private double temperature;

    public VirtualEquipment(String code, EquipmentStatus initialStatus) {
        this.code = code;
        this.status = initialStatus;
        this.temperature = targetTemperature(initialStatus);
    }

    /** 시뮬레이터가 켜질 때 서버와 상태를 맞추기 위해 보내는 현재 상태 */
    public StatusMessage currentStatus(Instant now) {
        return StatusMessage.of(code, status, now);
    }

    /**
     * 확률에 따라 상태를 바꾼다.
     *
     * @return 상태가 바뀐 경우에만 상태 메시지
     */
    public Optional<StatusMessage> nextStatus(Instant now, RandomGenerator random, TransitionRule rule) {
        EquipmentStatus next = pickNextStatus(random.nextDouble(), rule);
        if (next == status) {
            return Optional.empty();
        }
        status = next;
        return Optional.of(StatusMessage.of(code, next, now));
    }

    private EquipmentStatus pickNextStatus(double roll, TransitionRule rule) {
        return switch (status) {
            case RUNNING -> roll < rule.runToDown() ? EquipmentStatus.DOWN
                    : roll < rule.runToDown() + rule.runToIdle() ? EquipmentStatus.IDLE
                    : EquipmentStatus.RUNNING;
            case IDLE -> roll < rule.idleToDown() ? EquipmentStatus.DOWN
                    : roll < rule.idleToDown() + rule.idleToRun() ? EquipmentStatus.RUNNING
                    : EquipmentStatus.IDLE;
            case DOWN -> roll < rule.downToRun() ? EquipmentStatus.RUNNING : EquipmentStatus.DOWN;
        };
    }

    /**
     * 이번 주기의 센서값. 측정값 1건 = 메시지 1건이고, 각각 새 message_id를 받는다.
     *
     * @param anomalyRate 이상값을 섞을 확률 (0.0 ~ 1.0)
     */
    public List<SensorMessage> nextReadings(Instant now, RandomGenerator random, double anomalyRate) {
        boolean anomaly = random.nextDouble() < anomalyRate;
        boolean running = status == EquipmentStatus.RUNNING;

        temperature += (targetTemperature(status) - temperature) * HEATING_RATE;
        double temperatureValue = anomaly ? ANOMALY_TEMPERATURE : temperature + noise(random, 0.5);
        double vibrationValue = anomaly ? ANOMALY_VIBRATION
                : running ? 2.0 + noise(random, 0.3) : 0.2 + noise(random, 0.05);

        List<SensorMessage> readings = new ArrayList<>(3);
        readings.add(SensorMessage.of(code, Metric.TEMPERATURE, round(temperatureValue), now));
        readings.add(SensorMessage.of(code, Metric.VIBRATION, round(vibrationValue), now));
        if (running) {
            int count = 1 + random.nextInt(MAX_COUNT_PER_TICK);
            readings.add(SensorMessage.of(code, Metric.COUNT, count, now));
        }
        return readings;
    }

    private static double targetTemperature(EquipmentStatus status) {
        return status == EquipmentStatus.RUNNING ? RUNNING_TEMPERATURE : STOPPED_TEMPERATURE;
    }

    /** -amplitude ~ +amplitude 범위의 잡음 */
    private static double noise(RandomGenerator random, double amplitude) {
        return (random.nextDouble() * 2 - 1) * amplitude;
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }

    public String getCode() {
        return code;
    }

    public EquipmentStatus getStatus() {
        return status;
    }
}
