package com.smarthire.service;

import com.smarthire.aspect.Auditable;
import com.smarthire.dto.request.ApplicationRequest;
import com.smarthire.dto.request.PipelineUpdateRequest;
import com.smarthire.dto.response.ApplicationResponse;
import com.smarthire.dto.response.PipelineStageResponse;
import com.smarthire.entity.Application;
import com.smarthire.entity.Job;
import com.smarthire.entity.PipelineStage;
import com.smarthire.entity.User;
import com.smarthire.enums.ApplicationStatus;
import com.smarthire.enums.JobStatus;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.exception.UnauthorizedException;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.PipelineStageRepository;
import com.smarthire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final PipelineStageRepository pipelineStageRepository;
    private final EmailService emailService;

    // ── Apply to a job ─────────────────────────────────────────────
    @Auditable(action = "APPLY_JOB")
    @Transactional
    public ApplicationResponse apply(ApplicationRequest request, String candidateEmail) {

        User candidate = findUserByEmail(candidateEmail);

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + request.getJobId()));

        // Validate job is still open
        if (job.getStatus() != JobStatus.OPEN) {
            throw new RuntimeException("This job is no longer accepting applications");
        }

        // Prevent duplicate applications
        if (applicationRepository.existsByCandidateAndJob(candidate, job)) {
            throw new RuntimeException("You have already applied for this job");
        }

        Application application = Application.builder()
                .job(job)
                .candidate(candidate)
                .status(ApplicationStatus.APPLIED)
                .resumeUrl(request.getResumeUrl())
                .build();

        Application saved = applicationRepository.save(application);

        // Create first pipeline stage entry
        createPipelineEntry(saved, ApplicationStatus.APPLIED, "Application submitted", candidate);

        // Send confirmation email async
        emailService.sendApplicationConfirmation(
                saved.getCandidate().getEmail(),
                saved.getCandidate().getFullName(),
                saved.getJob().getTitle()
        );

        log.info("Application submitted by {} for job {}", candidateEmail, job.getTitle());
        return mapToResponse(saved);
    }

    // ── Get my applications (candidate) ───────────────────────────
    public List<ApplicationResponse> getMyApplications(String candidateEmail) {
        User candidate = findUserByEmail(candidateEmail);
        return applicationRepository.findByCandidate(candidate)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ── Get application by id ──────────────────────────────────────
    public ApplicationResponse getApplicationById(Long id) {
        return mapToResponse(findApplicationById(id));
    }

    // ── Get all applicants for a job (HR) ─────────────────────────
    public List<ApplicationResponse> getApplicantsByJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        return applicationRepository.findByJob(job)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ── Move pipeline stage (HR/ADMIN) ─────────────────────────────
    @Auditable(action = "UPDATE_PIPELINE")
    @Transactional
    public ApplicationResponse updateStatus(Long applicationId,
                                            PipelineUpdateRequest request,
                                            String hrEmail) {

        Application application = findApplicationById(applicationId);
        User hr = findUserByEmail(hrEmail);

        ApplicationStatus newStatus = request.getStatus();
        validateTransition(application.getStatus(), newStatus);

        application.setStatus(newStatus);
        applicationRepository.save(application);

        // Record stage change
        createPipelineEntry(application, newStatus, request.getNotes(), hr);

        // Notify candidate async
        emailService.sendStageUpdateEmail(
                application.getCandidate().getEmail(),
                application.getCandidate().getFullName(),
                application.getJob().getTitle(),
                newStatus
        );

        log.info("Application {} moved to {} by {}", applicationId, newStatus, hrEmail);
        return mapToResponse(application);
    }

    // ── Get stage history ──────────────────────────────────────────
    public List<PipelineStageResponse> getStageHistory(Long applicationId) {
        Application application = findApplicationById(applicationId);
        return pipelineStageRepository
                .findByApplicationOrderByMovedAtAsc(application)
                .stream()
                .map(this::mapStageToResponse)
                .toList();
    }

    // ── Validate stage transitions ─────────────────────────────────
    private void validateTransition(ApplicationStatus current, ApplicationStatus next) {
        boolean valid = switch (current) {
            case APPLIED   -> next == ApplicationStatus.SCREENED  || next == ApplicationStatus.REJECTED;
            case SCREENED  -> next == ApplicationStatus.INTERVIEW || next == ApplicationStatus.REJECTED;
            case INTERVIEW -> next == ApplicationStatus.OFFER     || next == ApplicationStatus.REJECTED;
            case OFFER, REJECTED -> false;
        };
        if (!valid) {
            throw new RuntimeException(
                    "Invalid transition: " + current + " → " + next);
        }
    }

    // ── Create pipeline stage entry ────────────────────────────────
    private void createPipelineEntry(Application application,
                                     ApplicationStatus stage,
                                     String notes,
                                     User movedBy) {
        PipelineStage entry = PipelineStage.builder()
                .application(application)
                .stage(stage)
                .notes(notes)
                .movedBy(movedBy)
                .build();
        pipelineStageRepository.save(entry);
    }

    // ── Helpers ────────────────────────────────────────────────────
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private Application findApplicationById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    // ── Mapping ────────────────────────────────────────────────────
    private ApplicationResponse mapToResponse(Application app) {
        List<PipelineStageResponse> history = pipelineStageRepository
                .findByApplicationOrderByMovedAtAsc(app)
                .stream()
                .map(this::mapStageToResponse)
                .toList();

        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .jobLocation(app.getJob().getLocation())
                .candidateId(app.getCandidate().getId())
                .candidateName(app.getCandidate().getFullName())
                .candidateEmail(app.getCandidate().getEmail())
                .status(app.getStatus())
                .resumeUrl(app.getResumeUrl())
                .stageHistory(history)
                .appliedAt(app.getAppliedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private PipelineStageResponse mapStageToResponse(PipelineStage stage) {
        return PipelineStageResponse.builder()
                .id(stage.getId())
                .stage(stage.getStage())
                .notes(stage.getNotes())
                .movedByName(stage.getMovedBy().getFullName())
                .movedAt(stage.getMovedAt())
                .build();
    }
}