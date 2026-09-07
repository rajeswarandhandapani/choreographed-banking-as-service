package com.rajeswaran.common.messaging;

/** Application port: event handlers do not depend on the Kafka transport API. */
public interface EventPublisher {
    void publish(String binding, Object event);
}
