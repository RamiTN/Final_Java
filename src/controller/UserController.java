package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.User;
import service.AuthService;

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

    private AuthService authService = new AuthService();

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
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String pass = passwordField.getText();
        if (email.isEmpty() || pass.isEmpty()) { msg("Please fill all fields."); return; }
        User user = authService.login(email, pass);
        if (user != null) {
            loadScene("/view/dashboard.fxml");
        } else {
            msg("Invalid email or password.");
        }
    }

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = regEmailField.getText().trim();
        String pass = regPasswordField.getText();
        String confirm = confirmPasswordField.getText();
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) { msg("Please fill all fields."); return; }
        if (!pass.equals(confirm)) { msg("Passwords don't match."); return; }
        if (authService.register(name, email, pass)) {
            loadScene("/view/login.fxml");
        } else {
            msg("Email already exists.");
        }
    }

    @FXML private void goToRegister() { loadScene("/view/register.fxml"); }
    @FXML private void goToLogin() { loadScene("/view/login.fxml"); }
    @FXML private void goToCv() { loadScene("/view/cv.fxml"); }
    @FXML private void goToProfile() { loadScene("/view/profile.fxml"); }
    @FXML private void goToJobs() { loadScene("/view/Job.fxml"); }
    @FXML private void goToJobGuide() { loadScene("/view/JobGuide.fxml"); }
    @FXML private void goToAdmin() { loadScene("/view/AdminDashboard.fxml"); }
    @FXML private void goToDashboard() { loadScene("/view/dashboard.fxml"); }

    @FXML
    private void handleLogout() {
        AuthService.logout();
        loadScene("/view/login.fxml");
    }

    private void msg(String text) {
        if (messageLabel != null) messageLabel.setText(text);
    }

    private void loadScene(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
        } catch (Exception e) { e.printStackTrace(); }
    }
}
