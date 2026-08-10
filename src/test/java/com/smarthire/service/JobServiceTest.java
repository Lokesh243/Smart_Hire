package com.smarthire.service;

import com.smarthire.dto.request.JobRequest;
import com.smarthire.dto.response.JobResponse;
import com.smarthire.entity.Job;
import com.smarthire.entity.User;
import com.smarthire.enums.JobStatus;
import com.smarthire.enums.Role;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationRepository applicationRepository;

    @InjectMocks private JobService jobService;

    private User mockHr;
    private Job mockJob;

    @BeforeEach
    void setUp() {
        mockHr = User.builder()
                .id(1L)
                .fullName("HR Manager")
                .email("hr@test.com")
                .role(Role.HR)
                .isActive(true)
                .build();

        mockJob = Job.builder()
                .id(1L)
                .title("Java Developer")
                .description("Spring Boot expert needed")
                .location("Hyderabad")
                .jobType("Full-time")
                .status(JobStatus.OPEN)
                .expiresAt(LocalDate.now().plusMonths(3))
                .postedBy(mockHr)
                .build();
    }

    // ── Create job tests ───────────────────────────────────────────

    @Test
    void createJob_success_returnsJobResponse() {
        JobRequest request = new JobRequest();
        request.setTitle("Java Developer");
        request.setDescription("Spring Boot expert needed");
        request.setLocation("Hyderabad");
        request.setJobType("Full-time");
        request.setExpiresAt(LocalDate.now().plusMonths(3));

        when(userRepository.findByEmail("hr@test.com"))
                .thenReturn(Optional.of(mockHr));
        when(jobRepository.save(any(Job.class))).thenReturn(mockJob);
        when(applicationRepository.countByJobId(anyLong())).thenReturn(0L);

        JobResponse response = jobService.createJob(request, "hr@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Java Developer");
        assertThat(response.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(response.getPostedByName()).isEqualTo("HR Manager");
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    void createJob_hrNotFound_throwsException() {
        JobRequest request = new JobRequest();
        request.setTitle("Java Developer");
        request.setExpiresAt(LocalDate.now().plusMonths(1));

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.createJob(request, "ghost@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobRepository, never()).save(any());
    }

    // ── Get job by id tests ────────────────────────────────────────

    @Test
    void getJobById_existingId_returnsJobResponse() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(mockJob));
        when(applicationRepository.countByJobId(1L)).thenReturn(5L);

        JobResponse response = jobService.getJobById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTotalApplicants()).isEqualTo(5L);
    }

    @Test
    void getJobById_notFound_throwsException() {
        when(jobRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Job not found");
    }

    // ── Get all jobs tests ─────────────────────────────────────────

    @Test
    void getAllJobs_returnsPagedResponse() {
        Page<Job> mockPage = new PageImpl<>(List.of(mockJob));
        when(jobRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(mockPage);
        when(applicationRepository.countByJobId(anyLong())).thenReturn(0L);

        Page<JobResponse> result = jobService.getAllJobs(
                null, null, null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle())
                .isEqualTo("Java Developer");
    }

    // ── Close job tests ────────────────────────────────────────────

    @Test
    void closeJob_existingJob_setsStatusClosed() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(mockJob));
        when(jobRepository.save(any(Job.class))).thenReturn(mockJob);

        jobService.closeJob(1L, "admin@test.com");

        assertThat(mockJob.getStatus()).isEqualTo(JobStatus.CLOSED);
        verify(jobRepository, times(1)).save(mockJob);
    }

    @Test
    void closeJob_notFound_throwsException() {
        when(jobRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.closeJob(999L, "admin@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}