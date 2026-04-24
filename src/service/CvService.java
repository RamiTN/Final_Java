package service;

import dao.CVDAO;
import model.Cv;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class CvService {
    private CVDAO cvDAO = new CVDAO();

    public boolean createCv(Cv cv) { return cvDAO.insert(cv); }
    public boolean updateCv(Cv cv) { return cvDAO.update(cv); }
    public boolean deleteCv(int id) { return cvDAO.delete(id); }
    public List<Cv> getCvsByUser(int userId) { return cvDAO.findByUserId(userId); }
    public Cv getCvById(int id) { return cvDAO.findById(id); }

    /**
     * Generate a temporary PDF preview of the CV and return the file path.
     * Opens with the OS default PDF viewer.
     */
    public String generatePreviewPdf(Cv cv, String profilePicturePath) {
        try {
            File temp = File.createTempFile("cv_preview_", ".pdf");
            temp.deleteOnExit();
            if (exportToPdf(cv, temp.getAbsolutePath(), profilePicturePath)) {
                return temp.getAbsolutePath();
            }
        } catch (IOException e) { e.printStackTrace(); }
        return null;
    }

    /** Export CV to PDF without profile picture */
    public boolean exportToPdf(Cv cv, String filePath) {
        return exportToPdf(cv, filePath, null);
    }

    /** Export CV to PDF with optional profile picture */
    public boolean exportToPdf(Cv cv, String filePath, String profilePicturePath) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont bold   = PDType1Font.HELVETICA_BOLD;
            PDFont normal = PDType1Font.HELVETICA;

            float pageWidth  = page.getMediaBox().getWidth();
            float margin     = 50;
            float y          = 780;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // ── Header background bar
                cs.setNonStrokingColor(new Color(33, 97, 140));
                cs.addRect(0, 740, pageWidth, 75);
                cs.fill();

                // ── Profile picture (if provided)
                boolean hasPic = false;
                if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
                    File picFile = new File(profilePicturePath);
                    if (picFile.exists()) {
                        try {
                            PDImageXObject image = PDImageXObject.createFromFile(profilePicturePath, doc);
                            float picSize = 55;
                            cs.drawImage(image, margin, 748, picSize, picSize);
                            hasPic = true;
                        } catch (Exception ignored) {}
                    }
                }

                float textStartX = hasPic ? margin + 65 : 0;

                // ── Full Name
                cs.setNonStrokingColor(Color.WHITE);
                cs.beginText();
                cs.setFont(bold, 22);
                String name = safe(cv.getFullName());
                if (hasPic) {
                    cs.newLineAtOffset(textStartX, 778);
                } else {
                    float nameWidth = bold.getStringWidth(name) / 1000 * 22;
                    cs.newLineAtOffset((pageWidth - nameWidth) / 2, 778);
                }
                cs.showText(name);
                cs.endText();

                // ── Contact line
                cs.setNonStrokingColor(new Color(220, 230, 240));
                cs.beginText();
                cs.setFont(normal, 9);
                String contact = safe(cv.getEmail()) + "  |  " + safe(cv.getPhone()) + "  |  " + safe(cv.getAddress());
                if (hasPic) {
                    cs.newLineAtOffset(textStartX, 760);
                } else {
                    float contactWidth = normal.getStringWidth(contact) / 1000 * 9;
                    cs.newLineAtOffset((pageWidth - contactWidth) / 2, 754);
                }
                cs.showText(contact);
                cs.endText();

                y = 725;

                // ── Sections
                y = addSection(cs, doc, page, bold, normal, "Objective",  cv.getObjective(),  y, margin, pageWidth);
                y = addSection(cs, doc, page, bold, normal, "Education",  cv.getEducation(),  y, margin, pageWidth);
                y = addSection(cs, doc, page, bold, normal, "Experience", cv.getExperience(), y, margin, pageWidth);
                y = addSection(cs, doc, page, bold, normal, "Skills",     cv.getSkills(),     y, margin, pageWidth);
                y = addSection(cs, doc, page, bold, normal, "Languages",  cv.getLanguages(),  y, margin, pageWidth);
            }

            doc.save(filePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private float addSection(PDPageContentStream cs, PDDocument doc, PDPage page,
                             PDFont bold, PDFont normal,
                             String title, String content,
                             float y, float margin, float pageWidth) throws IOException {
        float usableWidth = pageWidth - 2 * margin;

        // Section title
        y -= 18;
        cs.setNonStrokingColor(new Color(52, 73, 94));
        cs.beginText();
        cs.setFont(bold, 12);
        cs.newLineAtOffset(margin, y);
        cs.showText(title);
        cs.endText();

        // Underline
        y -= 4;
        cs.setStrokingColor(new Color(180, 180, 180));
        cs.setLineWidth(0.5f);
        cs.moveTo(margin, y);
        cs.lineTo(pageWidth - margin, y);
        cs.stroke();

        // Content — word-wrap
        y -= 4;
        cs.setNonStrokingColor(Color.BLACK);
        cs.setFont(normal, 10);

        String text = safe(content);
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float fontSize = 10;
        float lineHeight = 14;

        for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            float testWidth = normal.getStringWidth(test) / 1000 * fontSize;
            if (testWidth > usableWidth && !line.isEmpty()) {
                y -= lineHeight;
                cs.beginText();
                cs.newLineAtOffset(margin + 8, y);
                cs.showText(line.toString());
                cs.endText();
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (!line.isEmpty()) {
            y -= lineHeight;
            cs.beginText();
            cs.newLineAtOffset(margin + 8, y);
            cs.showText(line.toString());
            cs.endText();
        }

        return y;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}