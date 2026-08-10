package com.smarthire.scheduler;

import com.smarthire.enums.JobStatus;
import com.smarthire.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobExpiryScheduler {

    private final JobRepository jobRepository;

    // Runs every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void expireOldJobs() {

        var expiredJobs = jobRepository
                .findByStatusAndExpiresAtBefore(
                        JobStatus.OPEN,
                        LocalDate.now());

        if (expiredJobs.isEmpty()) {
            log.debug("No jobs to expire today");
            return;
        }

        expiredJobs.forEach(job -> {
            job.setStatus(JobStatus.EXPIRED);
            log.info("Job expired: {} ({})", job.getId(), job.getTitle());
        });

        jobRepository.saveAll(expiredJobs);
        log.info("Expired {} jobs", expiredJobs.size());
    }
}