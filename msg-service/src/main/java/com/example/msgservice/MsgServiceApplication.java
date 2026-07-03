package com.example.msgservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MsgServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsgServiceApplication.class, args);
    }
}
