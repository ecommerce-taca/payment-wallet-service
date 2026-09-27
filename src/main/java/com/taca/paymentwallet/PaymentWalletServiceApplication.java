package com.taca.paymentwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;

@SpringBootApplication
@ConfigurationProperties
public class PaymentWalletServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentWalletServiceApplication.class, args);
	}

}
