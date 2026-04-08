package com.microbiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MicrobizProApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicrobizProApplication.class, args);
	}

}
