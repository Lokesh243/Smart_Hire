package com.smarthire.dto.request;

import com.smarthire.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PipelineUpdateRequest {

    @NotNull(message = "Status is required")
    private ApplicationStatus status;

    private String notes;
}