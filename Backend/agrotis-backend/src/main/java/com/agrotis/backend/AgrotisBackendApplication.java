package com.agrotis.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgrotisBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgrotisBackendApplication.class, args);
    }
}
