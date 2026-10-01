package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {

    @Bean
    NewTopic paymentEventsTopic(KafkaTopicProperties properties) {
        return TopicBuilder.name(properties.paymentEvents())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic walletEventsTopic(KafkaTopicProperties properties) {
        return TopicBuilder.name(properties.walletEvents())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic outboxDlqTopic(KafkaTopicProperties properties) {
        return TopicBuilder.name(properties.outboxDlq())
                .partitions(3)
                .replicas(1)
                .build();
    }
}