package com.smarthire.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    // HR dashboard fields
    private Long totalJobsPosted;
    private Long totalApplicants;
    private Long openJobs;
    private Long closedJobs;
    private Map<String, Long> pipelineFunnel;

    // Candidate dashboard fields
    private Long totalApplications;
    private Long activeApplications;
    private Long rejectedApplications;

    // Admin dashboard fields
    private Long totalUsers;
    private Long totalCandidates;
    private Long totalHRs;
    private Long totalJobs;
    private Long totalApplicationsSystem;
}