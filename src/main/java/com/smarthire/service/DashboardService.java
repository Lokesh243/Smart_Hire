package com.smarthire.service;

import com.smarthire.dto.response.DashboardResponse;
import com.smarthire.entity.User;
import com.smarthire.enums.ApplicationStatus;
import com.smarthire.enums.JobStatus;
import com.smarthire.enums.Role;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    // ── HR Dashboard ───────────────────────────────────────────────
    public DashboardResponse getHrDashboard(String hrEmail) {

        User hr = userRepository.findByEmail(hrEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var myJobs = jobRepository.findByPostedBy(hr);
        long totalJobs = myJobs.size();
        long openJobs = myJobs.stream()
                .filter(j -> j.getStatus() == JobStatus.OPEN).count();
        long closedJobs = myJobs.stream()
                .filter(j -> j.getStatus() != JobStatus.OPEN).count();

        long totalApplicants = myJobs.stream()
                .mapToLong(job -> applicationRepository.countByJobId(job.getId()))
                .sum();

        // Pipeline funnel — count per stage
        var allApplications = myJobs.stream()
                .flatMap(job -> applicationRepository.findByJob(job).stream())
                .toList();

        Map<String, Long> funnel = Map.of(
                "APPLIED",   countByStatus(allApplications, ApplicationStatus.APPLIED),
                "SCREENED",  countByStatus(allApplications, ApplicationStatus.SCREENED),
                "INTERVIEW", countByStatus(allApplications, ApplicationStatus.INTERVIEW),
                "OFFER",     countByStatus(allApplications, ApplicationStatus.OFFER),
                "REJECTED",  countByStatus(allApplications, ApplicationStatus.REJECTED)
        );

        log.debug("HR dashboard generated for {}", hrEmail);

        return DashboardResponse.builder()
                .totalJobsPosted(totalJobs)
                .openJobs(openJobs)
                .closedJobs(closedJobs)
                .totalApplicants(totalApplicants)
                .pipelineFunnel(funnel)
                .build();
    }

    // ── Candidate Dashboard ────────────────────────────────────────
    public DashboardResponse getCandidateDashboard(String candidateEmail) {

        User candidate = userRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var myApplications = applicationRepository.findByCandidate(candidate);
        long total = myApplications.size();
        long active = myApplications.stream()
                .filter(a -> a.getStatus() != ApplicationStatus.REJECTED
                        && a.getStatus() != ApplicationStatus.OFFER)
                .count();
        long rejected = countByStatus(myApplications, ApplicationStatus.REJECTED);

        return DashboardResponse.builder()
                .totalApplications(total)
                .activeApplications(active)
                .rejectedApplications(rejected)
                .build();
    }

    // ── Admin Dashboard ────────────────────────────────────────────
    public DashboardResponse getAdminDashboard() {

        long totalUsers       = userRepository.count();
        long totalCandidates  = userRepository.findByRole(Role.CANDIDATE).size();
        long totalHRs         = userRepository.findByRole(Role.HR).size();
        long totalJobs        = jobRepository.count();
        long totalApplications= applicationRepository.count();

        return DashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalCandidates(totalCandidates)
                .totalHRs(totalHRs)
                .totalJobs(totalJobs)
                .totalApplicationsSystem(totalApplications)
                .build();
    }

    // ── Helper ─────────────────────────────────────────────────────
    private long countByStatus(java.util.List<com.smarthire.entity.Application> apps,
                               ApplicationStatus status) {
        return apps.stream()
                .filter(a -> a.getStatus() == status)
                .count();
    }
}