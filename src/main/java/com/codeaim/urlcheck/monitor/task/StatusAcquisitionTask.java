package com.codeaim.urlcheck.monitor.task;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.codeaim.urlcheck.monitor.model.Monitor;
import com.codeaim.urlcheck.monitor.model.MonitorEvent;
import com.codeaim.urlcheck.monitor.model.State;
import com.codeaim.urlcheck.monitor.model.Status;
import com.codeaim.urlcheck.monitor.repository.MonitorEventRepository;
import com.codeaim.urlcheck.monitor.repository.MonitorRepository;

@Component
public class StatusAcquisitionTask
{
    private static final Logger log = LoggerFactory.getLogger(StatusAcquisitionTask.class);

    @Autowired
    private MonitorRepository monitorRepository;
    @Autowired
    private MonitorEventRepository monitorEventRepository;

    @Value("${com.codeaim.urlcheck.scheduler.isClustered}")
    private boolean isClustered;
    @Value("${com.codeaim.urlcheck.scheduler.name}")
    private String schedulerName;

    public void run()
    {
        getElectableMonitors()
                .getContent()
                .stream()
                .map(this::markMonitorElected)
                .map(this::checkAndUpdateMonitor)
                .forEach(monitor -> log.info("Processing complete for monitor {}", monitor));
    }

    private Page<Monitor> getElectableMonitors()
    {
        log.info("Getting electable monitors");
        PageRequest pageRequest = new PageRequest(0, 5, new Sort(Sort.Direction.ASC, "audit"));
        return monitorRepository.findElectable(
                schedulerName,
                isClustered,
                now(),
                pageRequest);
    }

    private Monitor markMonitorElected(Monitor monitor)
    {
        log.info("Marking monitor elected {}", monitor);

        return monitorRepository.save(Monitor
                .buildFrom(monitor)
                .state(State.ELECTED)
                .locked(now().plusMinutes(1))
                .scheduler(schedulerName)
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
        log.info("Getting monitor event for monitor {}", monitor);
        MonitorEvent monitorEvent = requestUrlAndCreateMonitorEvent(monitor);
        log.info("Received monitor event {}", monitorEvent);
        return monitorEvent;
    }

    public MonitorEvent requestUrlAndCreateMonitorEvent(Monitor monitor)
    {
        try
        {
            long startResponseTime = System.currentTimeMillis();
            int statusCode = HttpClients
                    .createDefault()
                    .execute(new HttpHead(monitor.getUrl()))
                    .getStatusLine()
                    .getStatusCode();
            return monitorEventRepository.save(MonitorEvent
                    .builder()
                    .monitorId(monitor.getId())
                    .previous(monitor.getMonitorEventId())
                    .scheduler(monitor.getScheduler())
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
        log.info("Updating monitor {} - No status change", monitor);

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
        log.info("Updating monitor {} - Status change confirmation required", monitor);

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
        log.info("Updating monitor {} - Status change confirmation inconclusive", monitor);

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
        log.info("Updating monitor {} - Confirmed status change ", monitor);

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
