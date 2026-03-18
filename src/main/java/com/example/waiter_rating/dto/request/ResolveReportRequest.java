package com.example.waiter_rating.dto.request;

import com.example.waiter_rating.model.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveReportRequest {

    @NotNull
    private ReportStatus status; // APPROVED o REJECTED

    private String adminNotes;
}