package com.rajeswaran.common.components;

import com.rajeswaran.common.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StreamEventPublisher implements EventPublisher {
    private final StreamBridge bridge;
    @Override
    public void publish(String binding, Object event) {
        if (!bridge.send(binding, event)) {
            throw new IllegalStateException("Event publication rejected for " + binding);
        }
    }
}
