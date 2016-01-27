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
    @Autowired
    private static final Logger log = LoggerFactory.getLogger(TaskSchedule.class);

    @Autowired
    private StatusAcquisitionTask statusAcquisitionTask;

    @Scheduled(fixedRate = 1000)
    public void StatusAcquisitionTask()
    {
        log.info("Starting StatusAcquisitionTask {}", LocalDateTime.now(ZoneOffset.UTC));
        statusAcquisitionTask.run();
    }
}
