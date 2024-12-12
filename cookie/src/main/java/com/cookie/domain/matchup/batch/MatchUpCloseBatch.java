package com.cookie.domain.matchup.batch;

import com.cookie.domain.matchup.dto.response.MatchUpCloseResponse;
import com.cookie.domain.matchup.service.MatchUpService;
import com.cookie.domain.reward.service.RewardPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class MatchUpCloseBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RewardPointService rewardPointService;
    private final MatchUpService matchUpService;

    @Bean public Job matchUpCloseJob(@Qualifier("matchUpCloseStep") Step matchUpCloseStep) {
        return new JobBuilder("matchUpCloseJob", jobRepository)
                .start(matchUpCloseStep)
                .build();
    }

    @Bean
    public Step matchUpCloseStep() {
        return new StepBuilder("matchUpCloseStep", jobRepository)
                .<MatchUpCloseResponse, Void>chunk(100, transactionManager)
                .reader(matchUpUserCloseItemReader())
                .processor(matchUserCloseProcessor())
                .writer(matchUserCloseWriter())
                .faultTolerant()
                .retryLimit(2)
                .retry(Exception.class)
                .build();
    }

    @Bean
    public ItemReader<MatchUpCloseResponse> matchUpUserCloseItemReader() {
        return new ItemReader<>() {
            private Iterator<MatchUpCloseResponse> userIterator;

            @Override
            public MatchUpCloseResponse read() {

                if (userIterator == null) {
                    List<MatchUpCloseResponse> winUsers = matchUpService.expireAndReturnMatchUps();
                    userIterator = winUsers.iterator();
                }

                if (userIterator.hasNext()) {
                    return userIterator.next();
                }

                return null;
            }
        };
    }

    @Bean
    public ItemProcessor<MatchUpCloseResponse, Void> matchUserCloseProcessor() {
        return response -> {
            rewardPointService.updateBadgePointAndBadgeObtain(response.getUser(), "MatchUp", response.getMovieName());
            return null;
        };
    }

    @Bean
    public ItemWriter<Void> matchUserCloseWriter() {
        return item -> {
            // Do nothing
        };
    }
}
