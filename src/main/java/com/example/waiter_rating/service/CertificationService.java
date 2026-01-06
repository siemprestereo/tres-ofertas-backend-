package com.example.waiter_rating.service;

import com.example.waiter_rating.model.Certification;
import java.util.List;

public interface CertificationService {
    List<Certification> getCertificationsByProfessional(Long professionalId);
    Certification addCertification(Long professionalId, Certification certification);
    Certification updateCertification(Long professionalId, Long certificationId, Certification certification);
    void deleteCertification(Long professionalId, Long certificationId);
    void deleteAllByProfessional(Long professionalId);
}