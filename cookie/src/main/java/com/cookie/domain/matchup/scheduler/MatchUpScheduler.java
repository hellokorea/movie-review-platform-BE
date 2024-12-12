package com.cookie.domain.matchup.scheduler;

import com.cookie.domain.matchup.service.MatchUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchUpScheduler {
    private final MatchUpService matchUpService;

    private final JobLauncher jobLauncher;
    private final Job matchUpCloseJob;

    @Scheduled(cron = "0 0 0 ? * MON")
    public void activateMatchUp() {
        log.info("Activating match-up: Setting PENDING to NOW");
        matchUpService.updateMatchUpStatusToNow();
    }

    @Scheduled(cron = "0 0 0 ? * SUN")
    public void expireMatchUp() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(matchUpCloseJob ,jobParameters);
            log.info("매치 업 이벤트 종료 정상 처리");

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
