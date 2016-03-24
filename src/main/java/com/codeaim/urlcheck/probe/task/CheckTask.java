package com.codeaim.urlcheck.probe.task;

import static net.logstash.logback.argument.StructuredArguments.value;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

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

import com.codeaim.urlcheck.common.model.Check;
import com.codeaim.urlcheck.common.model.Result;
import com.codeaim.urlcheck.common.model.State;
import com.codeaim.urlcheck.common.model.Status;
import com.codeaim.urlcheck.common.repository.CheckRepository;
import com.codeaim.urlcheck.common.repository.ResultRepository;

@Component
public class CheckTask
{
    private static final Logger log = LoggerFactory.getLogger(CheckTask.class);

    @Autowired
    private CheckRepository checkRepository;
    @Autowired
    private ResultRepository resultRepository;

    @Value("${com.codeaim.urlcheck.probe.isClustered:false}")
    private boolean isClustered;
    @Value("${com.codeaim.urlcheck.probe.name:Standalone}")
    private String probeName;
    @Value("${com.codeaim.urlcheck.probe.task.checkTask.electionSize:10}")
    private int electionSize;

    private RestTemplate restTemplate = new RestTemplate();

    public void run()
    {
        markChecksElected(getElectableChecks().getContent())
                .parallelStream()
                .map(this::runAndUpdateCheck)
                .forEach(check -> log.info("Check {} complete", value("check-id", check.getId())));
    }

    private Page<Check> getElectableChecks()
    {
        log.info("Getting electable checks");
        PageRequest pageRequest = new PageRequest(0, electionSize, new Sort(Sort.Direction.ASC, "refresh"));
        return checkRepository.findElectable(
                probeName,
                isClustered,
                now(),
                pageRequest);
    }

    private Collection<Check> markChecksElected(Collection<Check> checks)
    {
        return checkRepository.save(checks
                .stream()
                .map(check -> {
                    log.info("Check {} marking elected", value("check-id", check.getId()));
                    return Check
                            .buildFrom(check)
                            .state(State.ELECTED)
                            .locked(now().plusMinutes(1))
                            .probe(probeName)
                            .build();
                })
                .collect(Collectors.toList()));
    }

    private Check runAndUpdateCheck(Check check)
    {
        Result result = getCheckResult(check);

        if (result.isChanged() && result.isConfirmation())
            return checkRepository.save(statusChangeConfirmed(check, result));
        if (!result.isChanged() && result.isConfirmation())
            return checkRepository.save(statusChangeConfirmationInconclusive(check, result));
        if (result.isChanged())
            return checkRepository.save(statusChangeConfirmationRequired(check, result));

        return checkRepository.save(statusChangeNone(check, result));
    }

    private Result getCheckResult(Check check)
    {
        log.info("Check {} getting result", value("check-id", check.getId()));
        Result result = requestUrlAndCreateResult(check);
        log.info("Check {} received result {}",
                value("check-id", check.getId()),
                value("result-id", result.getId()),
                value("check-name", check.getName()),
                value("check-url", check.getUrl()),
                value("result-response-time", result.getResponseTime()),
                value("result-status-code", result.getStatusCode()),
                value("result-probe", result.getProbe()));
        return result;
    }

    public Result requestUrlAndCreateResult(Check check)
    {
        long startResponseTime = System.currentTimeMillis();
        int statusCode = requestUrlStatus(check);
        return resultRepository.save(Result
                .builder()
                .check(check)
                .previous(check.getLatestResult())
                .probe(check.getProbe())
                .responseTime(System.currentTimeMillis() - startResponseTime)
                .statusCode(statusCode)
                .status((statusCode >= 200 && statusCode <= 399) ? Status.UP : Status.DOWN)
                .changed(!Objects.equals((statusCode >= 200 && statusCode <= 399) ? Status.UP : Status.DOWN, check.getStatus()))
                .confirmation(check.isConfirming())
                .build());
    }

    private int requestUrlStatus(Check check)
    {
        try
        {
            log.info("Check {} requesting url {}", value("check-id", check.getId()), value("check-url", check.getUrl()));

            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");

            return restTemplate.exchange(check.getUrl(), HttpMethod.HEAD, new HttpEntity<>("", headers), String.class)
                    .getStatusCode()
                    .value();
        } catch (HttpClientErrorException exception)
        {
            log.warn("Check {} received http error requesting url {} - Exception {}", value("check-id", check.getId()), value("check-url", check.getUrl()), value("check-exception", exception.getMessage()));
            return exception.getStatusCode().value();
        } catch (Exception exception)
        {
            log.error("Check {} received exception requesting url {} - Exception {}", value("check-id", check.getId()), value("check-url", check.getUrl()), value("check-exception", exception.getMessage()));
            return 500;
        }
    }

    private Check statusChangeNone(Check check, Result result)
    {
        log.info("Check {} no status change, updating check", value("check-id", check.getId()));

        return Check
                .buildFrom(check)
                .latestResult(result)
                .refresh(now().plusMinutes(check.getInterval()))
                .state(State.WAITING)
                .locked(null)
                .build();
    }

    private Check statusChangeConfirmationRequired(Check check, Result result)
    {
        log.info("Check {} status change confirmation required, updating check", value("check-id", check.getId()));

        return Check
                .buildFrom(check)
                .latestResult(result)
                .confirming(true)
                .state(State.WAITING)
                .locked(null)
                .build();
    }

    private Check statusChangeConfirmationInconclusive(Check check, Result result)
    {
        log.info("Check {} status change confirmation inconclusive, updating check", value("check-id", check.getId()));

        return Check
                .buildFrom(check)
                .latestResult(result)
                .confirming(false)
                .state(State.WAITING)
                .locked(null)
                .build();
    }

    private Check statusChangeConfirmed(Check check, Result result)
    {
        log.info("Check {} confirmed status change, updating check", value("check-id", check.getId()));

        return Check
                .buildFrom(check)
                .latestResult(result)
                .status(result.getStatus())
                .confirming(false)
                .refresh(now().plusMinutes(check.getInterval()))
                .state(State.WAITING)
                .locked(null)
                .build();
    }

    private LocalDateTime now()
    {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
