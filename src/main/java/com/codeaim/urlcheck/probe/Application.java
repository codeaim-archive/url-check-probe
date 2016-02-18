package com.codeaim.urlcheck.probe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = "com.codeaim.urlcheck")
@EnableJpaRepositories(basePackages = "com.codeaim.urlcheck")
public class Application
{
    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class);
    }
}
