package com.smartdoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmartDocApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartDocApplication.class, args);
    }
}
