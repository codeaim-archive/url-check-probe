package com.codeaim.urlcheck.auditor.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.codeaim.urlcheck.common.model.MonitorEvent;
import com.codeaim.urlcheck.common.repository.MonitorEventRepository;

@Component
public class MonitorEventExpiryTask
{
    private static final Logger log = LoggerFactory.getLogger(MonitorEventExpiryTask.class);

    @Autowired
    private MonitorEventRepository monitorEventRepository;

    @Value("${com.codeaim.urlcheck.auditor.monitorEventExistenceDuration:P7D}")
    private String monitorEventExistenceDuration;

    public void run()
    {
        getExpiredMonitorEvents()
                .stream()
                .map(this::deleteExpiredMonitorEvent)
                .forEach(monitorEventId -> log.info("Monitor event {} expiry complete", monitorEventId));
    }

    private List<MonitorEvent> getExpiredMonitorEvents()
    {
        return monitorEventRepository
                .findExpiredMonitorEvents(now().minus(Duration.parse(monitorEventExistenceDuration)));
    }

    private Long deleteExpiredMonitorEvent(MonitorEvent monitorEvent)
    {
        log.error("Monitor event {} has expired, deleting monitor event", monitorEvent.getId());
        monitorEventRepository.delete(monitorEvent);
        return monitorEvent.getId();
    }

    private LocalDateTime now()
    {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
