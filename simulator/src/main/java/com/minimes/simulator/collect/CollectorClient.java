package com.minimes.simulator.collect;

import com.minimes.common.message.SensorMessage;
import com.minimes.common.message.StatusMessage;
import com.minimes.simulator.config.SimulatorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * MES 서버 수집 API로 메시지를 보낸다. 실패하면 로그만 남기고 버린다.
 */
@Component
public class CollectorClient {

    private static final Logger log = LoggerFactory.getLogger(CollectorClient.class);

    private final RestClient restClient;
    private final SimulatorProperties properties;

    public CollectorClient(RestClient.Builder builder, SimulatorProperties properties) {
        this.restClient = builder.baseUrl(properties.serverUrl()).build();
        this.properties = properties;
    }

    /** 한 설비의 한 주기 센서값을 한 번에 보낸다 */
    public void sendReadings(List<SensorMessage> readings) {
        if (readings.isEmpty()) {
            return;
        }
        post(properties.sensorPath(), readings, readings.get(0).equipmentCode() + " 센서 " + readings.size() + "건");
    }

    public void sendStatus(StatusMessage status) {
        post(properties.statusPath(), status, status.equipmentCode() + " 상태 " + status.status()
                + " (message_id=" + status.messageId() + ")");
    }

    // TODO(5단계): 실패한 메시지 재시도·보관(Outbox, DLQ)
    private void post(String path, Object body, String description) {
        try {
            restClient.post()
                    .uri(path)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("전송 성공: {}", description);
        } catch (RestClientException e) {
            log.warn("전송 실패, 버림: {} - {}", description, e.getMessage());
        }
    }
}
