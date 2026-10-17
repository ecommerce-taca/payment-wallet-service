package com.taca.paymentwallet.infrastructure.config;

import com.taca.paymentwallet.application.port.in.RequestPayoutUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayoutContractGateTest {

    @Test
    void shouldNotProductionWireRequestPayoutUseCase() {
        boolean requestPayoutBeanExists = Arrays.stream(
                        ApplicationConfiguration.class.getDeclaredMethods()
                )
                .filter(method -> method.isAnnotationPresent(Bean.class))
                .map(Method::getReturnType)
                .anyMatch(RequestPayoutUseCase.class::isAssignableFrom);

        assertThat(requestPayoutBeanExists).isFalse();
    }

    @Test
    void shouldNotExposeSellerPayoutControllerBeforeContractResolution() {
        assertThatThrownBy(() ->
                Class.forName(
                        "com.taca.paymentwallet.presentation.rest.seller.SellerPayoutController"
                )
        ).isInstanceOf(ClassNotFoundException.class);
    }
}