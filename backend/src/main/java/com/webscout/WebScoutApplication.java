package com.webscout;

import com.webscout.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class WebScoutApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebScoutApplication.class, args);
	}
}