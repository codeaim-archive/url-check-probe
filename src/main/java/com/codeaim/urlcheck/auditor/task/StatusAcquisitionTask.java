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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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

    @Value("${com.codeaim.urlcheck.auditor.isClustered:false}")
    private boolean isClustered;
    @Value("${com.codeaim.urlcheck.auditor.name:Standalone}")
    private String auditorName;

    public void run()
    {
        getElectableMonitors()
                .getContent()
                .stream()
                .map(this::markMonitorElected)
                .map(this::checkAndUpdateMonitor)
                .forEach(monitor -> log.info("Monitor {} status acquisition complete - {}",monitor.getId(), monitor));
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
        log.info("Monitor {} marking elected - {}", monitor.getId(), monitor);

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
        log.info("Monitor {} getting monitor event - {}", monitor.getId(), monitor);
        MonitorEvent monitorEvent = requestUrlAndCreateMonitorEvent(monitor);
        log.info("Monitor {} received monitor event - {}", monitor.getId(), monitorEvent);
        return monitorEvent;
    }

    public MonitorEvent requestUrlAndCreateMonitorEvent(Monitor monitor)
    {
            long startResponseTime = System.currentTimeMillis();
            int statusCode = requestUrlStatus(monitor);
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
    }

    private int requestUrlStatus(Monitor monitor)
    {
        try
        {
            log.debug("Monitor {} requesting url {}", monitor.getId(), monitor.getUrl());

            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");

            return new RestTemplate().exchange(monitor.getUrl(), HttpMethod.GET, new HttpEntity<>("", headers), String.class)
                    .getStatusCode()
                    .value();
        } catch (HttpClientErrorException exception)
        {
            log.warn("Monitor {} received http error requesting url {} - {}", monitor.getId(), monitor.getUrl(), exception.getMessage());
            return exception.getStatusCode().value();
        } catch (Exception exception)
        {
            log.error("Monitor {} received exception requesting url {} - {}", monitor.getId(), monitor.getUrl(), exception.getMessage());
            return 500;
        }
    }

    private Monitor statusChangeNone(Monitor monitor, MonitorEvent monitorEvent)
    {
        log.info("Monitor {} no status change, updating monitor - {}", monitor.getId(), monitor);

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
        log.info("Monitor {} status change confirmation required, updating monitor - {}", monitor.getId(), monitor);

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
        log.info("Monitor {} status change confirmation inconclusive, updating monitor - {}", monitor.getId(), monitor);

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
        log.info("Monitor {} confirmed status change, updating monitor - {}", monitor.getId(), monitor);

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
