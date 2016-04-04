package com.codeaim.urlcheck.probe.task;

import com.codeaim.urlcheck.common.model.Result;
import com.codeaim.urlcheck.common.repository.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.value;

@Component
public class ResultExpiryTask
{
    private static final Logger log = LoggerFactory.getLogger(ResultExpiryTask.class);

    @Autowired
    private ResultRepository resultRepository;

    @Value("${com.codeaim.urlcheck.probe.resultExistenceDuration:P7D}")
    private String resultExistenceDuration;

    public void run()
    {
        getExpiredResults()
                .parallelStream()
                .map(this::deleteExpiredResult)
                .collect(Collectors.toList())
                .forEach(resultId -> log.info("Result {} expiry complete", value("result-id", resultId)));
    }

    private List<Result> getExpiredResults()
    {
        return resultRepository
                .findExpired(now().minus(
                        Duration.parse(resultExistenceDuration)));
    }

    private Long deleteExpiredResult(Result result)
    {
        log.info("Result {} has expired, deleting result", value("result-id", result.getId()));

        resultRepository.save(resultRepository
                .findByPrevious(result.getId())
                .stream()
                .map(nextResult -> Result.buildFrom(nextResult)
                        .previous(null)
                        .build())
                .collect(Collectors.toList()));

        resultRepository.delete(result);
        return result.getId();
    }

    private LocalDateTime now()
    {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
