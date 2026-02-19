package com.kt.cfreminder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CfreminderApplication {

	public static void main(String[] args) {
		SpringApplication.run(CfreminderApplication.class, args);

	}

}
