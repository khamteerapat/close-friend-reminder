package com.kt.cfreminder;

import com.linecorp.bot.messaging.model.SetWebhookEndpointRequest;
import com.linecorp.bot.webhook.model.Event;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CfreminderApplication {

	public static void main(String[] args) {
		SpringApplication.run(CfreminderApplication.class, args);

	}

}
