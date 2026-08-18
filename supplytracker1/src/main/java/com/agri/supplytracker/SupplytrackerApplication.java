package com.agri.supplytracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SupplytrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupplytrackerApplication.class, args);
    }
}
