package com.example.waiter_rating.dto.request;

import java.util.List;

public class PdfGenerationRequest {

    private String layout = "clasico";
    private boolean includeDescription = true;
    private boolean includeSkills = true;

    // null = incluir todos; lista con IDs = incluir solo esos
    private List<Long> workHistoryIds;
    private List<Long> educationIds;
    private List<Long> certificationIds;

    public String getLayout() { return layout; }
    public void setLayout(String layout) { this.layout = layout; }

    public boolean isIncludeDescription() { return includeDescription; }
    public void setIncludeDescription(boolean includeDescription) { this.includeDescription = includeDescription; }

    public boolean isIncludeSkills() { return includeSkills; }
    public void setIncludeSkills(boolean includeSkills) { this.includeSkills = includeSkills; }

    public List<Long> getWorkHistoryIds() { return workHistoryIds; }
    public void setWorkHistoryIds(List<Long> workHistoryIds) { this.workHistoryIds = workHistoryIds; }

    public List<Long> getEducationIds() { return educationIds; }
    public void setEducationIds(List<Long> educationIds) { this.educationIds = educationIds; }

    public List<Long> getCertificationIds() { return certificationIds; }
    public void setCertificationIds(List<Long> certificationIds) { this.certificationIds = certificationIds; }
}
