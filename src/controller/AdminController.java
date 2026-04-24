package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import dao.CVDAO;
import dao.JobOfferDAO;
import dao.NotificationDAO;
import dao.SupportTicketDAO;
import dao.UserDAO;
import model.Cv;
import model.Job;
import model.Notification;
import model.SupportTicket;
import model.User;
import service.AuthService;
import service.CvService;
import service.DialogHelper;

import java.awt.Desktop;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminController {

    @FXML private VBox rootPane;
    @FXML private TableView<User> userTable;
    @FXML private TableView<Job> adminJobTable;
    @FXML private TextField jobTitleField, jobCompanyField, jobLocationField;
    @FXML private TextField jobDescField, jobReqField;
    @FXML private Label userMsg, jobMsg, appMsg, ticketMsg;

    @FXML private Label totalUsersLabel, totalJobsLabel, totalCvsLabel;
    @FXML private Label totalAppsLabel, totalTicketsLabel;
    @FXML private Label adminWelcomeLabel;
    @FXML private PieChart rolesChart;
    @FXML private BarChart<String, Number> jobsChart;
    @FXML private TabPane adminTabPane;
    @FXML private VBox applicationsContainer;
    @FXML private VBox ticketsContainer;

    private UserDAO userDAO = new UserDAO();
    private JobOfferDAO jobDAO = new JobOfferDAO();
    private CVDAO cvDAO = new CVDAO();
    private NotificationDAO notifDAO = new NotificationDAO();
    private SupportTicketDAO ticketDAO = new SupportTicketDAO();
    private CvService cvService = new CvService();

    @FXML
    public void initialize() {
        refreshUsers();
        refreshJobs();
        loadAdminStats();
        loadCharts();
        loadApplications();
        loadTickets();

        User admin = AuthService.getCurrentUser();
        if (adminWelcomeLabel != null && admin != null) {
            adminWelcomeLabel.setText("Welcome, " + admin.getName() + "!");
        }
    }

    // ══════════════════════════ STATS & CHARTS ══════════════════════════

    private void loadAdminStats() {
        List<User> users = userDAO.findAll();
        List<Job> jobs = jobDAO.findAll();
        List<Cv> cvs = cvDAO.findAll();

        if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(users.size()));
        if (totalJobsLabel != null) totalJobsLabel.setText(String.valueOf(jobs.size()));
        if (totalCvsLabel != null) totalCvsLabel.setText(String.valueOf(cvs.size()));
        if (totalAppsLabel != null) totalAppsLabel.setText(String.valueOf(notifDAO.countPendingApplications()));
        if (totalTicketsLabel != null) totalTicketsLabel.setText(String.valueOf(ticketDAO.countOpen()));
    }

    private void loadCharts() {
        if (rolesChart != null) {
            List<User> users = userDAO.findAll();
            int admins = 0, regulars = 0;
            for (User u : users) { if (u.isAdmin()) admins++; else regulars++; }
            rolesChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Admins (" + admins + ")", admins),
                    new PieChart.Data("Users (" + regulars + ")", regulars)
            ));
            rolesChart.setLabelsVisible(true);
            rolesChart.setLegendVisible(true);
        }

        if (jobsChart != null) {
            List<Job> jobs = jobDAO.findAll();
            Map<String, Integer> companyCount = new HashMap<>();
            for (Job j : jobs) {
                String company = j.getCompany() != null && !j.getCompany().isEmpty() ? j.getCompany() : "Unknown";
                companyCount.put(company, companyCount.getOrDefault(company, 0) + 1);
            }
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Jobs");
            for (Map.Entry<String, Integer> entry : companyCount.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            jobsChart.getData().clear();
            jobsChart.getData().add(series);
        }
    }

    // ══════════════════════════ APPLICATIONS ══════════════════════════

    private void loadApplications() {
        if (applicationsContainer == null) return;
        applicationsContainer.getChildren().clear();

        List<Notification> apps = notifDAO.findApplicationsForAdmin();
        if (apps.isEmpty()) {
            Label empty = new Label("No pending applications.");
            empty.getStyleClass().add("subtitle");
            applicationsContainer.getChildren().add(empty);
            return;
        }

        for (Notification app : apps) {
            VBox card = buildApplicationCard(app);
            applicationsContainer.getChildren().add(card);
        }
    }

    private VBox buildApplicationCard(Notification app) {
        VBox card = new VBox(8);
        card.getStyleClass().add("list-item");
        card.setPadding(new Insets(14));
        card.setStyle("-fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label userLbl = new Label("👤 " + (app.getUserName() != null ? app.getUserName() : "User #" + app.getUserId()));
        userLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #7ca3ff; -fx-font-size: 14;");
        HBox.setHgrow(userLbl, Priority.ALWAYS);

        Label jobLbl = new Label("📋 " + (app.getJobTitle() != null ? app.getJobTitle() : "Job #" + app.getJobId()));
        jobLbl.setStyle("-fx-text-fill: #e0c97f; -fx-font-weight: bold;");

        header.getChildren().addAll(userLbl, jobLbl);

        Label msgLbl = new Label(app.getMessage() != null ? app.getMessage() : "");
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-text-fill: #b0b8c8; -fx-font-size: 12;");

        Label cvLbl = new Label("CV: " + (app.getCvName() != null ? app.getCvName() : "CV #" + app.getCvId()));
        cvLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

        Label dateLbl = new Label(app.getCreatedAt() != null ? "Applied: " + app.getCreatedAt() : "");
        dateLbl.setStyle("-fx-text-fill: #666; -fx-font-size: 10;");

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button viewCvBtn = new Button("👁 View CV (PDF)");
        viewCvBtn.getStyleClass().add("info-btn");
        viewCvBtn.setStyle("-fx-padding: 5 14; -fx-font-size: 11;");
        viewCvBtn.setOnAction(e -> handleViewCvPdf(app.getCvId()));

        Button acceptBtn = new Button("✔ Accept");
        acceptBtn.getStyleClass().add("success-btn");
        acceptBtn.setStyle("-fx-padding: 5 14; -fx-font-size: 11;");
        acceptBtn.setOnAction(e -> handleAccept(app));

        Button rejectBtn = new Button("✖ Reject");
        rejectBtn.getStyleClass().add("danger-btn");
        rejectBtn.setStyle("-fx-padding: 5 14; -fx-font-size: 11;");
        rejectBtn.setOnAction(e -> handleReject(app));

        btnRow.getChildren().addAll(viewCvBtn, acceptBtn, rejectBtn);

        card.getChildren().addAll(header, msgLbl, cvLbl, dateLbl, new Separator(), btnRow);
        return card;
    }

    private void handleViewCvPdf(int cvId) {
        Cv cv = cvDAO.findById(cvId);
        if (cv == null) { if (appMsg != null) appMsg.setText("CV not found."); return; }

        // Find user's profile picture
        User cvOwner = userDAO.findById(cv.getUserId());
        String picPath = null;
        if (cvOwner != null && cvOwner.getProfilePicture() != null) {
            File f = new File(cvOwner.getProfilePicture());
            if (f.exists()) picPath = cvOwner.getProfilePicture();
        }

        String pdfPath = cvService.generatePreviewPdf(cv, picPath);
        if (pdfPath != null) {
            try {
                Desktop.getDesktop().open(new File(pdfPath));
            } catch (Exception ex) {
                ex.printStackTrace();
                if (appMsg != null) appMsg.setText("Could not open PDF viewer.");
            }
        }
    }

    private void handleAccept(Notification app) {
        User admin = AuthService.getCurrentUser();
        if (admin == null) return;

        String jobTitle = app.getJobTitle() != null ? app.getJobTitle() : "the position";
        String message = "Congratulations! Your application for \"" + jobTitle + "\" has been accepted.";

        // Respond: deletes original application, creates response notification
        notifDAO.respond(app.getId(), admin.getId(), "accepted", message,
                app.getUserId(), app.getJobId(), app.getCvId());

        // Set job to unavailable
        jobDAO.updateStatus(app.getJobId(), "unavailable");

        if (appMsg != null) appMsg.setText("✔ Application accepted. Job marked as unavailable.");
        loadApplications();
        loadAdminStats();
        refreshJobs();
    }

    private void handleReject(Notification app) {
        User admin = AuthService.getCurrentUser();
        if (admin == null) return;

        String jobTitle = app.getJobTitle() != null ? app.getJobTitle() : "the position";
        String message = "We regret to inform you that your application for \"" + jobTitle + "\" was not selected.";

        // Respond: deletes original application, creates response notification
        notifDAO.respond(app.getId(), admin.getId(), "rejected", message,
                app.getUserId(), app.getJobId(), app.getCvId());

        // Also remove from users_applied
        jobDAO.removeAppliedUser(app.getJobId(), app.getUserId());

        if (appMsg != null) appMsg.setText("Application rejected. User notified.");
        loadApplications();
        loadAdminStats();
    }

    @FXML
    private void refreshApplications() {
        loadApplications();
        loadAdminStats();
        if (appMsg != null) appMsg.setText("Refreshed.");
    }

    // ══════════════════════════ SUPPORT TICKETS ══════════════════════════

    private void loadTickets() {
        if (ticketsContainer == null) return;
        ticketsContainer.getChildren().clear();

        List<SupportTicket> tickets = ticketDAO.findAll();
        if (tickets.isEmpty()) {
            Label empty = new Label("No support tickets.");
            empty.getStyleClass().add("subtitle");
            ticketsContainer.getChildren().add(empty);
            return;
        }

        for (SupportTicket t : tickets) {
            VBox card = buildTicketCard(t);
            ticketsContainer.getChildren().add(card);
        }
    }

    private VBox buildTicketCard(SupportTicket t) {
        VBox card = new VBox(8);
        card.getStyleClass().add("list-item");
        card.setPadding(new Insets(14));
        String borderColor = t.isOpen() ? "rgba(74,124,255,0.3)" : "rgba(255,255,255,0.06)";
        card.setStyle("-fx-border-color: " + borderColor + "; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label statusBadge = new Label(t.isOpen() ? "● Open" : "● Closed");
        statusBadge.setStyle(t.isOpen()
                ? "-fx-text-fill: #74b9ff; -fx-font-weight: bold; -fx-font-size: 11;"
                : "-fx-text-fill: #888; -fx-font-weight: bold; -fx-font-size: 11;");

        Label userLbl = new Label("👤 " + (t.getUserName() != null ? t.getUserName() : "User #" + t.getUserId()));
        userLbl.setStyle("-fx-text-fill: #7ca3ff; -fx-font-weight: bold;");
        HBox.setHgrow(userLbl, Priority.ALWAYS);

        Label dateLbl = new Label(t.getCreatedAt() != null ? t.getCreatedAt() : "");
        dateLbl.setStyle("-fx-text-fill: #666; -fx-font-size: 10;");

        header.getChildren().addAll(statusBadge, userLbl, dateLbl);

        Label subjectLbl = new Label("Subject: " + t.getSubject());
        subjectLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");

        Label msgLbl = new Label(t.getMessage());
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-text-fill: #b0b8c8; -fx-font-size: 12;");

        card.getChildren().addAll(header, subjectLbl, msgLbl);

        if (t.getAdminResponse() != null && !t.getAdminResponse().isEmpty()) {
            Label respLbl = new Label("Your response: " + t.getAdminResponse());
            respLbl.setWrapText(true);
            respLbl.setStyle("-fx-text-fill: #5ae596; -fx-font-size: 12; -fx-font-style: italic;");
            card.getChildren().add(respLbl);
        }

        if (t.isOpen()) {
            HBox btnRow = new HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);
            Button respondBtn = new Button("💬 Respond");
            respondBtn.getStyleClass().add("info-btn");
            respondBtn.setStyle("-fx-padding: 5 14; -fx-font-size: 11;");
            respondBtn.setOnAction(e -> handleRespondTicket(t));
            btnRow.getChildren().add(respondBtn);
            card.getChildren().addAll(new Separator(), btnRow);
        }

        return card;
    }

    private void handleRespondTicket(SupportTicket t) {
        TextInputDialog dialog = new TextInputDialog();
        DialogHelper.styleDialog(dialog);
        dialog.setTitle("Respond to Support Ticket");
        dialog.setHeaderText("Ticket: " + t.getSubject() + "\nFrom: " + (t.getUserName() != null ? t.getUserName() : "User"));
        dialog.setContentText("Your response:");
        dialog.getEditor().setPrefWidth(400);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            ticketDAO.respond(t.getId(), result.get().trim());
            if (ticketMsg != null) ticketMsg.setText("✔ Response sent.");
            loadTickets();
            loadAdminStats();
        }
    }

    @FXML
    private void refreshTickets() {
        loadTickets();
        loadAdminStats();
        if (ticketMsg != null) ticketMsg.setText("Refreshed.");
    }

    // ══════════════════════════ USERS ══════════════════════════

    private void refreshUsers() {
        if (userTable != null) {
            userTable.setItems(FXCollections.observableArrayList(userDAO.findAll()));
        }
    }

    @FXML
    private void handleDeleteUser() {
        User u = userTable.getSelectionModel().getSelectedItem();
        if (u == null) { userMsg.setText("Select a user."); return; }
        if (u.isAdmin()) { userMsg.setText("Cannot delete admin."); return; }
        userDAO.delete(u.getId());
        userMsg.setText("User deleted.");
        refreshUsers();
        loadAdminStats();
        loadCharts();
    }

    // ══════════════════════════ JOBS ══════════════════════════

    private void refreshJobs() {
        if (adminJobTable != null) {
            adminJobTable.setItems(FXCollections.observableArrayList(jobDAO.findAll()));
        }
    }

    @FXML
    private void handleAddJob() {
        String title = jobTitleField.getText().trim();
        String company = jobCompanyField.getText().trim();
        String location = jobLocationField.getText().trim();

        if (title.isEmpty()) { jobMsg.setText("⚠ Title is required."); return; }
        if (title.length() < 3) { jobMsg.setText("⚠ Title must be at least 3 characters."); return; }
        if (company.isEmpty()) { jobMsg.setText("⚠ Company is required."); return; }
        if (location.isEmpty()) { jobMsg.setText("⚠ Location is required."); return; }

        Job j = buildJob();
        jobDAO.insert(j);
        jobMsg.setText("✔ Job added.");
        clearJobForm();
        refreshJobs();
        loadAdminStats();
        loadCharts();
    }

    @FXML
    private void handleUpdateJob() {
        Job sel = adminJobTable.getSelectionModel().getSelectedItem();
        if (sel == null) { jobMsg.setText("Select a job."); return; }
        Job j = buildJob();
        j.setId(sel.getId());
        j.setStatus(sel.getStatus());
        j.setUsersApplied(sel.getUsersApplied());
        jobDAO.update(j);
        jobMsg.setText("Job updated.");
        clearJobForm();
        refreshJobs();
        loadAdminStats();
        loadCharts();
    }

    @FXML
    private void handleDeleteJob() {
        Job sel = adminJobTable.getSelectionModel().getSelectedItem();
        if (sel == null) { jobMsg.setText("Select a job."); return; }
        jobDAO.delete(sel.getId());
        jobMsg.setText("Job deleted.");
        refreshJobs();
        loadAdminStats();
        loadCharts();
    }

    private Job buildJob() {
        Job j = new Job();
        j.setTitle(jobTitleField.getText().trim());
        j.setCompany(jobCompanyField.getText().trim());
        j.setLocation(jobLocationField.getText().trim());
        j.setDescription(jobDescField.getText().trim());
        j.setRequirements(jobReqField.getText().trim());
        j.setStatus("available");
        return j;
    }

    private void clearJobForm() {
        jobTitleField.setText(""); jobCompanyField.setText("");
        jobLocationField.setText("");
        jobDescField.setText(""); jobReqField.setText("");
    }

    // ══════════════════════════ NAVIGATION ══════════════════════════

    @FXML private void handleLogout() { AuthService.logout(); loadScene("/view/landing.fxml"); }
    @FXML private void goToDashboard() { loadScene("/view/AdminDashboard.fxml"); }
    @FXML private void goToUsers() { if (adminTabPane != null) adminTabPane.getSelectionModel().select(0); }
    @FXML private void goToJobs() { if (adminTabPane != null) adminTabPane.getSelectionModel().select(1); }
    @FXML private void goToApplications() { if (adminTabPane != null) adminTabPane.getSelectionModel().select(2); }
    @FXML private void goToSupport() { if (adminTabPane != null) adminTabPane.getSelectionModel().select(3); }

    private String safe(String s) { return s == null ? "" : s; }

    private void loadScene(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
