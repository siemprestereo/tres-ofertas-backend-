package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.PdfGenerationRequest;
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
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return generateCvPdf(professionalId, new PdfGenerationRequest());
    }

    @Override
    public byte[] generateCvPdf(Long professionalId, PdfGenerationRequest request) throws Exception {
        AppUser professional = appUserRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        List<WorkHistory> workHistory = filterWorkHistory(
                workHistoryRepo.findByProfessionalId(professionalId), request.getWorkHistoryIds());
        List<EducationResponse> education = filterById(
                educationService.getEducationByProfessional(professionalId), request.getEducationIds(),
                e -> e.getId());
        List<CertificationResponse> certifications = filterById(
                certificationService.getCertificationsByProfessional(professionalId), request.getCertificationIds(),
                c -> c.getId());

        String layout = request.getLayout() != null ? request.getLayout() : "clasico";
        return switch (layout) {
            case "minimalista" -> buildMinimalistaLayout(professional, workHistory, education, certifications, request);
            case "ejecutivo"   -> buildEjecutivoLayout(professional, workHistory, education, certifications, request);
            default            -> buildClasicoLayout(professional, workHistory, education, certifications, request);
        };
    }

    // ===== LAYOUTS =====

    private byte[] buildClasicoLayout(AppUser professional, List<WorkHistory> workHistory,
            List<EducationResponse> education, List<CertificationResponse> certifications,
            PdfGenerationRequest request) throws Exception {

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
        if (professional.getBirthDate() != null) addContactItem(leftColumn, "Fecha de nac.", professional.getBirthDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), regularFont);

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

        if (request.isIncludeDescription() && professional.getCv() != null && professional.getCv().getDescription() != null) {
            addContentSectionTitle(rightColumn, "SOBRE MÍ", boldFont);
            rightColumn.add(new Paragraph(professional.getCv().getDescription())
                    .setFont(regularFont).setFontSize(10).setMultipliedLeading(1.5f).setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(20));
        }

        if (request.isIncludeSkills() && professional.getCv() != null && professional.getCv().getSkills() != null && !professional.getCv().getSkills().isBlank()) {
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

    private byte[] buildMinimalistaLayout(AppUser professional, List<WorkHistory> workHistory,
            List<EducationResponse> education, List<CertificationResponse> certifications,
            PdfGenerationRequest request) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
        Document document = new Document(pdfDoc);
        document.setMargins(50, 50, 50, 50);

        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Nombre
        document.add(new Paragraph(professional.getName().toUpperCase())
                .setFont(boldFont).setFontSize(26).setFontColor(TEXT_DARK).setMarginBottom(2));

        // Título profesional
        if (professional.getProfessionalTitle() != null) {
            document.add(new Paragraph(professional.getProfessionalTitle())
                    .setFont(regularFont).setFontSize(13).setFontColor(PRIMARY_COLOR).setMarginBottom(4));
        }

        // Contacto en línea
        StringBuilder contact = new StringBuilder();
        contact.append(professional.getEmail());
        if (professional.getPhone() != null) contact.append("  |  ").append(professional.getPhone());
        if (professional.getLocation() != null) contact.append("  |  ").append(professional.getLocation());
        if (professional.getBirthDate() != null) contact.append("  |  ").append(professional.getBirthDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        document.add(new Paragraph(contact.toString())
                .setFont(regularFont).setFontSize(9).setFontColor(SECONDARY_COLOR).setMarginBottom(4));

        // Reputación en línea
        double score = professional.getReputationScore() != null ? professional.getReputationScore() : 0.0;
        String stars = "★".repeat((int) Math.round(score)) + "☆".repeat(5 - (int) Math.round(score));
        document.add(new Paragraph(stars + "  " + String.format("%.1f / 5.0", score) + "  (" + professional.getTotalRatings() + " reseñas)")
                .setFontSize(11).setFontColor(STAR_YELLOW).setMarginBottom(20));

        // Línea separadora
        document.add(new Paragraph("").setBorderBottom(new SolidBorder(PRIMARY_COLOR, 1)).setMarginBottom(20));

        if (request.isIncludeDescription() && professional.getCv() != null && professional.getCv().getDescription() != null) {
            addMinimalistaSectionTitle(document, "SOBRE MÍ", boldFont);
            document.add(new Paragraph(professional.getCv().getDescription())
                    .setFont(regularFont).setFontSize(10).setMultipliedLeading(1.5f)
                    .setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(20));
        }

        if (request.isIncludeSkills() && professional.getCv() != null
                && professional.getCv().getSkills() != null && !professional.getCv().getSkills().isBlank()) {
            addMinimalistaSectionTitle(document, "APTITUDES Y HABILIDADES", boldFont);
            String[] tags = professional.getCv().getSkills().split(",");
            Paragraph skillsPara = new Paragraph().setMarginBottom(20);
            for (String tag : tags) {
                String t = tag.trim();
                if (!t.isEmpty()) {
                    skillsPara.add(new Text("  " + t + "  ").setFont(regularFont).setFontSize(9)
                            .setFontColor(PRIMARY_COLOR).setBackgroundColor(new DeviceRgb(238, 242, 255)));
                    skillsPara.add(new Text("  ").setFont(regularFont));
                }
            }
            document.add(skillsPara);
        }

        if (!workHistory.isEmpty()) {
            addMinimalistaSectionTitle(document, "EXPERIENCIA LABORAL", boldFont);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
            for (WorkHistory work : workHistory) {
                Paragraph p = new Paragraph().setMarginBottom(4);
                p.add(new Text(work.getPosition() + "\n").setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK));
                p.add(new Text(work.getBusinessName()).setFont(boldFont).setFontSize(10).setFontColor(PRIMARY_COLOR));
                String dateRange = "  •  " + work.getStartDate().format(fmt) + " — " +
                        (work.getEndDate() != null ? work.getEndDate().format(fmt) : "Actualidad");
                p.add(new Text(dateRange).setFont(regularFont).setFontSize(9).setFontColor(SECONDARY_COLOR));
                document.add(p);
                if (work.getDescription() != null && !work.getDescription().isBlank()) {
                    document.add(new Paragraph(work.getDescription())
                            .setFont(regularFont).setFontSize(9).setMarginLeft(12).setMarginBottom(4));
                }
                if (work.getReferenceContact() != null && !work.getReferenceContact().isBlank()) {
                    document.add(new Paragraph("Referencia: " + work.getReferenceContact())
                            .setFont(regularFont).setFontSize(8).setFontColor(SECONDARY_COLOR).setMarginLeft(12).setMarginBottom(14));
                }
            }
        }

        if (!education.isEmpty()) {
            addMinimalistaSectionTitle(document, "EDUCACIÓN Y CAPACITACIONES", boldFont);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
            for (EducationResponse edu : education) {
                Paragraph p = new Paragraph().setMarginBottom(4);
                if (edu.getDegree() != null && !edu.getDegree().isBlank())
                    p.add(new Text(edu.getDegree() + "\n").setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK));
                if (edu.getInstitution() != null && !edu.getInstitution().isBlank())
                    p.add(new Text(edu.getInstitution()).setFont(boldFont).setFontSize(10).setFontColor(PRIMARY_COLOR));
                if (edu.getStartDate() != null) {
                    String dr = "  •  " + edu.getStartDate().format(fmt) + " — " +
                            (Boolean.TRUE.equals(edu.getCurrentlyStudying()) ? "Actualidad"
                                    : (edu.getEndDate() != null ? edu.getEndDate().format(fmt) : ""));
                    p.add(new Text(dr).setFont(regularFont).setFontSize(9).setFontColor(SECONDARY_COLOR));
                }
                document.add(p);
                if (edu.getDescription() != null && !edu.getDescription().isBlank())
                    document.add(new Paragraph(edu.getDescription()).setFont(regularFont).setFontSize(9).setMarginLeft(12).setMarginBottom(12));
                else
                    document.add(new Paragraph("").setMarginBottom(12));
            }
        }

        if (!certifications.isEmpty()) {
            addMinimalistaSectionTitle(document, "CERTIFICACIONES", boldFont);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
            for (CertificationResponse cert : certifications) {
                Paragraph p = new Paragraph().setMarginBottom(4);
                p.add(new Text(cert.getName() + "\n").setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK));
                if (cert.getIssuer() != null && !cert.getIssuer().isBlank())
                    p.add(new Text(cert.getIssuer()).setFont(boldFont).setFontSize(10).setFontColor(PRIMARY_COLOR));
                if (cert.getDateObtained() != null) {
                    String di = "  •  " + cert.getDateObtained().format(fmt);
                    if (cert.getExpiryDate() != null) di += " — vence " + cert.getExpiryDate().format(fmt);
                    p.add(new Text(di).setFont(regularFont).setFontSize(9).setFontColor(SECONDARY_COLOR));
                }
                document.add(p);
                document.add(new Paragraph("").setMarginBottom(12));
            }
        }

        document.close();
        return baos.toByteArray();
    }

    private void addMinimalistaSectionTitle(Document document, String title, PdfFont boldFont) {
        document.add(new Paragraph(title).setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK)
                .setBorderBottom(new SolidBorder(new DeviceRgb(203, 213, 225), 1))
                .setMarginBottom(10).setMarginTop(5));
    }

    private byte[] buildEjecutivoLayout(AppUser professional, List<WorkHistory> workHistory,
            List<EducationResponse> education, List<CertificationResponse> certifications,
            PdfGenerationRequest request) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
        Document document = new Document(pdfDoc);
        document.setMargins(0, 0, 40, 0);

        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Header: banda de color con foto + nombre + título
        DeviceRgb EJECUTIVO_DARK = new DeviceRgb(30, 41, 59);  // Slate 800
        float[] headerWidths = {25f, 75f};
        Table headerTable = new Table(UnitValue.createPercentArray(headerWidths))
                .setWidth(UnitValue.createPercentValue(100)).setBorder(null);

        // Foto en header
        Cell photoCell = new Cell().setBackgroundColor(EJECUTIVO_DARK).setBorder(null)
                .setPaddingTop(30).setPaddingBottom(30).setPaddingLeft(30).setVerticalAlignment(VerticalAlignment.MIDDLE);
        float size = 85f;
        if (professional.getProfilePicture() != null && !professional.getProfilePicture().isEmpty()) {
            try {
                Image photo = new Image(ImageDataFactory.create(professional.getProfilePicture()));
                photo.scaleToFit(size, size).setBorder(new SolidBorder(new DeviceRgb(100, 116, 139), 2));
                photoCell.add(photo);
            } catch (Exception e) {
                photoCell.add(initialCircle(professional, boldFont, size, EJECUTIVO_DARK));
            }
        } else {
            photoCell.add(initialCircle(professional, boldFont, size, EJECUTIVO_DARK));
        }
        headerTable.addCell(photoCell);

        // Nombre + título + contacto en header
        Cell nameCell = new Cell().setBackgroundColor(EJECUTIVO_DARK).setBorder(null)
                .setPaddingTop(35).setPaddingLeft(20).setPaddingRight(30).setVerticalAlignment(VerticalAlignment.MIDDLE);
        nameCell.add(new Paragraph(professional.getName().toUpperCase())
                .setFont(boldFont).setFontSize(26).setFontColor(ColorConstants.WHITE).setMarginBottom(4));
        if (professional.getProfessionalTitle() != null) {
            nameCell.add(new Paragraph(professional.getProfessionalTitle())
                    .setFont(regularFont).setFontSize(13).setFontColor(new DeviceRgb(148, 163, 184)).setMarginBottom(12));
        }
        // Contacto compacto en el header
        StringBuilder contact = new StringBuilder(professional.getEmail());
        if (professional.getPhone() != null) contact.append("   |   ").append(professional.getPhone());
        if (professional.getLocation() != null) contact.append("   |   ").append(professional.getLocation());
        if (professional.getBirthDate() != null) contact.append("   |   ").append(professional.getBirthDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        nameCell.add(new Paragraph(contact.toString())
                .setFont(regularFont).setFontSize(9).setFontColor(new DeviceRgb(148, 163, 184)).setMarginBottom(10));

        // Reputación en header
        double score = professional.getReputationScore() != null ? professional.getReputationScore() : 0.0;
        String stars = "★".repeat((int) Math.round(score)) + "☆".repeat(5 - (int) Math.round(score));
        nameCell.add(new Paragraph(stars + "  " + String.format("%.1f", score) + " (" + professional.getTotalRatings() + " reseñas)")
                .setFontSize(12).setFontColor(STAR_YELLOW).setMarginBottom(30));
        headerTable.addCell(nameCell);
        document.add(headerTable);

        // Acento inferior del header
        document.add(new Paragraph("").setHeight(4).setBackgroundColor(PRIMARY_COLOR).setMarginBottom(30));

        // Contenido con márgenes laterales
        document.setLeftMargin(50);
        document.setRightMargin(50);

        if (request.isIncludeDescription() && professional.getCv() != null && professional.getCv().getDescription() != null) {
            addEjecutivoSectionTitle(document, "PERFIL PROFESIONAL", boldFont);
            document.add(new Paragraph(professional.getCv().getDescription())
                    .setFont(regularFont).setFontSize(10).setMultipliedLeading(1.5f)
                    .setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(20));
        }

        if (request.isIncludeSkills() && professional.getCv() != null
                && professional.getCv().getSkills() != null && !professional.getCv().getSkills().isBlank()) {
            addEjecutivoSectionTitle(document, "APTITUDES Y HABILIDADES", boldFont);
            String[] tags = professional.getCv().getSkills().split(",");
            Paragraph skillsPara = new Paragraph().setMarginBottom(20);
            for (String tag : tags) {
                String t = tag.trim();
                if (!t.isEmpty()) {
                    skillsPara.add(new Text("  " + t + "  ").setFont(regularFont).setFontSize(9)
                            .setFontColor(ColorConstants.WHITE).setBackgroundColor(EJECUTIVO_DARK));
                    skillsPara.add(new Text("  ").setFont(regularFont));
                }
            }
            document.add(skillsPara);
        }

        if (!workHistory.isEmpty()) {
            addEjecutivoSectionTitle(document, "EXPERIENCIA LABORAL", boldFont);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
            for (WorkHistory work : workHistory) {
                Paragraph p = new Paragraph().setMarginBottom(4);
                p.add(new Text(work.getPosition() + "\n").setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK));
                p.add(new Text(work.getBusinessName()).setFont(boldFont).setFontSize(10).setFontColor(PRIMARY_COLOR));
                String dr = "  •  " + work.getStartDate().format(fmt) + " — " +
                        (work.getEndDate() != null ? work.getEndDate().format(fmt) : "Actualidad");
                p.add(new Text(dr).setFont(regularFont).setFontSize(9).setFontColor(SECONDARY_COLOR));
                document.add(p);
                if (work.getDescription() != null && !work.getDescription().isBlank())
                    document.add(new Paragraph(work.getDescription()).setFont(regularFont).setFontSize(9).setMarginLeft(12).setMarginBottom(4));
                if (work.getReferenceContact() != null && !work.getReferenceContact().isBlank())
                    document.add(new Paragraph("Referencia: " + work.getReferenceContact())
                            .setFont(regularFont).setFontSize(8).setFontColor(SECONDARY_COLOR).setMarginLeft(12).setMarginBottom(14));
            }
        }

        if (!education.isEmpty()) {
            addEjecutivoSectionTitle(document, "EDUCACIÓN Y CAPACITACIONES", boldFont);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
            for (EducationResponse edu : education) {
                Paragraph p = new Paragraph().setMarginBottom(4);
                if (edu.getDegree() != null && !edu.getDegree().isBlank())
                    p.add(new Text(edu.getDegree() + "\n").setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK));
                if (edu.getInstitution() != null && !edu.getInstitution().isBlank())
                    p.add(new Text(edu.getInstitution()).setFont(boldFont).setFontSize(10).setFontColor(PRIMARY_COLOR));
                if (edu.getStartDate() != null) {
                    String dr = "  •  " + edu.getStartDate().format(fmt) + " — " +
                            (Boolean.TRUE.equals(edu.getCurrentlyStudying()) ? "Actualidad"
                                    : (edu.getEndDate() != null ? edu.getEndDate().format(fmt) : ""));
                    p.add(new Text(dr).setFont(regularFont).setFontSize(9).setFontColor(SECONDARY_COLOR));
                }
                document.add(p);
                if (edu.getDescription() != null && !edu.getDescription().isBlank())
                    document.add(new Paragraph(edu.getDescription()).setFont(regularFont).setFontSize(9).setMarginLeft(12).setMarginBottom(12));
                else
                    document.add(new Paragraph("").setMarginBottom(12));
            }
        }

        if (!certifications.isEmpty()) {
            addEjecutivoSectionTitle(document, "CERTIFICACIONES", boldFont);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
            for (CertificationResponse cert : certifications) {
                Paragraph p = new Paragraph().setMarginBottom(4);
                p.add(new Text(cert.getName() + "\n").setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK));
                if (cert.getIssuer() != null && !cert.getIssuer().isBlank())
                    p.add(new Text(cert.getIssuer()).setFont(boldFont).setFontSize(10).setFontColor(PRIMARY_COLOR));
                if (cert.getDateObtained() != null) {
                    String di = "  •  " + cert.getDateObtained().format(fmt);
                    if (cert.getExpiryDate() != null) di += " — vence " + cert.getExpiryDate().format(fmt);
                    p.add(new Text(di).setFont(regularFont).setFontSize(9).setFontColor(SECONDARY_COLOR));
                }
                document.add(p);
                document.add(new Paragraph("").setMarginBottom(12));
            }
        }

        document.close();
        return baos.toByteArray();
    }

    private Paragraph initialCircle(AppUser professional, PdfFont boldFont, float size, DeviceRgb bg) {
        return new Paragraph(professional.getName().substring(0, 1))
                .setFont(boldFont).setFontSize(32).setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(bg).setTextAlignment(TextAlignment.CENTER)
                .setWidth(size).setHeight(size).setPaddingTop(18);
    }

    private void addEjecutivoSectionTitle(Document document, String title, PdfFont boldFont) {
        document.add(new Paragraph(title).setFont(boldFont).setFontSize(12).setFontColor(TEXT_DARK)
                .setBorderLeft(new SolidBorder(new DeviceRgb(30, 41, 59), 3))
                .setPaddingLeft(10).setMarginBottom(12).setMarginTop(8));
    }

    // ===== HELPERS DE FILTRADO =====

    private List<WorkHistory> filterWorkHistory(List<WorkHistory> all, List<Long> ids) {
        if (ids == null) return all;
        return all.stream().filter(w -> ids.contains(w.getId())).collect(Collectors.toList());
    }

    private <T> List<T> filterById(List<T> all, List<Long> ids, Function<T, Long> idExtractor) {
        if (ids == null) return all;
        return all.stream().filter(item -> ids.contains(idExtractor.apply(item))).collect(Collectors.toList());
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
                cell.add(new Paragraph(work.getDescription()).setFont(regularFont).setFontSize(9).setMarginLeft(10).setMarginBottom(4));
            }
            if (work.getReferenceContact() != null && !work.getReferenceContact().isBlank()) {
                String ref = work.getReferenceContact() +
                        (work.getReferencePhone() != null && !work.getReferencePhone().isBlank() ? " — " + work.getReferencePhone() : "");
                cell.add(new Paragraph("Referencia: " + ref)
                        .setFont(regularFont).setFontSize(8).setFontColor(SECONDARY_COLOR).setMarginLeft(10).setMarginBottom(15));
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