package com.medicare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedicareBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicareBackendApplication.class, args);
    }
}
