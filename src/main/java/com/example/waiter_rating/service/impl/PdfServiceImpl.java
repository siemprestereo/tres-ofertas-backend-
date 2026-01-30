package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.WorkHistory;
import com.example.waiter_rating.repository.WorkHistoryRepo;
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
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfServiceImpl implements PdfService {

    private final ProfessionalRepo professionalRepo;
    private final WorkHistoryRepo workHistoryRepo;

    // Colores personalizados
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(79, 70, 229); // Azul/Púrpura
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(107, 114, 128); // Gris
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(248, 250, 252); // Gris muy claro para sidebar
    private static final DeviceRgb DARK_GRAY = new DeviceRgb(31, 41, 55); // Gris oscuro para texto

    public PdfServiceImpl(ProfessionalRepo professionalRepo, WorkHistoryRepo workHistoryRepo) {
        this.professionalRepo = professionalRepo;
        this.workHistoryRepo = workHistoryRepo;
    }

    @Override
    public byte[] generateCvPdf(Long professionalId) throws Exception {
        // Obtener datos del profesional
        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        List<WorkHistory> workHistory = workHistoryRepo.findByProfessionalId(professionalId);

        // Crear PDF en memoria
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Eliminar márgenes por defecto
        document.setMargins(0, 0, 0, 0);

        // Fuentes
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ==================== LAYOUT DOS COLUMNAS ====================
        float[] columnWidths = {30f, 70f};
        Table mainTable = new Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(null);

        // ==================== COLUMNA IZQUIERDA ====================
        Cell leftColumn = new Cell()
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(null)
                .setPadding(25)
                .setVerticalAlignment(VerticalAlignment.TOP);

        // Foto placeholder
        addProfilePhoto(leftColumn, professional, boldFont);

        // Espacio
        leftColumn.add(new Paragraph("\n"));

        // Contacto
        addContactSection(leftColumn, professional, boldFont, regularFont);

        // Espacio
        leftColumn.add(new Paragraph("\n"));

        // Reputación
        addReputationSection(leftColumn, professional, boldFont, regularFont);

        mainTable.addCell(leftColumn);

        // ==================== COLUMNA DERECHA ====================
        Cell rightColumn = new Cell()
                .setBorder(null)
                .setPadding(25)
                .setVerticalAlignment(VerticalAlignment.TOP);

        // Header con nombre
        addNameHeader(rightColumn, professional, boldFont, regularFont);

        // Sobre mí
        if (professional.getCv() != null && professional.getCv().getDescription() != null
                && !professional.getCv().getDescription().isEmpty()) {
            addRightSectionTitle(rightColumn, "SOBRE MÍ", boldFont);
            Paragraph description = new Paragraph(professional.getCv().getDescription())
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(20);
            rightColumn.add(description);
        }

        // Experiencia laboral
        if (!workHistory.isEmpty()) {
            addRightSectionTitle(rightColumn, "EXPERIENCIA", boldFont);
            addWorkHistoryModern(rightColumn, workHistory, boldFont, regularFont);
        }

        mainTable.addCell(rightColumn);

        // Agregar tabla principal
        document.add(mainTable);

        // Footer
        addFooterModern(document, regularFont);

        document.close();
        return baos.toByteArray();
    }

    private void addProfilePhoto(Cell cell, Professional professional, PdfFont boldFont) {
        String initial = professional.getName().substring(0, 1).toUpperCase();

        // Si tiene foto, intentar cargarla
        if (professional.getProfilePicture() != null && !professional.getProfilePicture().isEmpty()) {
            try {
                // Construir ruta absoluta desde la ruta relativa
                String photoPath = professional.getProfilePicture();
                if (photoPath.startsWith("/")) {
                    photoPath = photoPath.substring(1); // Quitar el "/" inicial
                }

                File photoFile = new File(photoPath);

                if (photoFile.exists()) {
                    // Cargar imagen
                    Image photo = new Image(ImageDataFactory.create(photoFile.getAbsolutePath()));

                    // Configurar imagen con aspect ratio preservado
                    // Determinar si es más ancha o más alta
                    float imgWidth = photo.getImageWidth();
                    float imgHeight = photo.getImageHeight();

                    // Calcular tamaño manteniendo proporción
                    float targetSize = 80f;
                    if (imgWidth > imgHeight) {
                        // Imagen más ancha que alta
                        photo.scaleToFit(targetSize, targetSize * (imgHeight / imgWidth));
                    } else {
                        // Imagen más alta que ancha
                        photo.scaleToFit(targetSize * (imgWidth / imgHeight), targetSize);
                    }

                    // Si es cuadrada o casi cuadrada, ajustar a 80x80
                    if (Math.abs(imgWidth - imgHeight) < 10) {
                        photo.scaleToFit(targetSize, targetSize);
                    }

                    // Configurar bordes y márgenes
                    photo.setBorder(new SolidBorder(PRIMARY_COLOR, 3))
                            .setMarginBottom(15)
                            .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

                    cell.add(photo);
                    return; // Salir si la foto se cargó exitosamente
                }
            } catch (Exception e) {
                // Si falla, usar inicial como fallback
                System.err.println("Error cargando foto de perfil: " + e.getMessage());
            }
        }

        // Fallback: Círculo con inicial
        Paragraph photoPlaceholder = new Paragraph(initial)
                .setFont(boldFont)
                .setFontSize(40)
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setWidth(80)
                .setHeight(80)
                .setPadding(15)
                .setMarginBottom(15);

        cell.add(photoPlaceholder);
    }

    private void addContactSection(Cell cell, Professional professional, PdfFont boldFont, PdfFont regularFont) {
        // Título "CONTACTO"
        Paragraph title = new Paragraph("CONTACTO")
                .setFont(boldFont)
                .setFontSize(12)
                .setFontColor(DARK_GRAY)
                .setMarginBottom(10);
        cell.add(title);

        // Email
        addContactItem(cell, "Email", professional.getEmail(), regularFont);

        // Teléfono
        if (professional.getPhone() != null && !professional.getPhone().isEmpty()) {
            addContactItem(cell, "Teléfono", professional.getPhone(), regularFont);
        }

        // Ubicación
        if (professional.getLocation() != null && !professional.getLocation().isEmpty()) {
            addContactItem(cell, "Ubicación", professional.getLocation(), regularFont);
        }
    }

    private void addContactItem(Cell cell, String label, String value, PdfFont regularFont) {
        Paragraph labelP = new Paragraph(label)
                .setFont(regularFont)
                .setFontSize(9)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(2)
                .setBold();
        cell.add(labelP);

        Paragraph valueP = new Paragraph(value)
                .setFont(regularFont)
                .setFontSize(9)
                .setFontColor(DARK_GRAY)
                .setMarginBottom(10);
        cell.add(valueP);
    }

    private void addReputationSection(Cell cell, Professional professional, PdfFont boldFont, PdfFont regularFont) {
        double reputationScore = professional.getReputationScore() != null ? professional.getReputationScore() : 0.0;
        int totalRatings = professional.getTotalRatings() != null ? professional.getTotalRatings() : 0;

        // Título
        Paragraph title = new Paragraph("REPUTACIÓN")
                .setFont(boldFont)
                .setFontSize(12)
                .setFontColor(DARK_GRAY)
                .setMarginBottom(10);
        cell.add(title);

        // Estrellas visuales
        String stars = "★".repeat((int) Math.round(reputationScore)) +
                "☆".repeat(5 - (int) Math.round(reputationScore));

        Paragraph starsP = new Paragraph(stars)
                .setFont(regularFont)
                .setFontSize(16)
                .setFontColor(new DeviceRgb(251, 191, 36)) // Amarillo
                .setMarginBottom(5);
        cell.add(starsP);

        // Promedio
        Paragraph avgP = new Paragraph(String.format("%.1f / 5.0", reputationScore))
                .setFont(boldFont)
                .setFontSize(11)
                .setFontColor(DARK_GRAY)
                .setMarginBottom(3);
        cell.add(avgP);

        // Total calificaciones
        Paragraph totalP = new Paragraph(totalRatings + " calificaciones")
                .setFont(regularFont)
                .setFontSize(9)
                .setFontColor(SECONDARY_COLOR);
        cell.add(totalP);
    }

    private void addNameHeader(Cell cell, Professional professional, PdfFont boldFont, PdfFont regularFont) {
        // Nombre
        Paragraph name = new Paragraph(professional.getName().toUpperCase())
                .setFont(boldFont)
                .setFontSize(26)
                .setFontColor(DARK_GRAY)
                .setMarginBottom(5);
        cell.add(name);

        // Título profesional
        if (professional.getProfessionalTitle() != null && !professional.getProfessionalTitle().isEmpty()) {
            Paragraph title = new Paragraph(professional.getProfessionalTitle())
                    .setFont(regularFont)
                    .setFontSize(14)
                    .setFontColor(PRIMARY_COLOR)
                    .setMarginBottom(15);
            cell.add(title);
        } else {
            cell.add(new Paragraph("\n").setMarginBottom(10));
        }

        // Línea divisora
        addDividerLine(cell);
    }

    private void addRightSectionTitle(Cell cell, String title, PdfFont boldFont) {
        Paragraph titleP = new Paragraph(title)
                .setFont(boldFont)
                .setFontSize(13)
                .setFontColor(DARK_GRAY)
                .setMarginTop(10)
                .setMarginBottom(10);
        cell.add(titleP);

        // Línea divisora
        addDividerLine(cell);
    }

    private void addWorkHistoryModern(Cell cell, List<WorkHistory> workHistory, PdfFont boldFont, PdfFont regularFont) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = 0; i < workHistory.size(); i++) {
            WorkHistory work = workHistory.get(i);

            // Puesto
            Paragraph position = new Paragraph(work.getPosition())
                    .setFont(boldFont)
                    .setFontSize(12)
                    .setFontColor(DARK_GRAY)
                    .setMarginBottom(3);
            cell.add(position);

            // Empresa + Fechas en la misma línea
            String startDate = work.getStartDate().format(formatter);
            String endDate = work.getEndDate() != null ? work.getEndDate().format(formatter) : "Presente";

            Paragraph companyAndDates = new Paragraph()
                    .add(new Text(work.getBusinessName())
                            .setFont(regularFont)
                            .setFontSize(10)
                            .setFontColor(PRIMARY_COLOR)
                            .setBold())
                    .add(new Text("  •  " + startDate + " - " + endDate)
                            .setFont(regularFont)
                            .setFontSize(9)
                            .setFontColor(SECONDARY_COLOR))
                    .setMarginBottom(5);
            cell.add(companyAndDates);

            // Descripción con bullets
            if (work.getDescription() != null && !work.getDescription().isEmpty()) {
                // Dividir por líneas o comas
                String[] responsibilities = work.getDescription().split("[,\n]");
                for (String resp : responsibilities) {
                    String trimmed = resp.trim();
                    if (!trimmed.isEmpty()) {
                        Paragraph bullet = new Paragraph("• " + trimmed)
                                .setFont(regularFont)
                                .setFontSize(9)
                                .setFontColor(DARK_GRAY)
                                .setMarginLeft(10)
                                .setMarginBottom(2);
                        cell.add(bullet);
                    }
                }
            }

            // Badge "Activo"
            if (work.getIsActive()) {
                Paragraph activeBadge = new Paragraph("✓ Actualmente trabajando aquí")
                        .setFont(regularFont)
                        .setFontSize(8)
                        .setFontColor(new DeviceRgb(34, 197, 94)) // Verde
                        .setMarginTop(3);
                cell.add(activeBadge);
            }

            // Espacio entre trabajos (excepto el último)
            if (i < workHistory.size() - 1) {
                cell.add(new Paragraph("\n").setMarginBottom(5));
            }
        }
    }

    private void addDividerLine(Cell cell) {
        Table line = new Table(1)
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(null)
                .setMarginBottom(15);

        Cell lineCell = new Cell()
                .add(new Paragraph(""))
                .setHeight(1)
                .setBackgroundColor(new DeviceRgb(229, 231, 235)) // Gris claro
                .setBorder(null);

        line.addCell(lineCell);
        cell.add(line);
    }

    private void addFooterModern(Document document, PdfFont regularFont) {
        Paragraph footer = new Paragraph("Generado con ProRate - Sistema de Calificación Profesional")
                .setFont(regularFont)
                .setFontSize(7)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setMarginBottom(10);
        document.add(footer);
    }
}