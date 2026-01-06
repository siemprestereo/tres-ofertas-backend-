package com.example.waiter_rating.service;

import com.example.waiter_rating.model.Education;
import java.util.List;

public interface EducationService {
    List<Education> getEducationByProfessional(Long professionalId);
    Education addEducation(Long professionalId, Education education);
    Education updateEducation(Long professionalId, Long educationId, Education education);
    void deleteEducation(Long professionalId, Long educationId);
    void deleteAllByProfessional(Long professionalId);
}