package com.cmpe172.fitness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FitnessBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitnessBookingApplication.class, args);
    }
}
