package com.faiq.navan_billing_service;

import org.springframework.boot.SpringApplication;

public class TestNavanBillingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(NavanBillingServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
