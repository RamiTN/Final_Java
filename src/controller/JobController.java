package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.Cv;
import model.Job;
import model.Notification;
import model.User;
import service.AuthService;
import service.CvService;
import dao.CVDAO;
import dao.JobOfferDAO;
import dao.NotificationDAO;
import service.DialogHelper;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

public class JobController {

    @FXML private VBox rootPane;
    @FXML private VBox jobCardsContainer;
    @FXML private ComboBox<Cv> cvCombo;
    @FXML private ComboBox<String> filterCombo;

    private JobOfferDAO jobDAO = new JobOfferDAO();
    private CVDAO cvDAO = new CVDAO();
    private NotificationDAO notifDAO = new NotificationDAO();
    private CvService cvService = new CvService();

    /** Static field to allow JobGuide to open a job preview */
    public static int previewJobId = -1;

    @FXML
    public void initialize() {
        loadCvCombo();
        setupFilterCombo();
        loadJobCards();

        // If JobGuide requested a preview
        if (previewJobId > 0) {
            Job job = jobDAO.findById(previewJobId);
            previewJobId = -1;
            if (job != null) {
                javafx.application.Platform.runLater(() -> showJobPreview(job));
            }
        }
    }

    private void loadCvCombo() {
        User user = AuthService.getCurrentUser();
        if (user == null || cvCombo == null) return;
        List<Cv> cvs = cvDAO.findByUserId(user.getId());
        cvCombo.setItems(FXCollections.observableArrayList(cvs));
    }

    private void setupFilterCombo() {
        if (filterCombo == null) return;
        filterCombo.setItems(FXCollections.observableArrayList("All", "Available", "Unavailable"));
        filterCombo.setValue("All");
    }

    @FXML
    private void handleFilter() {
        loadJobCards();
    }

    private void loadJobCards() {
        if (jobCardsContainer == null) return;
        jobCardsContainer.getChildren().clear();

        String filter = filterCombo != null && filterCombo.getValue() != null ? filterCombo.getValue() : "All";

        List<Job> jobs;
        switch (filter) {
            case "Available": jobs = jobDAO.findByStatus("available"); break;
            case "Unavailable": jobs = jobDAO.findByStatus("unavailable"); break;
            default: jobs = jobDAO.findAll(); break;
        }

        if (jobs.isEmpty()) {
            Label empty = new Label("No job offers match the current filter.");
            empty.getStyleClass().add("subtitle");
            jobCardsContainer.getChildren().add(empty);
            return;
        }

        User user = AuthService.getCurrentUser();
        for (Job job : jobs) {
            VBox card = buildJobCard(job, user);
            jobCardsContainer.getChildren().add(card);
        }
    }

    private VBox buildJobCard(Job job, User user) {
        VBox card = new VBox(10);
        card.getStyleClass().add("panel");
        card.setPadding(new Insets(18));
        card.setStyle("-fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 12; -fx-background-radius: 12;");

        // Header row
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(job.getTitle() != null ? job.getTitle() : "Untitled");
        titleLbl.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: #7ca3ff;");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label companyLbl = new Label(job.getCompany() != null ? job.getCompany() : "");
        companyLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #e0c97f; -fx-font-weight: bold;");

        // Status badge
        Label statusBadge = new Label(job.isAvailable() ? "● Available" : "● Unavailable");
        statusBadge.setStyle(job.isAvailable()
                ? "-fx-text-fill: #5ae596; -fx-font-size: 11; -fx-font-weight: bold;"
                : "-fx-text-fill: #ff6b6b; -fx-font-size: 11; -fx-font-weight: bold;");

        header.getChildren().addAll(titleLbl, statusBadge, companyLbl);

        // Location
        Label locationLbl = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Not specified"));
        locationLbl.getStyleClass().add("subtitle");

        // Description preview
        String desc = job.getDescription() != null ? job.getDescription() : "No description.";
        String preview = desc.length() > 120 ? desc.substring(0, 120) + "..." : desc;
        Label descLbl = new Label(preview);
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-text-fill: #b0b8c8; -fx-font-size: 12;");

        // Button row
        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button consultBtn = new Button("📋 Consult");
        consultBtn.getStyleClass().add("info-btn");
        consultBtn.setOnAction(e -> showJobPreview(job));

        // Check if user has pending application
        boolean hasApplied = user != null && notifDAO.hasApplied(user.getId(), job.getId());

        if (hasApplied) {
            Button cancelBtn = new Button("↩ Cancel Application");
            cancelBtn.getStyleClass().add("warning-btn");
            cancelBtn.setOnAction(e -> handleCancelApply(job));
            btnRow.getChildren().addAll(consultBtn, cancelBtn);
        } else if (job.isAvailable()) {
            Button applyBtn = new Button("📤 Apply");
            applyBtn.getStyleClass().add("success-btn");
            applyBtn.setOnAction(e -> handleApply(job));
            btnRow.getChildren().addAll(consultBtn, applyBtn);
        } else {
            btnRow.getChildren().add(consultBtn);
        }

        card.getChildren().addAll(header, locationLbl, new Separator(), descLbl, btnRow);
        return card;
    }

