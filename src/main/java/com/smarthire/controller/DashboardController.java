package com.smarthire.controller;

import com.smarthire.dto.response.DashboardResponse;
import com.smarthire.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/hr")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<DashboardResponse> getHrDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                dashboardService.getHrDashboard(userDetails.getUsername()));
    }

    @GetMapping("/candidate")
    @PreAuthorize("hasAuthority('CANDIDATE')")
    public ResponseEntity<DashboardResponse> getCandidateDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                dashboardService.getCandidateDashboard(userDetails.getUsername()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }
}