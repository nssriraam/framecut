package com.framecut;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FrameCutApplication {
    public static void main(String[] args) {
        SpringApplication.run(FrameCutApplication.class, args);
    }
}
