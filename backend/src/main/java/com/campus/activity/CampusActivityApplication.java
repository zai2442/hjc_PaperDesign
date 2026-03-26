package com.campus.activity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan({"com.campus.activity.user.mapper", "com.campus.activity.activity.mapper"})
@EnableScheduling
public class CampusActivityApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusActivityApplication.class, args);
    }
}
