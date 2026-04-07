package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import dao.JobOfferDAO;
import dao.UserDAO;
import model.Job;
import model.User;

public class AdminController {

    @FXML private VBox rootPane;
    @FXML private TableView<User> userTable;
    @FXML private TableView<Job> adminJobTable;
    @FXML private TextField jobTitleField, jobCompanyField, jobLocationField, jobLinkField;
    @FXML private TextField jobDescField, jobReqField;
    @FXML private Label userMsg, jobMsg;

    private UserDAO userDAO = new UserDAO();
    private JobOfferDAO jobDAO = new JobOfferDAO();

    @FXML
    public void initialize() {
        refreshUsers();
        refreshJobs();
    }

    private void refreshUsers() {
        userTable.setItems(FXCollections.observableArrayList(userDAO.findAll()));
    }

    private void refreshJobs() {
        adminJobTable.setItems(FXCollections.observableArrayList(jobDAO.findAll()));
    }

    @FXML
    private void handleDeleteUser() {
        User u = userTable.getSelectionModel().getSelectedItem();
        if (u == null) { userMsg.setText("Select a user."); return; }
        if (u.isAdmin()) { userMsg.setText("Cannot delete admin."); return; }
        userDAO.delete(u.getId());
        userMsg.setText("User deleted.");
        refreshUsers();
    }

    @FXML
    private void handleAddJob() {
        if (jobTitleField.getText().trim().isEmpty()) { jobMsg.setText("Title required."); return; }
        Job j = buildJob();
        jobDAO.insert(j);
        jobMsg.setText("Job added.");
        clearJobForm();
        refreshJobs();
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
    }

    @FXML
    private void handleDeleteJob() {
        Job sel = adminJobTable.getSelectionModel().getSelectedItem();
        if (sel == null) { jobMsg.setText("Select a job."); return; }
        jobDAO.delete(sel.getId());
        jobMsg.setText("Job deleted.");
        refreshJobs();
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

    @FXML private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
        } catch (Exception e) { e.printStackTrace(); }
    }
}
