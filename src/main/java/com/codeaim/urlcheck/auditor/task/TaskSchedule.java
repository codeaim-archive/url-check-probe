package com.codeaim.urlcheck.auditor.task;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskSchedule
{
    private static final Logger log = LoggerFactory.getLogger(TaskSchedule.class);

    @Autowired
    private StatusAcquisitionTask statusAcquisitionTask;
    @Autowired
    private MonitorEventExpiryTask monitorEventExpiryTask;

    @Scheduled(fixedRate = 2000)
    public void StatusAcquisitionTask()
    {
        long startResponseTime = System.currentTimeMillis();
        log.info("Starting StatusAcquisitionTask {}", LocalDateTime.now(ZoneOffset.UTC));
        statusAcquisitionTask.run();
        log.info("Completed StatusAcquisitionTask {}", LocalDateTime.now(ZoneOffset.UTC));
        log.error("StatusAcquisitionTask time taken: {}", System.currentTimeMillis() - startResponseTime);
    }

    @Scheduled(fixedRate = 300000)
    public void MonitorEventExpiryTask()
    {
        long startResponseTime = System.currentTimeMillis();
        log.info("Starting MonitorEventExpiryTask {}", LocalDateTime.now(ZoneOffset.UTC));
        monitorEventExpiryTask.run();
        log.info("Completed MonitorEventExpiryTask {}", LocalDateTime.now(ZoneOffset.UTC));
        log.info("MonitorEventExpiryTask time taken: {}", System.currentTimeMillis() - startResponseTime);
    }
}
