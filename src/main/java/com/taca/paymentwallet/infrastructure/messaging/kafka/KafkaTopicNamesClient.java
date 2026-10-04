package com.taca.paymentwallet.infrastructure.messaging.kafka;

import java.time.Duration;
import java.util.Set;

public interface KafkaTopicNamesClient {

    Set<String> listTopicNames(Duration timeout);
}