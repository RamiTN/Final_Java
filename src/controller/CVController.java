package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Cv;
import model.User;
import service.AiService;
import service.AuthService;
import service.CvService;
import dao.UserDAO;
import service.DialogHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CVController {

    @FXML private VBox rootPane;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private TextField fullNameField, cvEmailField, phoneField, addressField;
    @FXML private TextArea objectiveField;
    @FXML private TextArea aiAdviceArea;
    @FXML private Label messageLabel;
    @FXML private HBox actionsBox;

    // Dynamic containers from FXML
    @FXML private VBox educationContainer;
    @FXML private VBox experienceContainer;
    @FXML private FlowPane skillTagsPane;
    @FXML private TextField skillInputField;
    @FXML private VBox languagesContainer;

    private CvService cvService = new CvService();
    private UserDAO userDAO = new UserDAO();
    private Cv selectedCv = null;
    private List<String> skillsList = new ArrayList<>();
    private boolean editMode = false;

    @FXML
    public void initialize() {
        // Check if we're editing an existing CV from the dashboard
        int editId = UserController.editCvId;
        if (editId > 0) {
            selectedCv = cvService.getCvById(editId);
            if (selectedCv != null) {
                editMode = true;
                UserController.editCvId = -1; // Reset after use
                pageTitleLabel.setText("Edit CV");
                pageSubtitleLabel.setText("Editing: " + (selectedCv.getFullName() != null ? selectedCv.getFullName() : "CV #" + selectedCv.getId()));
                populateForm(selectedCv);
            }
        }
        setupActionButtons();
    }

    private void setupActionButtons() {
        if (actionsBox == null) return;
        actionsBox.getChildren().clear();

        if (editMode && selectedCv != null) {
            // Edit mode: Save Changes + Download PDF + AI Advice
            Button saveBtn = new Button("✔  Save Changes");
            saveBtn.getStyleClass().add("success-btn");
            saveBtn.setPrefWidth(140);
            saveBtn.setOnAction(e -> handleUpdate());

            Button downloadBtn = new Button("⬇  Download PDF");
            downloadBtn.getStyleClass().add("accent-btn");
            downloadBtn.setPrefWidth(145);
            downloadBtn.setOnAction(e -> handleDownload());

            Button aiBtn = new Button("✦  AI Advice");
            aiBtn.getStyleClass().add("info-btn");
            aiBtn.setPrefWidth(120);
            aiBtn.setOnAction(e -> handleAiAdvice());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            actionsBox.getChildren().addAll(saveBtn, spacer, downloadBtn, aiBtn);
        } else {
            // Create mode: Create + AI Advice only
            Button createBtn = new Button("✔  Create");
            createBtn.getStyleClass().add("success-btn");
            createBtn.setPrefWidth(110);
            createBtn.setOnAction(e -> handleCreate());

            Button aiBtn = new Button("✦  AI Advice");
            aiBtn.getStyleClass().add("info-btn");
            aiBtn.setPrefWidth(120);
            aiBtn.setOnAction(e -> handleAiAdvice());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            actionsBox.getChildren().addAll(createBtn, spacer, aiBtn);
        }
    }

    private void populateForm(Cv cv) {
        fullNameField.setText(safe(cv.getFullName()));
        cvEmailField.setText(safe(cv.getEmail()));
        phoneField.setText(safe(cv.getPhone()));
        addressField.setText(safe(cv.getAddress()));
        objectiveField.setText(safe(cv.getObjective()));

        // Education
        educationContainer.getChildren().clear();
        if (cv.getEducation() != null && !cv.getEducation().isEmpty()) {
            for (String entry : cv.getEducation().split("\\n---\\n")) {
                if (!entry.trim().isEmpty()) addEducationRow(entry.trim());
            }
        }

        // Experience
        experienceContainer.getChildren().clear();
        if (cv.getExperience() != null && !cv.getExperience().isEmpty()) {
            for (String entry : cv.getExperience().split("\\n---\\n")) {
                if (!entry.trim().isEmpty()) addExperienceRow(entry.trim());
            }
        }

        // Skills
        skillsList.clear();
        skillTagsPane.getChildren().clear();
        if (cv.getSkills() != null && !cv.getSkills().isEmpty()) {
            for (String skill : cv.getSkills().split(",")) {
                if (!skill.trim().isEmpty()) {
                    skillsList.add(skill.trim());
                    addSkillTag(skill.trim());
                }
            }
        }

        // Languages
        languagesContainer.getChildren().clear();
        if (cv.getLanguages() != null && !cv.getLanguages().isEmpty()) {
            for (String lang : cv.getLanguages().split(",")) {
                if (!lang.trim().isEmpty()) addLanguageRow(lang.trim());
            }
        }
    }

    // ══════════════════════════ EDUCATION ══════════════════════════

    @FXML
    private void handleAddEducation() { addEducationRow(""); }

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
    private void handleAddExperience() { addExperienceRow(""); }

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
        if (skill.isEmpty()) { msg("⚠ Please enter a skill name."); return; }
        if (skillsList.contains(skill)) { msg("⚠ Skill \"" + skill + "\" already added."); skillInputField.setText(""); return; }
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
        x.setOnAction(e -> { skillsList.remove(skill); skillTagsPane.getChildren().remove(tag); });

        tag.getChildren().addAll(lbl, x);
        skillTagsPane.getChildren().add(tag);
    }

    // ══════════════════════════ LANGUAGES ══════════════════════════

    @FXML
    private void handleAddLanguage() { addLanguageRow(""); }

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

    private void handleCreate() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;
        if (!validateForm()) return;

        Cv cv = buildCvFromForm();
        cv.setUserId(user.getId());
        if (cvService.createCv(cv)) {
            msg("✔ CV created successfully!");
            messageLabel.getStyleClass().remove("message-label");
            messageLabel.getStyleClass().add("success-message");
            clearForm();
        } else {
            msg("✖ Error creating CV.");
        }
    }

    private void handleUpdate() {
        if (selectedCv == null) { msg("⚠ No CV selected."); return; }
        if (!validateForm()) return;

        Cv cv = buildCvFromForm();
        cv.setId(selectedCv.getId());
        cv.setUserId(selectedCv.getUserId());
        if (cvService.updateCv(cv)) {
            msg("✔ CV updated successfully!");
            messageLabel.getStyleClass().remove("message-label");
            messageLabel.getStyleClass().add("success-message");
        } else {
            msg("✖ Error updating CV.");
        }
    }

    private void handleDownload() {
        Cv cvData = (selectedCv != null) ? buildCvFromForm() : null;
        if (cvData == null) { msg("⚠ No CV data to download."); return; }

        // Update ID so it matches the saved CV
        cvData.setId(selectedCv.getId());
        cvData.setUserId(selectedCv.getUserId());

        // Check if user has a profile picture
        User user = AuthService.getCurrentUser();
        String picPath = null;
        if (user != null && user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            File picFile = new File(user.getProfilePicture());
            if (picFile.exists()) {
                // Ask user if they want the picture in the PDF
                Alert ask = new Alert(Alert.AlertType.CONFIRMATION);
                DialogHelper.styleAlert(ask);
                ask.setTitle("Profile Photo");
                ask.setHeaderText("Include profile photo in PDF?");
                ask.setContentText("You have a profile photo set. Would you like to include it in the PDF?");
                ButtonType yesBtn = new ButtonType("Yes, include photo");
                ButtonType noBtn = new ButtonType("No, without photo");
                ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                ask.getButtonTypes().setAll(yesBtn, noBtn, cancelBtn);

                Optional<ButtonType> result = ask.showAndWait();
                if (result.isEmpty() || result.get() == cancelBtn) return;
                if (result.get() == yesBtn) picPath = user.getProfilePicture();
            }
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save CV as PDF");
        fc.setInitialFileName(safe(cvData.getFullName()) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(rootPane.getScene().getWindow());
        if (file != null) {
            if (cvService.exportToPdf(cvData, file.getAbsolutePath(), picPath)) {
                msg("✔ PDF saved successfully!");
            } else {
                msg("✖ Error saving PDF.");
            }
        }
    }

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

        if (name.isEmpty()) { msg("⚠ Full Name is required."); fullNameField.requestFocus(); return false; }
        if (name.length() < 3) { msg("⚠ Full Name must be at least 3 characters."); fullNameField.requestFocus(); return false; }
        if (email.isEmpty()) { msg("⚠ Email is required."); cvEmailField.requestFocus(); return false; }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) { msg("⚠ Please enter a valid email address."); cvEmailField.requestFocus(); return false; }

        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^[+]?[0-9\\s\\-]{6,20}$")) {
            msg("⚠ Please enter a valid phone number."); phoneField.requestFocus(); return false;
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

        StringBuilder edu = new StringBuilder();
        for (var node : educationContainer.getChildren()) {
            if (node instanceof HBox) {
                for (var child : ((HBox) node).getChildren()) {
                    if (child instanceof TextField) {
                        String val = ((TextField) child).getText().trim();
                        if (!val.isEmpty()) { if (edu.length() > 0) edu.append("\n---\n"); edu.append(val); }
                    }
                }
            }
        }
        cv.setEducation(edu.toString());

        StringBuilder exp = new StringBuilder();
        for (var node : experienceContainer.getChildren()) {
            if (node instanceof HBox) {
                for (var child : ((HBox) node).getChildren()) {
                    if (child instanceof TextField) {
                        String val = ((TextField) child).getText().trim();
                        if (!val.isEmpty()) { if (exp.length() > 0) exp.append("\n---\n"); exp.append(val); }
                    }
                }
            }
        }
        cv.setExperience(exp.toString());

        cv.setSkills(String.join(", ", skillsList));

        StringBuilder langs = new StringBuilder();
        for (var node : languagesContainer.getChildren()) {
            if (node instanceof HBox) {
                String lang = "", level = "";
                for (var child : ((HBox) node).getChildren()) {
                    if (child instanceof ComboBox) {
                        @SuppressWarnings("unchecked") ComboBox<String> combo = (ComboBox<String>) child;
                        if (combo.getPromptText() != null && combo.getPromptText().equals("Language")) lang = combo.getValue() != null ? combo.getValue() : "";
                        else if (combo.getPromptText() != null && combo.getPromptText().equals("Level")) level = combo.getValue() != null ? combo.getValue() : "";
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

    private void msg(String t) { if (messageLabel != null) messageLabel.setText(t); }
    private String safe(String s) { return s == null ? "" : s; }

    @FXML private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
