package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.Education;
import com.example.waiter_rating.model.Professional;
import com.example.waiter_rating.repository.EducationRepo;
import com.example.waiter_rating.repository.ProfessionalRepo;
import com.example.waiter_rating.service.EducationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EducationServiceImpl implements EducationService {

    private final EducationRepo educationRepo;
    private final ProfessionalRepo professionalRepo;

    public EducationServiceImpl(EducationRepo educationRepo, ProfessionalRepo professionalRepo) {
        this.educationRepo = educationRepo;
        this.professionalRepo = professionalRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Education> getEducationByProfessional(Long professionalId) {
        return educationRepo.findByProfessionalId(professionalId);
    }

    @Override
    @Transactional
    public Education addEducation(Long professionalId, Education education) {
        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        education.setProfessional(professional);
        return educationRepo.save(education);
    }

    @Override
    @Transactional
    public Education updateEducation(Long professionalId, Long educationId, Education updatedEducation) {
        Education education = educationRepo.findById(educationId)
                .orElseThrow(() -> new IllegalArgumentException("Educación no encontrada"));

        // Verificar que pertenece al profesional
        if (!education.getProfessional().getId().equals(professionalId)) {
            throw new IllegalArgumentException("No tienes permiso para editar esta educación");
        }

        education.setInstitution(updatedEducation.getInstitution());
        education.setDegree(updatedEducation.getDegree());
        education.setStartDate(updatedEducation.getStartDate());
        education.setEndDate(updatedEducation.getEndDate());
        education.setCurrentlyStudying(updatedEducation.getCurrentlyStudying());
        education.setDescription(updatedEducation.getDescription());

        return educationRepo.save(education);
    }

    @Override
    @Transactional
    public void deleteEducation(Long professionalId, Long educationId) {
        Education education = educationRepo.findById(educationId)
                .orElseThrow(() -> new IllegalArgumentException("Educación no encontrada"));

        if (!education.getProfessional().getId().equals(professionalId)) {
            throw new IllegalArgumentException("No tienes permiso para eliminar esta educación");
        }

        educationRepo.delete(education);
    }

    @Override
    @Transactional
    public void deleteAllByProfessional(Long professionalId) {
        educationRepo.deleteByProfessionalId(professionalId);
    }
}