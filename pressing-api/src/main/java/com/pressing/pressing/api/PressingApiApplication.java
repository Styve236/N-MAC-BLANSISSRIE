package com.pressing.pressing.api;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;



@SpringBootApplication
@ConfigurationPropertiesScan
public class PressingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PressingApiApplication.class, args);
	}

}
