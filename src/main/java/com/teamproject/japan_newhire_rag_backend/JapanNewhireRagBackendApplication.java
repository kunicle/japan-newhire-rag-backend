package com.teamproject.japan_newhire_rag_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JapanNewhireRagBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JapanNewhireRagBackendApplication.class, args);
	}

}
