package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Cv;
import model.User;
import service.AiService;
import service.AuthService;
import service.CvService;

import java.io.File;
import java.util.List;

public class CVController {

    @FXML private VBox rootPane;
    @FXML private ComboBox<Cv> cvCombo;
    @FXML private TextField fullNameField, cvEmailField, phoneField, addressField;
    @FXML private TextArea objectiveField, educationField, experienceField, skillsField, languagesField;
    @FXML private TextArea aiAdviceArea;
    @FXML private Label messageLabel;

    private CvService cvService = new CvService();
    private Cv selectedCv = null;

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
            fullNameField.setText(selectedCv.getFullName());
            cvEmailField.setText(selectedCv.getEmail());
            phoneField.setText(selectedCv.getPhone());
            addressField.setText(selectedCv.getAddress());
            objectiveField.setText(selectedCv.getObjective());
            educationField.setText(selectedCv.getEducation());
            experienceField.setText(selectedCv.getExperience());
            skillsField.setText(selectedCv.getSkills());
            languagesField.setText(selectedCv.getLanguages());
        }
    }

    @FXML
    private void handleCreate() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;
        Cv cv = buildCvFromForm();
        cv.setUserId(user.getId());
        if (cvService.createCv(cv)) {
            msg("CV created!");
            loadCvList();
        } else {
            msg("Error creating CV.");
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedCv == null) { msg("Select a CV first."); return; }
        Cv cv = buildCvFromForm();
        cv.setId(selectedCv.getId());
        cv.setUserId(selectedCv.getUserId());
        if (cvService.updateCv(cv)) {
            msg("CV updated!");
            loadCvList();
        } else {
            msg("Error updating CV.");
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedCv == null) { msg("Select a CV first."); return; }
        if (cvService.deleteCv(selectedCv.getId())) {
            msg("CV deleted!");
            loadCvList();
        } else {
            msg("Error deleting CV.");
        }
    }

    @FXML
    private void handleDownload() {
        if (selectedCv == null) { msg("Select a CV first."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save CV as PDF");
        fc.setInitialFileName(selectedCv.getFullName() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(rootPane.getScene().getWindow());
        if (file != null) {
            if (cvService.exportToPdf(selectedCv, file.getAbsolutePath())) {
                msg("PDF saved successfully!");
            } else {
                msg("Error saving PDF.");
            }
        }
    }

    @FXML
    private void handleAiAdvice() {
        String prompt = "You are a CV expert. Review this CV data and give short, practical advice to improve it.\n"
                + "Full Name: " + fullNameField.getText() + "\n"
                + "Email: " + cvEmailField.getText() + "\n"
                + "Objective: " + objectiveField.getText() + "\n"
                + "Education: " + educationField.getText() + "\n"
                + "Experience: " + experienceField.getText() + "\n"
                + "Skills: " + skillsField.getText() + "\n"
                + "Languages: " + languagesField.getText() + "\n"
                + "Give specific suggestions to improve each section. Be concise.";
        aiAdviceArea.setText("Asking AI...");
        new Thread(() -> {
            String response = AiService.askGemini(prompt);
            javafx.application.Platform.runLater(() -> aiAdviceArea.setText(response));
        }).start();
    }

    private Cv buildCvFromForm() {
        Cv cv = new Cv();
        cv.setFullName(fullNameField.getText());
        cv.setEmail(cvEmailField.getText());
        cv.setPhone(phoneField.getText());
        cv.setAddress(addressField.getText());
        cv.setObjective(objectiveField.getText());
        cv.setEducation(educationField.getText());
        cv.setExperience(experienceField.getText());
        cv.setSkills(skillsField.getText());
        cv.setLanguages(languagesField.getText());
        return cv;
    }

    private void clearForm() {
        fullNameField.setText(""); cvEmailField.setText(""); phoneField.setText("");
        addressField.setText(""); objectiveField.setText(""); educationField.setText("");
        experienceField.setText(""); skillsField.setText(""); languagesField.setText("");
        aiAdviceArea.setText("");
    }

    private void msg(String t) { if (messageLabel != null) messageLabel.setText(t); }

    @FXML private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
        } catch (Exception e) { e.printStackTrace(); }
    }
}
