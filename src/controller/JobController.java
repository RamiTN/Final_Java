package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import dao.JobOfferDAO;
import model.Job;

public class JobController {

    @FXML private VBox rootPane;
    @FXML private TableView<Job> jobTable;

    private JobOfferDAO jobDAO = new JobOfferDAO();

    @FXML
    public void initialize() {
        jobTable.setItems(FXCollections.observableArrayList(jobDAO.findAll()));
    }

    @FXML private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
