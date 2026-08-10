package com.smarthire.dto.response;

import com.smarthire.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String jobLocation;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private ApplicationStatus status;
    private String resumeUrl;
    private String coverLetter;
    private List<PipelineStageResponse> stageHistory;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}