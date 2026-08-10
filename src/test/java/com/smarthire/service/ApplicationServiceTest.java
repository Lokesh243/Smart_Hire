package com.smarthire.service;

import com.smarthire.dto.request.ApplicationRequest;
import com.smarthire.dto.request.PipelineUpdateRequest;
import com.smarthire.dto.response.ApplicationResponse;
import com.smarthire.entity.Application;
import com.smarthire.entity.Job;
import com.smarthire.entity.User;
import com.smarthire.enums.ApplicationStatus;
import com.smarthire.enums.JobStatus;
import com.smarthire.enums.Role;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.PipelineStageRepository;
import com.smarthire.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;
    @Mock private PipelineStageRepository pipelineStageRepository;
    @Mock private EmailService emailService;

    @InjectMocks private ApplicationService applicationService;

    private User mockCandidate;
    private User mockHr;
    private Job mockJob;
    private Application mockApplication;

    @BeforeEach
    void setUp() {
        mockCandidate = User.builder()
                .id(1L)
                .fullName("John Candidate")
                .email("john@test.com")
                .role(Role.CANDIDATE)
                .isActive(true)
                .build();

        mockHr = User.builder()
                .id(2L)
                .fullName("HR Manager")
                .email("hr@test.com")
                .role(Role.HR)
                .isActive(true)
                .build();

        mockJob = Job.builder()
                .id(1L)
                .title("Java Developer")
                .description("Spring Boot expert")
                .location("Hyderabad")
                .status(JobStatus.OPEN)
                .expiresAt(LocalDate.now().plusMonths(3))
                .postedBy(mockHr)
                .build();

        mockApplication = Application.builder()
                .id(1L)
                .job(mockJob)
                .candidate(mockCandidate)
                .status(ApplicationStatus.APPLIED)
                .resumeUrl("https://drive.google.com/resume.pdf")
                .build();
    }

    // ── Apply tests ────────────────────────────────────────────────

    @Test
    void apply_success_returnsApplicationResponse() {
        ApplicationRequest request = new ApplicationRequest();
        request.setJobId(1L);
        request.setResumeUrl("https://drive.google.com/resume.pdf");

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(mockCandidate));
        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(mockJob));
        when(applicationRepository.existsByCandidateAndJob(any(), any()))
                .thenReturn(false);
        when(applicationRepository.save(any(Application.class)))
                .thenReturn(mockApplication);
        when(pipelineStageRepository.findByApplicationOrderByMovedAtAsc(any()))
                .thenReturn(List.of());

        ApplicationResponse response = applicationService.apply(
                request, "john@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getJobTitle()).isEqualTo("Java Developer");
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        verify(applicationRepository, times(1)).save(any(Application.class));
        verify(emailService, times(1))
                .sendApplicationConfirmation(anyString(), anyString(), anyString());
    }

    @Test
    void apply_jobClosed_throwsException() {
        mockJob.setStatus(JobStatus.CLOSED);

        ApplicationRequest request = new ApplicationRequest();
        request.setJobId(1L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(mockCandidate));
        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(mockJob));

        assertThatThrownBy(() -> applicationService.apply(
                request, "john@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no longer accepting");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void apply_duplicateApplication_throwsException() {
        ApplicationRequest request = new ApplicationRequest();
        request.setJobId(1L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(mockCandidate));
        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(mockJob));
        when(applicationRepository.existsByCandidateAndJob(any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(
                request, "john@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void apply_jobNotFound_throwsException() {
        ApplicationRequest request = new ApplicationRequest();
        request.setJobId(999L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(mockCandidate));
        when(jobRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.apply(
                request, "john@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── UpdateStatus tests ─────────────────────────────────────────

    @Test
    void updateStatus_validTransition_updatesStatus() {
        PipelineUpdateRequest request = new PipelineUpdateRequest();
        request.setStatus(ApplicationStatus.SCREENED);
        request.setNotes("Good profile");

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(mockApplication));
        when(userRepository.findByEmail("hr@test.com"))
                .thenReturn(Optional.of(mockHr));
        when(applicationRepository.save(any(Application.class)))
                .thenReturn(mockApplication);
        when(pipelineStageRepository.findByApplicationOrderByMovedAtAsc(any()))
                .thenReturn(List.of());

        ApplicationResponse response = applicationService.updateStatus(
                1L, request, "hr@test.com");

        assertThat(response).isNotNull();
        assertThat(mockApplication.getStatus())
                .isEqualTo(ApplicationStatus.SCREENED);
        verify(emailService, times(1)).sendStageUpdateEmail(
                anyString(), anyString(), anyString(),
                eq(ApplicationStatus.SCREENED));
    }

    @Test
    void updateStatus_invalidTransition_throwsException() {
        mockApplication.setStatus(ApplicationStatus.APPLIED);

        PipelineUpdateRequest request = new PipelineUpdateRequest();
        request.setStatus(ApplicationStatus.OFFER);
        request.setNotes("Skipping stages");

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(mockApplication));
        when(userRepository.findByEmail("hr@test.com"))
                .thenReturn(Optional.of(mockHr));

        assertThatThrownBy(() -> applicationService.updateStatus(
                1L, request, "hr@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    void updateStatus_applicationNotFound_throwsException() {
        PipelineUpdateRequest request = new PipelineUpdateRequest();
        request.setStatus(ApplicationStatus.SCREENED);

        when(applicationRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.updateStatus(
                999L, request, "hr@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── GetMyApplications tests ────────────────────────────────────

    @Test
    void getMyApplications_returnsListForCandidate() {
        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(mockCandidate));
        when(applicationRepository.findByCandidate(mockCandidate))
                .thenReturn(List.of(mockApplication));
        when(pipelineStageRepository.findByApplicationOrderByMovedAtAsc(any()))
                .thenReturn(List.of());

        List<ApplicationResponse> result = applicationService
                .getMyApplications("john@test.com");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCandidateName())
                .isEqualTo("John Candidate");
    }
}