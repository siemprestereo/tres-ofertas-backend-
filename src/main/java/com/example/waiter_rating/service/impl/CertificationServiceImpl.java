package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.response.CertificationResponse;
import com.example.waiter_rating.model.Certification;
import com.example.waiter_rating.model.Professional;
import com.example.waiter_rating.repository.CertificationRepo;
import com.example.waiter_rating.repository.ProfessionalRepo;
import com.example.waiter_rating.service.CertificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CertificationServiceImpl implements CertificationService {

    private final CertificationRepo certificationRepository;
    private final ProfessionalRepo professionalRepo;

    public CertificationServiceImpl(CertificationRepo certificationRepository, ProfessionalRepo professionalRepo) {
        this.certificationRepository = certificationRepository;
        this.professionalRepo = professionalRepo;
    }

    public List<CertificationResponse> getCertificationsByProfessional(Long professionalId) {
        return certificationRepository.findByProfessionalId(professionalId).stream()
                .map(this::toDTO)
                .toList();
    }

    private CertificationResponse toDTO(Certification certification) {
        return new CertificationResponse(
                certification.getId(),
                certification.getName(),
                certification.getIssuer(),
                certification.getDateObtained(),
                certification.getExpiryDate()
        );
    }

    @Override
    @Transactional
    public Certification addCertification(Long professionalId, Certification certification) {
        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        certification.setProfessional(professional);
        return certificationRepository.save(certification);
    }

    @Override
    @Transactional
    public Certification updateCertification(Long professionalId, Long certificationId, Certification updatedCertification) {
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));

        if (!certification.getProfessional().getId().equals(professionalId)) {
            throw new IllegalArgumentException("No tienes permiso para editar esta certificación");
        }

        certification.setName(updatedCertification.getName());
        certification.setIssuer(updatedCertification.getIssuer());
        certification.setDateObtained(updatedCertification.getDateObtained());
        certification.setExpiryDate(updatedCertification.getExpiryDate());

        return certificationRepository.save(certification);
    }

    @Override
    @Transactional
    public void deleteCertification(Long professionalId, Long certificationId) {
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));

        if (!certification.getProfessional().getId().equals(professionalId)) {
            throw new IllegalArgumentException("No tienes permiso para eliminar esta certificación");
        }

        certificationRepository.delete(certification);
    }

    @Override
    @Transactional
    public void deleteAllByProfessional(Long professionalId) {
        certificationRepository.deleteByProfessionalId(professionalId);
    }
}