package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.response.CertificationResponse;
import com.example.waiter_rating.dto.response.EducationResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.WorkHistory;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.repository.WorkHistoryRepo;
import com.example.waiter_rating.service.CertificationService;
import com.example.waiter_rating.service.EducationService;
import com.example.waiter_rating.service.PdfService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfServiceImpl implements PdfService {

    private final AppUserRepo appUserRepo;
    private final WorkHistoryRepo workHistoryRepo;
    private final EducationService educationService;
    private final CertificationService certificationService;

    // Paleta de colores Modernos
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(79, 70, 229);   // Indigo moderno
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(107, 114, 128); // Gris suave
    private static final DeviceRgb SIDEBAR_BG = new DeviceRgb(248, 250, 252);     // Slate 50
    private static final DeviceRgb TEXT_DARK = new DeviceRgb(31, 41, 55);       // Casi negro
    private static final DeviceRgb STAR_YELLOW = new DeviceRgb(251, 191, 36);    // Dorado ratings

    public PdfServiceImpl(AppUserRepo appUserRepo, WorkHistoryRepo workHistoryRepo,
                          EducationService educationService, CertificationService certificationService) {
        this.appUserRepo = appUserRepo;
        this.workHistoryRepo = workHistoryRepo;
        this.educationService = educationService;
        this.certificationService = certificationService;
    }

    @Override
    public byte[] generateCvPdf(Long professionalId) throws Exception {
        AppUser professional = appUserRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        List<WorkHistory> workHistory = workHistoryRepo.findByProfessionalId(professionalId);
        List<EducationResponse> education = educationService.getEducationByProfessional(professionalId);
        List<CertificationResponse> certifications = certificationService.getCertificationsByProfessional(professionalId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        document.setMargins(0, 0, 0, 0);

        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        float[] columnWidths = {33f, 67f};
        Table mainTable = new Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(null);

        // --- COLUMNA IZQUIERDA (SIDEBAR) ---
        Cell leftColumn = new Cell()
                .setBackgroundColor(SIDEBAR_BG)
                .setBorder(null)
                .setPadding(30)
                .setVerticalAlignment(VerticalAlignment.TOP);

        addProfilePhoto(leftColumn, professional, boldFont);
        leftColumn.add(new Paragraph("\n").setMarginBottom(10));

        addSidebarSectionTitle(leftColumn, "CONTACTO", boldFont);
        addContactItem(leftColumn, "Email", professional.getEmail(), regularFont);
        if (professional.getPhone() != null) addContactItem(leftColumn, "Teléfono", professional.getPhone(), regularFont);
        if (professional.getLocation() != null) addContactItem(leftColumn, "Ubicación", professional.getLocation(), regularFont);

        leftColumn.add(new Paragraph("\n").setMarginBottom(15));
        addReputationBadge(leftColumn, professional, boldFont, regularFont);

        mainTable.addCell(leftColumn);

        // --- COLUMNA DERECHA (CONTENIDO) ---
        Cell rightColumn = new Cell()
                .setBorder(null)
                .setPadding(40)
                .setVerticalAlignment(VerticalAlignment.TOP);

        rightColumn.add(new Paragraph(professional.getName().toUpperCase())
                .setFont(boldFont).setFontSize(28).setFontColor(TEXT_DARK).setMarginBottom(0));

        if (professional.getProfessionalTitle() != null) {
            rightColumn.add(new Paragraph(professional.getProfessionalTitle())
                    .setFont(regularFont).setFontSize(14).setFontColor(PRIMARY_COLOR).setMarginBottom(20));
        }

        if (professional.getCv() != null && professional.getCv().getDescription() != null) {
            addContentSectionTitle(rightColumn, "SOBRE MÍ", boldFont);
            // CAMBIO AQUÍ: Usamos setMultipliedLeading en lugar de setLineSpacing
            rightColumn.add(new Paragraph(professional.getCv().getDescription())
                    .setFont(regularFont).setFontSize(10).setMultipliedLeading(1.5f).setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(20));
        }

        if (professional.getCv() != null && professional.getCv().getSkills() != null && !professional.getCv().getSkills().isBlank()) {
            addContentSectionTitle(rightColumn, "APTITUDES Y HABILIDADES", boldFont);
            String[] skillTags = professional.getCv().getSkills().split(",");
            Paragraph skillsPara = new Paragraph().setMarginBottom(20);
            for (int i = 0; i < skillTags.length; i++) {
                String tag = skillTags[i].trim();
                if (!tag.isEmpty()) {
                    skillsPara.add(new Text("  " + tag + "  ").setFont(regularFont).setFontSize(9)
                            .setFontColor(PRIMARY_COLOR)
                            .setBackgroundColor(new DeviceRgb(238, 242, 255)));
                    if (i < skillTags.length - 1) {
                        skillsPara.add(new Text("  ").setFont(regularFont));
                    }
                }
            }
            rightColumn.add(skillsPara);
        }

        if (!workHistory.isEmpty()) {
            addContentSectionTitle(rightColumn, "EXPERIENCIA LABORAL", boldFont);
            addTimelineExperience(rightColumn, workHistory, boldFont, regularFont);
        }

        if (!education.isEmpty()) {
            addContentSectionTitle(rightColumn, "EDUCACIÓN Y CAPACITACIONES", boldFont);
            addEducationSection(rightColumn, education, boldFont, regularFont);
        }

        if (!certifications.isEmpty()) {
            addContentSectionTitle(rightColumn, "CERTIFICACIONES", boldFont);
            addCertificationsSection(rightColumn, certifications, boldFont, regularFont);
        }

        mainTable.addCell(rightColumn);
        document.add(mainTable);

        document.close();
        return baos.toByteArray();
    }

    private void addProfilePhoto(Cell cell, AppUser professional, PdfFont boldFont) {
        float size = 90f;
        if (professional.getProfilePicture() != null && !professional.getProfilePicture().isEmpty()) {
            try {
                Image photo = new Image(ImageDataFactory.create(professional.getProfilePicture()));
                photo.scaleToFit(size, size).setBorder(new SolidBorder(ColorConstants.WHITE, 3));
                cell.add(photo);
                return;
            } catch (Exception e) { }
        }

        Paragraph initial = new Paragraph(professional.getName().substring(0, 1))
                .setFont(boldFont).setFontSize(35).setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(PRIMARY_COLOR).setTextAlignment(TextAlignment.CENTER)
                .setWidth(size).setHeight(size).setPaddingTop(20);
        cell.add(initial);
    }

    private void addSidebarSectionTitle(Cell cell, String title, PdfFont boldFont) {
        cell.add(new Paragraph(title).setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK)
                .setBorderBottom(new SolidBorder(new DeviceRgb(203, 213, 225), 1)).setMarginBottom(10));
    }

    private void addContactItem(Cell cell, String label, String value, PdfFont regularFont) {
        cell.add(new Paragraph(label).setFont(regularFont).setBold().setFontSize(8).setFontColor(SECONDARY_COLOR).setMarginBottom(0));
        cell.add(new Paragraph(value).setFont(regularFont).setFontSize(9).setFontColor(TEXT_DARK).setMarginBottom(8));
    }

    private void addReputationBadge(Cell cell, AppUser professional, PdfFont boldFont, PdfFont regularFont) {
        addSidebarSectionTitle(cell, "REPUTACIÓN", boldFont);
        double score = professional.getReputationScore() != null ? professional.getReputationScore() : 0.0;
        String stars = "★".repeat((int) Math.round(score)) + "☆".repeat(5 - (int) Math.round(score));
        cell.add(new Paragraph(stars).setFontSize(18).setFontColor(STAR_YELLOW).setMarginBottom(0));
        cell.add(new Paragraph(String.format("%.1f / 5.0", score)).setFont(boldFont).setFontSize(12).setMarginBottom(0));
        cell.add(new Paragraph(professional.getTotalRatings() + " reseñas").setFont(regularFont).setFontSize(8).setFontColor(SECONDARY_COLOR));
    }

    private void addContentSectionTitle(Cell cell, String title, PdfFont boldFont) {
        cell.add(new Paragraph(title).setFont(boldFont).setFontSize(13).setFontColor(TEXT_DARK)
                .setBorderLeft(new SolidBorder(PRIMARY_COLOR, 3)).setPaddingLeft(10).setMarginBottom(15).setMarginTop(10));
    }

    private void addTimelineExperience(Cell cell, List<WorkHistory> workHistory, PdfFont boldFont, PdfFont regularFont) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        for (WorkHistory work : workHistory) {
            Paragraph p = new Paragraph().setMarginBottom(12);
            p.add(new Text(work.getPosition() + "\n").setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK));
            p.add(new Text(work.getBusinessName()).setFont(boldFont).setFontSize(10).setFontColor(PRIMARY_COLOR));
            String dateRange = "  •  " + work.getStartDate().format(fmt) + " - " +
                    (work.getEndDate() != null ? work.getEndDate().format(fmt) : "Actualidad");
            p.add(new Text(dateRange).setFont(regularFont).setFontSize(9).setFontColor(SECONDARY_COLOR));
            cell.add(p);
            if (work.getDescription() != null && !work.getDescription().isBlank()) {
                cell.add(new Paragraph(work.getDescription()).setFont(regularFont).setFontSize(9).setMarginLeft(10).setMarginBottom(15));
            }
        }
    }

    private void addEducationSection(Cell cell, List<EducationResponse> education, PdfFont boldFont, PdfFont regularFont) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        for (EducationResponse edu : education) {
            Paragraph p = new Paragraph().setMarginBottom(4);
            if (edu.getDegree() != null && !edu.getDegree().isBlank()) {
                p.add(new Text(edu.getDegree() + "\n").setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK));
            }
            if (edu.getInstitution() != null && !edu.getInstitution().isBlank()) {
                p.add(new Text(edu.getInstitution()).setFont(boldFont).setFontSize(10).setFontColor(PRIMARY_COLOR));
            }
            if (edu.getStartDate() != null) {
                String dateRange = "  •  " + edu.getStartDate().format(fmt) + " - " +
                        (Boolean.TRUE.equals(edu.getCurrentlyStudying()) ? "Actualidad"
                                : (edu.getEndDate() != null ? edu.getEndDate().format(fmt) : ""));
                p.add(new Text(dateRange).setFont(regularFont).setFontSize(9).setFontColor(SECONDARY_COLOR));
            }
            cell.add(p);
            if (edu.getDescription() != null && !edu.getDescription().isBlank()) {
                cell.add(new Paragraph(edu.getDescription()).setFont(regularFont).setFontSize(9).setMarginLeft(10).setMarginBottom(12));
            } else {
                cell.add(new Paragraph("").setMarginBottom(12));
            }
        }
    }

    private void addCertificationsSection(Cell cell, List<CertificationResponse> certifications, PdfFont boldFont, PdfFont regularFont) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        for (CertificationResponse cert : certifications) {
            Paragraph p = new Paragraph().setMarginBottom(4);
            p.add(new Text(cert.getName() + "\n").setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK));
            if (cert.getIssuer() != null && !cert.getIssuer().isBlank()) {
                p.add(new Text(cert.getIssuer()).setFont(boldFont).setFontSize(10).setFontColor(PRIMARY_COLOR));
            }
            if (cert.getDateObtained() != null) {
                String dateInfo = "  •  " + cert.getDateObtained().format(fmt);
                if (cert.getExpiryDate() != null) {
                    dateInfo += " — vence " + cert.getExpiryDate().format(fmt);
                }
                p.add(new Text(dateInfo).setFont(regularFont).setFontSize(9).setFontColor(SECONDARY_COLOR));
            }
            cell.add(p);
            cell.add(new Paragraph("").setMarginBottom(12));
        }
    }
}