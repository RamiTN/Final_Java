package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Cv;
import model.User;
import service.AiService;
import service.AuthService;
import service.CvService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CVController {

    @FXML private VBox rootPane;
    @FXML private ComboBox<Cv> cvCombo;
    @FXML private TextField fullNameField, cvEmailField, phoneField, addressField;
    @FXML private TextArea objectiveField;
    @FXML private TextArea aiAdviceArea;
    @FXML private Label messageLabel;

    // Dynamic containers from FXML
    @FXML private VBox educationContainer;
    @FXML private VBox experienceContainer;
    @FXML private FlowPane skillTagsPane;
    @FXML private TextField skillInputField;
    @FXML private VBox languagesContainer;

    private CvService cvService = new CvService();
    private Cv selectedCv = null;
    private List<String> skillsList = new ArrayList<>();

    @FXML
    public void initialize() {
        loadCvList();
    }

    private void loadCvList() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;
        List<Cv> cvs = cvService.getCvsByUser(user.getId());
        cvCombo.setItems(FXCollections.observableArrayList(cvs));
        selectedCv = null;
        clearForm();
    }

    @FXML
    private void handleSelectCv() {
        selectedCv = cvCombo.getValue();
        if (selectedCv != null) {
            fullNameField.setText(safe(selectedCv.getFullName()));
            cvEmailField.setText(safe(selectedCv.getEmail()));
            phoneField.setText(safe(selectedCv.getPhone()));
            addressField.setText(safe(selectedCv.getAddress()));
            objectiveField.setText(safe(selectedCv.getObjective()));

            // Populate education entries
            educationContainer.getChildren().clear();
            if (selectedCv.getEducation() != null && !selectedCv.getEducation().isEmpty()) {
                for (String entry : selectedCv.getEducation().split("\\n---\\n")) {
                    if (!entry.trim().isEmpty()) {
                        addEducationRow(entry.trim());
                    }
                }
            }

            // Populate experience entries
            experienceContainer.getChildren().clear();
            if (selectedCv.getExperience() != null && !selectedCv.getExperience().isEmpty()) {
                for (String entry : selectedCv.getExperience().split("\\n---\\n")) {
                    if (!entry.trim().isEmpty()) {
                        addExperienceRow(entry.trim());
                    }
                }
            }

            // Populate skills
            skillsList.clear();
            skillTagsPane.getChildren().clear();
            if (selectedCv.getSkills() != null && !selectedCv.getSkills().isEmpty()) {
                for (String skill : selectedCv.getSkills().split(",")) {
                    if (!skill.trim().isEmpty()) {
                        skillsList.add(skill.trim());
                        addSkillTag(skill.trim());
                    }
                }
            }

            // Populate languages
            languagesContainer.getChildren().clear();
            if (selectedCv.getLanguages() != null && !selectedCv.getLanguages().isEmpty()) {
                for (String lang : selectedCv.getLanguages().split(",")) {
                    if (!lang.trim().isEmpty()) {
                        addLanguageRow(lang.trim());
                    }
                }
            }
        }
    }

    // ══════════════════════════ EDUCATION ══════════════════════════

    @FXML
    private void handleAddEducation() {
        addEducationRow("");
    }

    private void addEducationRow(String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-item");

        TextField field = new TextField(value);
        field.setPromptText("e.g. BSc Computer Science — ESPRIT (2022–2025)");
        field.getStyleClass().add("form-field");
        HBox.setHgrow(field, Priority.ALWAYS);

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("danger-btn");
        removeBtn.setStyle("-fx-padding: 4 10; -fx-font-size: 11;");
        removeBtn.setOnAction(e -> educationContainer.getChildren().remove(row));

        row.getChildren().addAll(field, removeBtn);
        educationContainer.getChildren().add(row);
    }

    // ══════════════════════════ EXPERIENCE ══════════════════════════

    @FXML
    private void handleAddExperience() {
        addExperienceRow("");
    }

    private void addExperienceRow(String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-item");

        TextField field = new TextField(value);
        field.setPromptText("e.g. Java Developer — Company XYZ (2023–2024)");
        field.getStyleClass().add("form-field");
        HBox.setHgrow(field, Priority.ALWAYS);

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("danger-btn");
        removeBtn.setStyle("-fx-padding: 4 10; -fx-font-size: 11;");
        removeBtn.setOnAction(e -> experienceContainer.getChildren().remove(row));

        row.getChildren().addAll(field, removeBtn);
        experienceContainer.getChildren().add(row);
    }

    // ══════════════════════════ SKILLS ══════════════════════════

    @FXML
    private void handleAddSkill() {
        if (skillInputField == null) return;
        String skill = skillInputField.getText().trim();
        if (skill.isEmpty()) {
            msg("⚠ Please enter a skill name.");
            return;
        }
        if (skillsList.contains(skill)) {
            msg("⚠ Skill \"" + skill + "\" already added.");
            skillInputField.setText("");
            return;
        }
        skillsList.add(skill);
        addSkillTag(skill);
        skillInputField.setText("");
    }

    private void addSkillTag(String skill) {
        HBox tag = new HBox(6);
        tag.setAlignment(Pos.CENTER);
        tag.setStyle("-fx-background-color: rgba(74,124,255,0.15); -fx-background-radius: 16; -fx-padding: 4 12;");

        Label lbl = new Label(skill);
        lbl.setStyle("-fx-text-fill: #7ca3ff; -fx-font-size: 12; -fx-font-weight: bold;");

        Button x = new Button("✕");
        x.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff6b6b; -fx-cursor: hand; -fx-padding: 0 2; -fx-font-size: 10;");
        x.setOnAction(e -> {
            skillsList.remove(skill);
            skillTagsPane.getChildren().remove(tag);
        });

        tag.getChildren().addAll(lbl, x);
        skillTagsPane.getChildren().add(tag);
    }

    // ══════════════════════════ LANGUAGES ══════════════════════════

    @FXML
    private void handleAddLanguage() {
        addLanguageRow("");
    }

    private void addLanguageRow(String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-item");

        ComboBox<String> langCombo = new ComboBox<>();
        langCombo.getItems().addAll("Arabic", "French", "English", "German", "Spanish", "Italian", "Chinese", "Other");
        langCombo.setPromptText("Language");
        langCombo.getStyleClass().add("combo-box");
        langCombo.setPrefWidth(160);

        ComboBox<String> levelCombo = new ComboBox<>();
        levelCombo.getItems().addAll("Native", "Fluent", "Advanced", "Intermediate", "Beginner");
        levelCombo.setPromptText("Level");
        levelCombo.getStyleClass().add("combo-box");
        levelCombo.setPrefWidth(140);

        // Parse existing value like "French (Fluent)"
        if (!value.isEmpty()) {
            if (value.contains("(")) {
                String lang = value.substring(0, value.indexOf("(")).trim();
                String level = value.substring(value.indexOf("(") + 1, value.indexOf(")")).trim();
                langCombo.setValue(lang);
                levelCombo.setValue(level);
            } else {
                langCombo.setValue(value);
            }
        }

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("danger-btn");
        removeBtn.setStyle("-fx-padding: 4 10; -fx-font-size: 11;");
        removeBtn.setOnAction(e -> languagesContainer.getChildren().remove(row));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(langCombo, levelCombo, spacer, removeBtn);
        languagesContainer.getChildren().add(row);
    }

    // ══════════════════════════ CRUD ══════════════════════════

    @FXML
    private void handleCreate() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        // Validation
        if (!validateForm()) return;

        Cv cv = buildCvFromForm();
        cv.setUserId(user.getId());
        if (cvService.createCv(cv)) {
            msg("✔ CV created successfully!");
            messageLabel.getStyleClass().remove("message-label");
            messageLabel.getStyleClass().add("success-message");
            loadCvList();
        } else {
            msg("✖ Error creating CV.");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedCv == null) { msg("⚠ Select a CV first."); return; }

        // Validation
        if (!validateForm()) return;

        Cv cv = buildCvFromForm();
        cv.setId(selectedCv.getId());
        cv.setUserId(selectedCv.getUserId());
        if (cvService.updateCv(cv)) {
            msg("✔ CV updated successfully!");
            messageLabel.getStyleClass().remove("message-label");
            messageLabel.getStyleClass().add("success-message");
            loadCvList();
        } else {
            msg("✖ Error updating CV.");
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedCv == null) { msg("⚠ Select a CV first."); return; }
        if (cvService.deleteCv(selectedCv.getId())) {
            msg("✔ CV deleted.");
            messageLabel.getStyleClass().remove("message-label");
            messageLabel.getStyleClass().add("success-message");
            loadCvList();
        } else {
            msg("✖ Error deleting CV.");
        }
    }

    @FXML
    private void handleDownload() {
        if (selectedCv == null) { msg("⚠ Select a CV first."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save CV as PDF");
        fc.setInitialFileName(selectedCv.getFullName() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(rootPane.getScene().getWindow());
        if (file != null) {
            if (cvService.exportToPdf(selectedCv, file.getAbsolutePath())) {
                msg("✔ PDF saved successfully!");
            } else {
                msg("✖ Error saving PDF.");
            }
        }
    }

    @FXML
    private void handleAiAdvice() {
        Cv cv = buildCvFromForm();
        if (cv.getFullName() == null || cv.getFullName().trim().isEmpty()) {
            msg("⚠ Fill in your CV data before requesting AI advice.");
            return;
        }
        String prompt = "You are a CV expert. Review this CV data and give short, practical advice to improve it.\n"
                + "Full Name: " + safe(cv.getFullName()) + "\n"
                + "Email: " + safe(cv.getEmail()) + "\n"
                + "Objective: " + safe(cv.getObjective()) + "\n"
                + "Education: " + safe(cv.getEducation()) + "\n"
                + "Experience: " + safe(cv.getExperience()) + "\n"
                + "Skills: " + safe(cv.getSkills()) + "\n"
                + "Languages: " + safe(cv.getLanguages()) + "\n"
                + "Give specific suggestions to improve each section. Be concise.";
        aiAdviceArea.setText("Asking AI...");
        new Thread(() -> {
            String response = AiService.askGemini(prompt);
            javafx.application.Platform.runLater(() -> aiAdviceArea.setText(response));
        }).start();
    }

    // ══════════════════════════ VALIDATION ══════════════════════════

    private boolean validateForm() {
        messageLabel.getStyleClass().remove("success-message");
        messageLabel.getStyleClass().add("message-label");

        String name = fullNameField.getText().trim();
        String email = cvEmailField.getText().trim();

        if (name.isEmpty()) {
            msg("⚠ Full Name is required.");
            fullNameField.requestFocus();
            return false;
        }
        if (name.length() < 3) {
            msg("⚠ Full Name must be at least 3 characters.");
            fullNameField.requestFocus();
            return false;
        }
        if (email.isEmpty()) {
            msg("⚠ Email is required.");
            cvEmailField.requestFocus();
            return false;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            msg("⚠ Please enter a valid email address.");
            cvEmailField.requestFocus();
            return false;
        }

        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^[+]?[0-9\\s\\-]{6,20}$")) {
            msg("⚠ Please enter a valid phone number.");
            phoneField.requestFocus();
            return false;
        }

        return true;
    }

    // ══════════════════════════ HELPERS ══════════════════════════

    private Cv buildCvFromForm() {
        Cv cv = new Cv();
        cv.setFullName(fullNameField.getText().trim());
        cv.setEmail(cvEmailField.getText().trim());
        cv.setPhone(phoneField.getText().trim());
        cv.setAddress(addressField.getText().trim());
        cv.setObjective(objectiveField.getText() != null ? objectiveField.getText().trim() : "");

        // Collect education entries
        StringBuilder edu = new StringBuilder();
        for (var node : educationContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                for (var child : row.getChildren()) {
                    if (child instanceof TextField) {
                        String val = ((TextField) child).getText().trim();
                        if (!val.isEmpty()) {
                            if (edu.length() > 0) edu.append("\n---\n");
                            edu.append(val);
                        }
                    }
                }
            }
        }
        cv.setEducation(edu.toString());

        // Collect experience entries
        StringBuilder exp = new StringBuilder();
        for (var node : experienceContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                for (var child : row.getChildren()) {
                    if (child instanceof TextField) {
                        String val = ((TextField) child).getText().trim();
                        if (!val.isEmpty()) {
                            if (exp.length() > 0) exp.append("\n---\n");
                            exp.append(val);
                        }
                    }
                }
            }
        }
        cv.setExperience(exp.toString());

        // Skills
        cv.setSkills(String.join(", ", skillsList));

        // Languages
        StringBuilder langs = new StringBuilder();
        for (var node : languagesContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                String lang = "";
                String level = "";
                for (var child : row.getChildren()) {
                    if (child instanceof ComboBox) {
                        @SuppressWarnings("unchecked")
                        ComboBox<String> combo = (ComboBox<String>) child;
                        if (combo.getPromptText() != null && combo.getPromptText().equals("Language")) {
                            lang = combo.getValue() != null ? combo.getValue() : "";
                        } else if (combo.getPromptText() != null && combo.getPromptText().equals("Level")) {
                            level = combo.getValue() != null ? combo.getValue() : "";
                        }
                    }
                }
                if (!lang.isEmpty()) {
                    if (langs.length() > 0) langs.append(", ");
                    langs.append(lang);
                    if (!level.isEmpty()) langs.append(" (").append(level).append(")");
                }
            }
        }
        cv.setLanguages(langs.toString());

        return cv;
    }

    private void clearForm() {
        if (fullNameField != null) fullNameField.setText("");
        if (cvEmailField != null) cvEmailField.setText("");
        if (phoneField != null) phoneField.setText("");
        if (addressField != null) addressField.setText("");
        if (objectiveField != null) objectiveField.setText("");
        if (educationContainer != null) educationContainer.getChildren().clear();
        if (experienceContainer != null) experienceContainer.getChildren().clear();
        if (skillTagsPane != null) skillTagsPane.getChildren().clear();
        if (skillInputField != null) skillInputField.setText("");
        if (languagesContainer != null) languagesContainer.getChildren().clear();
        if (aiAdviceArea != null) aiAdviceArea.setText("");
        skillsList.clear();
        if (messageLabel != null) messageLabel.setText("");
    }

    private void msg(String t) {
        if (messageLabel != null) messageLabel.setText(t);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    @FXML private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
