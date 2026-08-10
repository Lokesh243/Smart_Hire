package com.smarthire.dto.response;

import com.smarthire.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStageResponse {

    private Long id;
    private ApplicationStatus stage;
    private String notes;
    private String movedByName;
    private LocalDateTime movedAt;
}