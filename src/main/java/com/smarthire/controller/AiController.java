package com.smarthire.controller;

import com.smarthire.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // ── Generate job description ───────────────────────────────────
    @PostMapping("/generate-job-description")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<Map<String, String>> generateJobDescription(
            @RequestBody Map<String, String> request) {

        String description = aiService.generateJobDescription(
                request.get("title"),
                request.get("location"),
                request.get("jobType")
        );

        return ResponseEntity.ok(Map.of("description", description));
    }
    // ── Candidate summarizer ───────────────────────────────────────
    @PostMapping("/summarize-candidate")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<Map<String, String>> summarizeCandidate(
            @RequestBody Map<String, String> request) {

        return ResponseEntity.ok(aiService.summarizeCandidate(
                request.get("candidateName"),
                request.get("resumeUrl"),
                request.get("jobTitle")
        ));
    }

    // ── Rejection email writer ─────────────────────────────────────
    @PostMapping("/rejection-email")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<Map<String, String>> writeRejectionEmail(
            @RequestBody Map<String, String> request) {

        String email = aiService.writeRejectionEmail(
                request.get("candidateName"),
                request.get("jobTitle"),
                request.get("companyName")
        );
        return ResponseEntity.ok(Map.of("email", email));
    }

    // ── Job recommendations ────────────────────────────────────────
    @PostMapping("/recommend-jobs")
    public ResponseEntity<List<Map<String, Object>>> recommendJobs(
            @RequestBody Map<String, Object> request) {

        return ResponseEntity.ok(aiService.recommendJobs(
                (String) request.get("candidateHistory"),
                (List<Map<String, Object>>) request.get("availableJobs")
        ));
    }

    // ── Match resume against job ───────────────────────────────────
    @PostMapping("/match-resume")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> matchResume(
            @RequestBody Map<String, String> request) {

        return ResponseEntity.ok(aiService.matchResume(
                request.get("resumeUrl"),
                request.get("jobTitle"),
                request.get("jobDescription")
        ));
    }

    // ── Natural language search ────────────────────────────────────
    @PostMapping("/search-filters")
    public ResponseEntity<Map<String, String>> extractSearchFilters(
            @RequestBody Map<String, String> request) {

        return ResponseEntity.ok(
                aiService.extractSearchFilters(request.get("query")));
    }

    // ── Generate interview questions ───────────────────────────────
    @PostMapping("/interview-questions")
    @PreAuthorize("hasAnyAuthority('HR', 'ADMIN')")
    public ResponseEntity<List<String>> generateInterviewQuestions(
            @RequestBody Map<String, String> request) {

        return ResponseEntity.ok(aiService.generateInterviewQuestions(
                request.get("jobTitle"),
                request.get("jobDescription"),
                request.get("candidateName")
        ));
    }
}