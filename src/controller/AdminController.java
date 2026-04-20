package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import dao.CVDAO;
import dao.JobOfferDAO;
import dao.UserDAO;
import model.Cv;
import model.Job;
import model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminController {

    @FXML private VBox rootPane;
    @FXML private TableView<User> userTable;
    @FXML private TableView<Job> adminJobTable;
    @FXML private TextField jobTitleField, jobCompanyField, jobLocationField, jobLinkField;
    @FXML private TextField jobDescField, jobReqField;
    @FXML private Label userMsg, jobMsg;

    // Admin dashboard stats
    @FXML private Label totalUsersLabel;
    @FXML private Label totalJobsLabel;
    @FXML private Label totalCvsLabel;
    @FXML private Label adminWelcomeLabel;
    @FXML private PieChart rolesChart;
    @FXML private BarChart<String, Number> jobsChart;
    @FXML private TabPane adminTabPane;

    private UserDAO userDAO = new UserDAO();
    private JobOfferDAO jobDAO = new JobOfferDAO();
    private CVDAO cvDAO = new CVDAO();

    @FXML
    public void initialize() {
        refreshUsers();
        refreshJobs();
        loadAdminStats();
        loadCharts();

        // Welcome message
        User admin = service.AuthService.getCurrentUser();
        if (adminWelcomeLabel != null && admin != null) {
            adminWelcomeLabel.setText("Welcome, " + admin.getName() + "!");
        }
    }

    private void loadAdminStats() {
        List<User> users = userDAO.findAll();
        List<Job> jobs = jobDAO.findAll();
        List<Cv> cvs = cvDAO.findAll();

        if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(users.size()));
        if (totalJobsLabel != null) totalJobsLabel.setText(String.valueOf(jobs.size()));
        if (totalCvsLabel != null) totalCvsLabel.setText(String.valueOf(cvs.size()));
    }

    private void loadCharts() {
        // Pie chart: user roles distribution
        if (rolesChart != null) {
            List<User> users = userDAO.findAll();
            int admins = 0, regulars = 0;
            for (User u : users) {
                if (u.isAdmin()) admins++;
                else regulars++;
            }
            rolesChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Admins (" + admins + ")", admins),
                    new PieChart.Data("Users (" + regulars + ")", regulars)
            ));
            rolesChart.setLabelsVisible(true);
            rolesChart.setLegendVisible(true);
        }

        // Bar chart: jobs per company
        if (jobsChart != null) {
            List<Job> jobs = jobDAO.findAll();
            Map<String, Integer> companyCount = new HashMap<>();
            for (Job j : jobs) {
                String company = j.getCompany() != null && !j.getCompany().isEmpty()
                        ? j.getCompany() : "Unknown";
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

    private void refreshUsers() {
        if (userTable != null) {
            userTable.setItems(FXCollections.observableArrayList(userDAO.findAll()));
        }
    }

    private void refreshJobs() {
        if (adminJobTable != null) {
            adminJobTable.setItems(FXCollections.observableArrayList(jobDAO.findAll()));
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

    @FXML
    private void handleAddJob() {
        String title = jobTitleField.getText().trim();
        String company = jobCompanyField.getText().trim();
        String location = jobLocationField.getText().trim();

        if (title.isEmpty()) { jobMsg.setText("⚠ Title is required."); return; }
        if (title.length() < 3) { jobMsg.setText("⚠ Title must be at least 3 characters."); return; }
        if (company.isEmpty()) { jobMsg.setText("⚠ Company is required."); return; }
        if (location.isEmpty()) { jobMsg.setText("⚠ Location is required."); return; }

        String link = jobLinkField.getText().trim();
        if (!link.isEmpty() && !link.matches("^https?://.*")) {
            jobMsg.setText("⚠ Link must start with http:// or https://");
            return;
        }

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
        j.setLink(jobLinkField.getText().trim());
        return j;
    }

    private void clearJobForm() {
        jobTitleField.setText(""); jobCompanyField.setText("");
        jobLocationField.setText(""); jobLinkField.setText("");
        jobDescField.setText(""); jobReqField.setText("");
    }

    @FXML
    private void handleLogout() {
        service.AuthService.logout();
        loadScene("/view/landing.fxml");
    }

    @FXML private void goToDashboard() {
        loadScene("/view/AdminDashboard.fxml");
    }

    @FXML private void goToUsers() {
        if (adminTabPane != null) adminTabPane.getSelectionModel().select(0);
    }

    @FXML private void goToJobs() {
        if (adminTabPane != null) adminTabPane.getSelectionModel().select(1);
    }

    private void loadScene(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
