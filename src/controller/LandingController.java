package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import dao.CVDAO;
import dao.JobOfferDAO;
import dao.UserDAO;
import model.Cv;
import model.Job;
import model.User;

import java.util.List;

public class LandingController {

    @FXML private VBox rootPane;
    @FXML private VBox featuresSection;

    // Dynamic stat labels
    @FXML private Label statUsersLabel;
    @FXML private Label statCvsLabel;
    @FXML private Label statJobsLabel;

    // Dynamic jobs section
    @FXML private VBox recentJobsContainer;
    @FXML private Label jobsSectionSubtitle;
    @FXML private Label ctaSubtitleLabel;

    private UserDAO userDAO = new UserDAO();
    private CVDAO cvDAO = new CVDAO();
    private JobOfferDAO jobDAO = new JobOfferDAO();

    @FXML
    public void initialize() {
        loadStats();
        loadRecentJobs();
    }

    private void loadStats() {
        List<User> users = userDAO.findAll();
        List<Cv> cvs = cvDAO.findAll();
        List<Job> jobs = jobDAO.findAll();

        if (statUsersLabel != null) statUsersLabel.setText(String.valueOf(users.size()));
        if (statCvsLabel != null) statCvsLabel.setText(String.valueOf(cvs.size()));
        if (statJobsLabel != null) statJobsLabel.setText(String.valueOf(jobs.size()));

        if (ctaSubtitleLabel != null) {
            ctaSubtitleLabel.setText("Join " + users.size() + " users already building their careers with CVBuilder.");
        }
    }

    private void loadRecentJobs() {
        if (recentJobsContainer == null) return;
        recentJobsContainer.getChildren().clear();

        List<Job> jobs = jobDAO.findAll();

        if (jobsSectionSubtitle != null) {
            if (jobs.isEmpty()) {
                jobsSectionSubtitle.setText("No jobs posted yet — check back soon");
            } else {
                jobsSectionSubtitle.setText("Showing " + Math.min(jobs.size(), 5) + " of " + jobs.size() + " job offers");
            }
        }

        if (jobs.isEmpty()) {
            Label empty = new Label("No job offers available right now.");
            empty.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 14;");
            recentJobsContainer.getChildren().add(empty);
            return;
        }

        int limit = Math.min(jobs.size(), 5);
        for (int i = 0; i < limit; i++) {
            Job job = jobs.get(i);
            HBox card = buildJobCard(job);
            recentJobsContainer.getChildren().add(card);
        }
    }

    private HBox buildJobCard(Job job) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.04);" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 16 24;" +
            "-fx-border-color: rgba(255,255,255,0.06);" +
            "-fx-border-radius: 12;"
        );

        // Icon
        Label icon = new Label("💼");
        icon.setStyle("-fx-font-size: 24;");

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label titleLabel = new Label(safe(job.getTitle()));
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold;");

        String meta = safe(job.getCompany());
        if (!safe(job.getLocation()).isEmpty()) {
            meta += "  •  " + job.getLocation();
        }
        Label metaLabel = new Label(meta);
        metaLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 13;");

        info.getChildren().addAll(titleLabel, metaLabel);

        // Description preview
        if (job.getDescription() != null && !job.getDescription().isEmpty()) {
            String desc = job.getDescription();
            if (desc.length() > 80) desc = desc.substring(0, 80) + "...";
            Label descLabel = new Label(desc);
            descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.35); -fx-font-size: 12;");
            descLabel.setWrapText(true);
            info.getChildren().add(descLabel);
        }

        card.getChildren().addAll(icon, info);
        return card;
    }

    @FXML
    private void goToLogin() {
        loadScene("/view/login.fxml");
    }

    @FXML
    private void goToRegister() {
        loadScene("/view/register.fxml");
    }

    @FXML
    private void goToFeatures() {
        loadScene("/view/features.fxml");
    }

    @FXML
    private void goToHowItWorks() {
        loadScene("/view/how_it_works.fxml");
    }

    private void loadScene(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
