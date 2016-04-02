package com.codeaim.urlcheck.probe.task;

import static net.logstash.logback.argument.StructuredArguments.value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.codeaim.urlcheck.common.model.Result;
import com.codeaim.urlcheck.common.repository.ResultRepository;

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
                .forEach(resultId -> log.info("Result {} expiry complete", value("result-id", resultId)));
    }

    private List<Result> getExpiredResults()
    {
        return resultRepository
                .findExpired(now().minus(Duration.parse(resultExistenceDuration)));
    }

    private Long deleteExpiredResult(Result result)
    {
        log.info("Result {} has expired, deleting result", value("result-id", result.getId()));
        resultRepository.save(
                Result.buildFrom(
                        resultRepository.findByPrevious(result))
                        .previous(null)
                        .build());
        resultRepository.delete(result);
        return result.getId();
    }

    private LocalDateTime now()
    {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
