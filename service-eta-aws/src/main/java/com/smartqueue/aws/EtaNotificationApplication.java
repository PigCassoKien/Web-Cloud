package com.smartqueue.aws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EtaNotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(EtaNotificationApplication.class, args);
    }
}