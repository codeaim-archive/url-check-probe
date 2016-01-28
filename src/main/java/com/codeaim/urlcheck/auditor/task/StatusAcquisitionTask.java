package com.codeaim.urlcheck.auditor.task;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.codeaim.urlcheck.auditor.model.Monitor;
import com.codeaim.urlcheck.auditor.model.MonitorEvent;
import com.codeaim.urlcheck.auditor.model.State;
import com.codeaim.urlcheck.auditor.model.Status;
import com.codeaim.urlcheck.auditor.repository.MonitorEventRepository;
import com.codeaim.urlcheck.auditor.repository.MonitorRepository;

@Component
public class StatusAcquisitionTask
{
    private static final Logger log = LoggerFactory.getLogger(StatusAcquisitionTask.class);

    @Autowired
    private MonitorRepository monitorRepository;
    @Autowired
    private MonitorEventRepository monitorEventRepository;

    @Value("${com.codeaim.urlcheck.auditor.isClustered ?:false}")
    private boolean isClustered;
    @Value("${com.codeaim.urlcheck.auditor.name ?:Standalone}")
    private String auditorName;

    public void run()
    {
        getElectableMonitors()
                .getContent()
                .stream()
                .map(this::markMonitorElected)
                .map(this::checkAndUpdateMonitor)
                .forEach(monitor -> log.info("Processing complete for auditor {}", monitor));
    }

    private Page<Monitor> getElectableMonitors()
    {
        log.info("Getting electable monitors");
        PageRequest pageRequest = new PageRequest(0, 5, new Sort(Sort.Direction.ASC, "audit"));
        return monitorRepository.findElectable(
                auditorName,
                isClustered,
                now(),
                pageRequest);
    }

    private Monitor markMonitorElected(Monitor monitor)
    {
        log.info("Marking auditor elected {}", monitor);

        return monitorRepository.save(Monitor
                .buildFrom(monitor)
                .state(State.ELECTED)
                .locked(now().plusMinutes(1))
                .auditor(auditorName)
                .build());
    }

    private Monitor checkAndUpdateMonitor(Monitor monitor)
    {
        MonitorEvent monitorEvent = getMonitorCheckEvent(monitor);

        if (monitorEvent.isChanged() && monitorEvent.isConfirmation())
            return monitorRepository.save(statusChangeConfirmed(monitor, monitorEvent));
        if (!monitorEvent.isChanged() && monitorEvent.isConfirmation())
            return monitorRepository.save(statusChangeConfirmationInconclusive(monitor, monitorEvent));
        if (monitorEvent.isChanged())
            return monitorRepository.save(statusChangeConfirmationRequired(monitor, monitorEvent));

        return monitorRepository.save(statusChangeNone(monitor, monitorEvent));
    }

    private MonitorEvent getMonitorCheckEvent(Monitor monitor)
    {
        log.info("Getting auditor event for auditor {}", monitor);
        MonitorEvent monitorEvent = requestUrlAndCreateMonitorEvent(monitor);
        log.info("Received auditor event {}", monitorEvent);
        return monitorEvent;
    }

    public MonitorEvent requestUrlAndCreateMonitorEvent(Monitor monitor)
    {
        try
        {
            long startResponseTime = System.currentTimeMillis();
            RestTemplate restTemplate = new RestTemplate();
            int statusCode = restTemplate
                .getForEntity(monitor.getUrl(), String.class)
                .getStatusCode()
                .value();
            return monitorEventRepository.save(MonitorEvent
                    .builder()
                    .monitorId(monitor.getId())
                    .previous(monitor.getMonitorEventId())
                    .auditor(monitor.getAuditor())
                    .responseTime(System.currentTimeMillis() - startResponseTime)
                    .statusCode(statusCode)
                    .status((statusCode >= 200 && statusCode <= 399) ? Status.UP : Status.DOWN)
                    .changed(!Objects.equals((statusCode >= 200 && statusCode <= 399) ? Status.UP : Status.DOWN, monitor.getStatus()))
                    .confirmation(monitor.isConfirming())
                    .build());
        } catch (Exception exception)
        {
            return monitorEventRepository.save(MonitorEvent
                    .builder()
                    .status(Status.ERROR)
                    .build());
        }
    }

    private Monitor statusChangeNone(Monitor monitor, MonitorEvent monitorEvent)
    {
        log.info("Updating auditor {} - No status change", monitor);

        return Monitor
                .buildFrom(monitor)
                .monitorEventId(monitorEvent.getId())
                .audit(now().plusMinutes(monitor.getInterval()))
                .state(State.WAITING)
                .locked(null)
                .build();
    }

    private Monitor statusChangeConfirmationRequired(Monitor monitor, MonitorEvent monitorEvent)
    {
        log.info("Updating auditor {} - Status change confirmation required", monitor);

        return Monitor
                .buildFrom(monitor)
                .monitorEventId(monitorEvent.getId())
                .confirming(true)
                .state(State.WAITING)
                .locked(null)
                .build();
    }

    private Monitor statusChangeConfirmationInconclusive(Monitor monitor, MonitorEvent monitorEvent)
    {
        log.info("Updating auditor {} - Status change confirmation inconclusive", monitor);

        return Monitor
                .buildFrom(monitor)
                .monitorEventId(monitorEvent.getId())
                .confirming(false)
                .state(State.WAITING)
                .locked(null)
                .build();
    }

    private Monitor statusChangeConfirmed(Monitor monitor, MonitorEvent monitorEvent)
    {
        log.info("Updating auditor {} - Confirmed status change ", monitor);

        return Monitor
                .buildFrom(monitor)
                .monitorEventId(monitorEvent.getId())
                .status(monitorEvent.getStatus())
                .confirming(false)
                .audit(now().plusMinutes(monitor.getInterval()))
                .state(State.WAITING)
                .locked(null)
                .build();
    }

    private LocalDateTime now()
    {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
