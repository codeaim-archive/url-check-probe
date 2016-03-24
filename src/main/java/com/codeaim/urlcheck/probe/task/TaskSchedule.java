package com.codeaim.urlcheck.probe.task;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class TaskSchedule
{
    private static final Logger log = LoggerFactory.getLogger(TaskSchedule.class);

    @Autowired
    private CheckTask checkTask;
    @Autowired
    private ResultExpiryTask resultExpiryTask;

    @Value("${com.codeaim.urlcheck.probe.taskSchedulerPoolSize:2}")
    private int taskSchedulerPoolSize;

    @Bean
    public  ThreadPoolTaskScheduler  taskScheduler(){
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(taskSchedulerPoolSize);
        return taskScheduler;
    }

    @Scheduled(fixedRate = 2000)
    public void CheckTask()
    {
        long startResponseTime = System.currentTimeMillis();
        log.info("Starting check task {}", LocalDateTime.now(ZoneOffset.UTC));
        checkTask.run();
        log.info("Completed check task {}", LocalDateTime.now(ZoneOffset.UTC));
        log.info("Check task time taken: {}", System.currentTimeMillis() - startResponseTime);
    }

    @Scheduled(fixedRate = 300000)
    public void ResultExpiryTask()
    {
        long startResponseTime = System.currentTimeMillis();
        log.info("Starting result expiry task {}", LocalDateTime.now(ZoneOffset.UTC));
        resultExpiryTask.run();
        log.info("Completed result expiry task {}", LocalDateTime.now(ZoneOffset.UTC));
        log.info("Result expiry task time taken: {}", System.currentTimeMillis() - startResponseTime);
    }
}
