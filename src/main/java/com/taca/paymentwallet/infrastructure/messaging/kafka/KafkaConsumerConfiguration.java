package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.port.in.ProcessInboxEventUseCase;
import com.taca.paymentwallet.application.observability.W3cTraceContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConsumerConfiguration {

    @Bean
    KafkaInboundMetadataExtractor kafkaInboundMetadataExtractor(
            W3cTraceContext traceContext
    ) {
        return new KafkaInboundMetadataExtractor(traceContext);
    }

    @Bean
    KafkaPayloadHasher kafkaPayloadHasher() {
        return new KafkaPayloadHasher();
    }

    @Bean
    KafkaInboxProcessor kafkaInboxProcessor(
            ProcessInboxEventUseCase processInboxEventUseCase,
            KafkaInboundMetadataExtractor metadataExtractor,
            KafkaPayloadHasher payloadHasher,
            KafkaConsumerObservation observation
    ) {
        return new KafkaInboxProcessor(
                processInboxEventUseCase,
                metadataExtractor,
                payloadHasher,
                observation
        );
    }

    @Bean
    KafkaConsumerObservation kafkaConsumerObservation(
            MeterRegistry meterRegistry
    ) {
        return new KafkaConsumerObservation(meterRegistry);
    }
}