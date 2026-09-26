package com.li.lwg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LwgApplication {

    public static void main(String[] args) {
        SpringApplication.run(LwgApplication.class, args);
    }

}
