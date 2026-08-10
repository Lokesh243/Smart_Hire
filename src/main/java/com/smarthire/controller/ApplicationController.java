package com.smarthire.controller;

import com.smarthire.dto.request.ApplicationRequest;
import com.smarthire.dto.request.PipelineUpdateRequest;
import com.smarthire.dto.response.ApplicationResponse;
import com.smarthire.dto.response.PipelineStageResponse;
import com.smarthire.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // ── Apply to job ───────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAuthority('CANDIDATE')")
    public ResponseEntity<ApplicationResponse> apply(
            @Valid @RequestBody ApplicationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                applicationService.apply(request, userDetails.getUsername()));
    }

    // ── My applications ────────────────────────────────────────────
    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('CANDIDATE')")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                applicationService.getMyApplications(userDetails.getUsername()));
    }

    // ── Get application by id ──────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN', 'CANDIDATE')")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable Long id) {

        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    // ── Get applicants for a job ───────────────────────────────────
    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> getApplicantsByJob(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(applicationService.getApplicantsByJob(jobId));
    }

    // ── Move pipeline stage ────────────────────────────────────────
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PipelineUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                applicationService.updateStatus(id, request, userDetails.getUsername()));
    }

    // ── Stage history ──────────────────────────────────────────────
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN', 'CANDIDATE')")
    public ResponseEntity<List<PipelineStageResponse>> getStageHistory(
            @PathVariable Long id) {

        return ResponseEntity.ok(applicationService.getStageHistory(id));
    }
}