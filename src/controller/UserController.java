package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Cv;
import model.Job;
import model.User;
import service.AuthService;
import dao.CVDAO;
import dao.JobOfferDAO;

import java.util.List;

public class UserController {

    @FXML private VBox rootPane;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField nameField;
    @FXML private TextField regEmailField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label welcomeLabel;
    @FXML private Button adminBtn;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label messageLabel;

    // Dynamic dashboard fields
    @FXML private Label cvCountLabel;
    @FXML private Label jobCountLabel;
    @FXML private Label profilePercentLabel;
    @FXML private VBox recentCvsContainer;
    @FXML private TextField searchField;

    private AuthService authService = new AuthService();
    private CVDAO cvDAO = new CVDAO();
    private JobOfferDAO jobDAO = new JobOfferDAO();

    @FXML
    public void initialize() {
        User user = AuthService.getCurrentUser();
        // dashboard init
        if (welcomeLabel != null && user != null) {
            welcomeLabel.setText("Welcome, " + user.getName() + "!");
        }
        if (adminBtn != null && user != null) {
            adminBtn.setVisible(user.isAdmin());
            adminBtn.setManaged(user.isAdmin());
        }
        // profile init
        if (profileNameLabel != null && user != null) {
            profileNameLabel.setText(user.getName());
            profileEmailLabel.setText(user.getEmail());
            profileRoleLabel.setText(user.getRole().toUpperCase());
        }

        // Dynamic dashboard data
        if (user != null) {
            loadDashboardStats(user);
            loadRecentCvs(user);
        }

        // Search listener
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch(newVal));
        }
    }

    private void loadDashboardStats(User user) {
        // CV count
        if (cvCountLabel != null) {
            List<Cv> userCvs = cvDAO.findByUserId(user.getId());
            cvCountLabel.setText(String.valueOf(userCvs.size()));
        }
        // Job count
        if (jobCountLabel != null) {
            List<Job> jobs = jobDAO.findAll();
            jobCountLabel.setText(String.valueOf(jobs.size()));
        }
        // Profile completion
        if (profilePercentLabel != null) {
            int filled = 0;
            int total = 3;
            if (user.getName() != null && !user.getName().isEmpty()) filled++;
            if (user.getEmail() != null && !user.getEmail().isEmpty()) filled++;
            // Check if user has at least one CV with content
            List<Cv> cvs = cvDAO.findByUserId(user.getId());
            if (!cvs.isEmpty()) filled++;
            int percent = (int) ((filled / (double) total) * 100);
            profilePercentLabel.setText(percent + "%");
        }
    }

    private void loadRecentCvs(User user) {
        if (recentCvsContainer == null) return;
        recentCvsContainer.getChildren().clear();
        recentCvsContainer.setSpacing(10);

        List<Cv> cvs = cvDAO.findByUserId(user.getId());
        if (cvs.isEmpty()) {
            Label empty = new Label("No CVs yet. Create your first CV!");
            empty.getStyleClass().add("subtitle");
            recentCvsContainer.getChildren().add(empty);
            return;
        }

        // Show up to 5 most recent CVs
        int limit = Math.min(cvs.size(), 5);
        for (int i = 0; i < limit; i++) {
            Cv cv = cvs.get(i);
            HBox row = buildCvRow(cv);
            recentCvsContainer.getChildren().add(row);
        }
    }

    private HBox buildCvRow(Cv cv) {
        HBox row = new HBox(10);
        row.getStyleClass().add("list-item");
        row.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("\uD83D\uDCC4");
        icon.getStyleClass().add("stat-icon");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameL = new Label(cv.getFullName() != null ? cv.getFullName() : "CV #" + cv.getId());
        nameL.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        Label skillsL = new Label(cv.getSkills() != null && !cv.getSkills().isEmpty()
                ? cv.getSkills() : "No skills listed");
        skillsL.getStyleClass().add("subtitle");
        info.getChildren().addAll(nameL, skillsL);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("small-btn");
        editBtn.setOnAction(e -> goToCv());

        row.getChildren().addAll(icon, info, editBtn);
        return row;
    }

    private void handleSearch(String query) {
        if (recentCvsContainer == null) return;
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        recentCvsContainer.getChildren().clear();

        if (query == null || query.trim().isEmpty()) {
            loadRecentCvs(user);
            return;
        }

        String q = query.toLowerCase().trim();
        List<Cv> cvs = cvDAO.findByUserId(user.getId());
        boolean found = false;
        for (Cv cv : cvs) {
            String fullName = cv.getFullName() != null ? cv.getFullName().toLowerCase() : "";
            String skills = cv.getSkills() != null ? cv.getSkills().toLowerCase() : "";
            String objective = cv.getObjective() != null ? cv.getObjective().toLowerCase() : "";
            if (fullName.contains(q) || skills.contains(q) || objective.contains(q)) {
                recentCvsContainer.getChildren().add(buildCvRow(cv));
                found = true;
            }
        }

        if (!found) {
            Label noResult = new Label("No CVs match \"" + query.trim() + "\"");
            noResult.getStyleClass().add("subtitle");
            recentCvsContainer.getChildren().add(noResult);
        }
    }

    // ══════════════════════════ LOGIN with validation ══════════════════════════

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String pass = passwordField.getText();

        // Validation
        if (email.isEmpty() || pass.isEmpty()) {
            msg("⚠ Please fill in all fields.");
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            msg("⚠ Please enter a valid email address.");
            return;
        }
        if (pass.length() < 4) {
            msg("⚠ Password must be at least 4 characters.");
            return;
        }

        User user = authService.login(email, pass);
        if (user != null) {
            if (user.isAdmin()) {
                loadScene("/view/AdminDashboard.fxml");
            } else {
                loadScene("/view/dashboard.fxml");
            }
        } else {
            msg("✖ Invalid email or password.");
        }
    }

    // ══════════════════════════ REGISTER with validation ══════════════════════════

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = regEmailField.getText().trim();
        String pass = regPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        // Name validation
        if (name.isEmpty()) {
            msg("⚠ Full Name is required.");
            nameField.requestFocus();
            return;
        }
        if (name.length() < 3) {
            msg("⚠ Full Name must be at least 3 characters.");
            nameField.requestFocus();
            return;
        }
        if (!name.matches("^[A-Za-zÀ-ÿ\\s'-]+$")) {
            msg("⚠ Full Name can only contain letters, spaces, hyphens, and apostrophes.");
            nameField.requestFocus();
            return;
        }

        // Email validation
        if (email.isEmpty()) {
            msg("⚠ Email is required.");
            regEmailField.requestFocus();
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            msg("⚠ Please enter a valid email address.");
            regEmailField.requestFocus();
            return;
        }

        // Password validation
        if (pass.isEmpty()) {
            msg("⚠ Password is required.");
            regPasswordField.requestFocus();
            return;
        }
        if (pass.length() < 6) {
            msg("⚠ Password must be at least 6 characters.");
            regPasswordField.requestFocus();
            return;
        }

        // Confirm password
        if (!pass.equals(confirm)) {
            msg("⚠ Passwords don't match.");
            confirmPasswordField.requestFocus();
            return;
        }

        if (authService.register(name, email, pass)) {
            loadScene("/view/login.fxml");
        } else {
            msg("✖ Email already exists.");
        }
    }

    // ══════════════════════════ NAVIGATION ══════════════════════════

    @FXML private void goToRegister() { loadScene("/view/register.fxml"); }
    @FXML private void goToLogin() { loadScene("/view/login.fxml"); }
    @FXML private void goToCv() { loadScene("/view/cv.fxml"); }
    @FXML private void goToProfile() { loadScene("/view/profile.fxml"); }
    @FXML private void goToJobs() { loadScene("/view/Job.fxml"); }
    @FXML private void goToJobGuide() { loadScene("/view/JobGuide.fxml"); }
    @FXML private void goToAdmin() { loadScene("/view/AdminDashboard.fxml"); }
    @FXML private void goToDashboard() { loadScene("/view/dashboard.fxml"); }
    @FXML private void goToLanding() { loadScene("/view/landing.fxml"); }

    @FXML
    private void handleLogout() {
        AuthService.logout();
        loadScene("/view/landing.fxml");
    }

    private void msg(String text) {
        if (messageLabel != null) messageLabel.setText(text);
    }

    private void loadScene(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
