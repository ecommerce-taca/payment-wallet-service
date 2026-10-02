package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.port.in.ProcessInboxEventUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConsumerConfiguration {

    @Bean
    KafkaInboundMetadataExtractor kafkaInboundMetadataExtractor() {
        return new KafkaInboundMetadataExtractor();
    }

    @Bean
    KafkaPayloadHasher kafkaPayloadHasher() {
        return new KafkaPayloadHasher();
    }

    @Bean
    KafkaInboxProcessor kafkaInboxProcessor(
            ProcessInboxEventUseCase processInboxEventUseCase,
            KafkaInboundMetadataExtractor metadataExtractor,
            KafkaPayloadHasher payloadHasher
    ) {
        return new KafkaInboxProcessor(
                processInboxEventUseCase,
                metadataExtractor,
                payloadHasher
        );
    }
}