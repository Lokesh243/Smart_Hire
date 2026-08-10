package com.smarthire.dto.response;

import com.smarthire.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private String jobType;
    private JobStatus status;
    private LocalDate expiresAt;
    private Long postedById;
    private String postedByName;
    private Long totalApplicants;
    private LocalDateTime createdAt;
}