    /** Styled job preview dialog matching app design, with Apply button */
    private void showJobPreview(Job job) {
        User user = AuthService.getCurrentUser();

        Dialog<ButtonType> dialog = new Dialog<>();
        DialogHelper.styleDialog(dialog);
        dialog.setTitle("Job Details");
        dialog.setHeaderText(null);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setPrefWidth(520);
        content.setStyle("-fx-background-color: #0e1225;");

        // Title + Company
        Label titleLbl = new Label(job.getTitle());
        titleLbl.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #7ca3ff;");

        HBox companyRow = new HBox(12);
        companyRow.setAlignment(Pos.CENTER_LEFT);
        Label compLbl = new Label("🏢 " + (job.getCompany() != null ? job.getCompany() : "N/A"));
        compLbl.setStyle("-fx-text-fill: #e0c97f; -fx-font-size: 14; -fx-font-weight: bold;");
        Label locLbl = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "N/A"));
        locLbl.setStyle("-fx-text-fill: #b0b8c8; -fx-font-size: 13;");
        companyRow.getChildren().addAll(compLbl, locLbl);

        // Status
        Label statusLbl = new Label(job.isAvailable() ? "● Available" : "● Unavailable");
        statusLbl.setStyle(job.isAvailable()
                ? "-fx-text-fill: #5ae596; -fx-font-size: 13; -fx-font-weight: bold;"
                : "-fx-text-fill: #ff6b6b; -fx-font-size: 13; -fx-font-weight: bold;");

        Separator sep1 = new Separator();

        // Description
        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        Label descLbl = new Label(job.getDescription() != null ? job.getDescription() : "N/A");
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-text-fill: #b0b8c8; -fx-font-size: 12;");

        // Requirements
        Label reqTitle = new Label("Requirements");
        reqTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        Label reqLbl = new Label(job.getRequirements() != null ? job.getRequirements() : "N/A");
        reqLbl.setWrapText(true);
        reqLbl.setStyle("-fx-text-fill: #b0b8c8; -fx-font-size: 12;");

        Separator sep2 = new Separator();

        content.getChildren().addAll(titleLbl, companyRow, statusLbl, sep1,
                descTitle, descLbl, reqTitle, reqLbl, sep2);

        // Apply / Cancel button inside preview
        boolean hasApplied = user != null && notifDAO.hasApplied(user.getId(), job.getId());
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        if (hasApplied) {
            Button cancelBtn = new Button("↩ Cancel Application");
            cancelBtn.setStyle("-fx-background-color: rgba(240,173,78,0.2); -fx-text-fill: #ffc96b; "
                    + "-fx-padding: 8 20; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
            cancelBtn.setOnAction(e -> {
                handleCancelApply(job);
                dialog.close();
            });
            actionRow.getChildren().add(cancelBtn);
        } else if (job.isAvailable()) {
            Button applyBtn = new Button("📤 Apply for this Job");
            applyBtn.setStyle("-fx-background-color: rgba(39,174,96,0.2); -fx-text-fill: #5ae596; "
                    + "-fx-padding: 8 20; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
            applyBtn.setOnAction(e -> {
                handleApply(job);
                dialog.close();
            });
            actionRow.getChildren().add(applyBtn);
        }
        content.getChildren().add(actionRow);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #0e1225;");
        dialog.showAndWait();
    }

    private void handleApply(Job job) {
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        Cv selectedCv = cvCombo.getValue();
        if (selectedCv == null) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            DialogHelper.styleAlert(warn);
            warn.setTitle("Select a CV");
            warn.setHeaderText("Please select a CV first");
            warn.setContentText("Use the 'Apply with' dropdown at the top to choose which CV to send.");
            warn.showAndWait();
            return;
        }

        // Check if already applied
        if (notifDAO.hasApplied(user.getId(), job.getId())) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            DialogHelper.styleAlert(info);
            info.setTitle("Already Applied");
            info.setContentText("You have already applied for this job.");
            info.showAndWait();
            return;
        }

        // Create application notification
        Notification notif = new Notification();
        notif.setUserId(user.getId());
        notif.setJobId(job.getId());
        notif.setCvId(selectedCv.getId());
        notif.setType("application");
        notif.setMessage(user.getName() + " applied for \"" + job.getTitle() + "\" with CV: " + selectedCv.getFullName());

        if (notifDAO.insert(notif)) {
            // Track user in job's users_applied
            jobDAO.addAppliedUser(job.getId(), user.getId());

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            DialogHelper.styleAlert(success);
            success.setTitle("Application Sent!");
            success.setHeaderText("✔ Application submitted successfully");
            success.setContentText("Your application for \"" + job.getTitle() + "\" has been sent.\n\n"
                    + "CV used: " + selectedCv.getFullName()
                    + "\n\nYou will be notified when the admin responds.");
            success.showAndWait();
            loadJobCards();
        } else {
            Alert error = new Alert(Alert.AlertType.ERROR);
            DialogHelper.styleAlert(error);
            error.setTitle("Error");
            error.setContentText("Failed to submit application. Please try again.");
            error.showAndWait();
        }
    }

    private void handleCancelApply(Job job) {
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        DialogHelper.styleAlert(confirm);
        confirm.setTitle("Cancel Application");
        confirm.setHeaderText("Cancel your application?");
        confirm.setContentText("Are you sure you want to cancel your application for \"" + job.getTitle() + "\"?");

        java.util.Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            notifDAO.deleteByUserAndJob(user.getId(), job.getId());
            jobDAO.removeAppliedUser(job.getId(), user.getId());

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            DialogHelper.styleAlert(info);
            info.setTitle("Application Cancelled");
            info.setContentText("Your application for \"" + job.getTitle() + "\" has been cancelled.");
            info.showAndWait();
            loadJobCards();
        }
    }

    @FXML
    private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
