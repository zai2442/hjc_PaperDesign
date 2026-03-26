package com.campus.activity;

import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.mapper.ActivityMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.util.List;

@SpringBootApplication
@Profile("scratch")
public class DBCheckScript {
    public static void main(String[] args) {
        SpringApplication.run(DBCheckScript.class, args);
    }

    @Bean
    public CommandLineRunner runner(ActivityMapper mapper) {
        return args -> {
            System.out.println("--- DUMPING ALL ACTIVITIES ---");
            List<Activity> activities = mapper.selectList(null);
            for (Activity a : activities) {
                System.out.println("ID: " + a.getId() + ", Title: " + a.getTitle() + ", Status: " + a.getStatus() + ", PublishAt: " + a.getPublishAt());
            }
            System.out.println("--- END OF DUMP ---");
            System.exit(0);
        };
    }
}
