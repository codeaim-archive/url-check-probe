package com.codeaim.urlcheck.auditor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories
public class Application
{
    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class);
    }
}
