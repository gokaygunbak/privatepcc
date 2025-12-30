package com.pcc.interaction_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class InteractionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InteractionServiceApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.web.client.RestTemplate restTemplate() {
		return new org.springframework.web.client.RestTemplate();
	}
}
