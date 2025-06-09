package com.ai.hackathon.error_resolution_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"com.ai.hackathon.error_resolution_app.config"
		}
)
public class SpringAiAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiAppApplication.class, args);
	}

}
