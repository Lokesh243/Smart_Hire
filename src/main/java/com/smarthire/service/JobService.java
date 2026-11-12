package com.smarthire.service;

import com.smarthire.aspect.Auditable;
import com.smarthire.dto.request.JobRequest;
import com.smarthire.dto.response.JobResponse;
import com.smarthire.entity.Job;
import com.smarthire.entity.User;
import com.smarthire.enums.JobStatus;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.exception.UnauthorizedException;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    // ── Create job ─────────────────────────────────────────────────
    @Auditable(action = "CREATE_JOB")
    @Transactional
    public JobResponse createJob(JobRequest request, String hrEmail) {

        User hr = userRepository.findByEmail(hrEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + hrEmail));

        Job job = Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .jobType(request.getJobType())
                .status(JobStatus.OPEN)
                .expiresAt(request.getExpiresAt())
                .postedBy(hr)
                .build();

        Job saved = jobRepository.save(job);
        log.info("Job created: {} by {}", saved.getTitle(), hrEmail);
        return mapToResponse(saved);
    }

    // ── Get all open jobs (paginated + filterable) ─────────────────
    public Page<JobResponse> getAllJobs(String title,
                                        String location,
                                        String jobType,
                                        Pageable pageable) {

        Specification<Job> spec = Specification
                .allOf(
                        hasStatus(JobStatus.OPEN),
                        hasTitle(title),
                        hasLocation(location),
                        hasJobType(jobType)
                );

        return jobRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    // ── Get job by id ──────────────────────────────────────────────
    public JobResponse getJobById(Long id) {
        Job job = findJobById(id);
        return mapToResponse(job);
    }

    // ── Update job ─────────────────────────────────────────────────
    @Transactional
    public JobResponse updateJob(Long id, JobRequest request, String email) {

        Job job = findJobById(id);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Only the HR who posted it or ADMIN can update
        if (!job.getPostedBy().getId().equals(user.getId())
                && !user.getRole().name().equals("ADMIN")) {
            throw new UnauthorizedException("You can only update your own jobs");
        }

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setLocation(request.getLocation());
        job.setJobType(request.getJobType());
        job.setExpiresAt(request.getExpiresAt());

        log.info("Job updated: {} by {}", id, email);
        return mapToResponse(jobRepository.save(job));
    }

    // ── Close job (Admin only) ─────────────────────────────────────
    @Auditable(action = "CLOSE_JOB")
    @Transactional
    public void closeJob(Long id, String email) {
        Job job = findJobById(id);
        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);
        log.info("Job closed: {} by {}", id, email);
    }

    // ── Get jobs posted by HR ──────────────────────────────────────
    public List<JobResponse> getMyJobs(String hrEmail) {
        User hr = userRepository.findByEmail(hrEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
        return jobRepository.findByPostedBy(hr)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ── Spring Specifications (dynamic filters) ────────────────────
    private Specification<Job> hasStatus(JobStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    private Specification<Job> hasTitle(String title) {
        return (root, query, cb) -> title == null || title.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("title")),
                "%" + title.toLowerCase() + "%");
    }

    private Specification<Job> hasLocation(String location) {
        return (root, query, cb) -> location == null || location.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("location")),
                "%" + location.toLowerCase() + "%");
    }

    private Specification<Job> hasJobType(String jobType) {
        return (root, query, cb) -> jobType == null || jobType.isBlank()
                ? cb.conjunction()
                : cb.equal(cb.lower(root.get("jobType")),
                jobType.toLowerCase());
    }

    // ── Helper ─────────────────────────────────────────────────────
    private Job findJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job not found with id: " + id));
    }





    // ── Entity to DTO mapping ──────────────────────────────────────
    JobResponse mapToDTO(Job job) {
        Long applicantCount = applicationRepository
                .countByJobId(job.getId());

        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .location(job.getLocation())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .expiresAt(job.getExpiresAt())
                .postedById(job.getPostedBy().getId())
                .postedByName(job.getPostedBy().getFullName())
                .totalApplicants(applicantCount)
                .createdAt(job.getCreatedAt())
                .build();
    }

    private JobResponse mapToResponse(Job job) {
        return mapToDTO(job);
    }
}