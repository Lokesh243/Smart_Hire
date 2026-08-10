package com.smarthire.controller;

import com.smarthire.dto.request.JobRequest;
import com.smarthire.dto.response.JobResponse;
import com.smarthire.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    // ── Create job ─────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                jobService.createJob(request, userDetails.getUsername()));
    }

    // ── List all open jobs (public, paginated, filterable) ─────────
    @GetMapping
    public ResponseEntity<Page<JobResponse>> getAllJobs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(
                jobService.getAllJobs(title, location, jobType, pageable));
    }

    // ── Get job by id ──────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    // ── Update job ─────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                jobService.updateJob(id, request, userDetails.getUsername()));
    }

    // ── Close job ──────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> closeJob(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        jobService.closeJob(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Job closed successfully"));
    }

    // ── My posted jobs (HR) ────────────────────────────────────────
    @GetMapping("/my-jobs")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<List<JobResponse>> getMyJobs(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                jobService.getMyJobs(userDetails.getUsername()));
    }
}