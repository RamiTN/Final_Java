package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Cv;
import model.Job;
import model.Notification;
import model.SupportTicket;
import model.User;
import service.AuthService;
import service.CvService;
import dao.CVDAO;
import dao.JobOfferDAO;
import dao.NotificationDAO;
import dao.SupportTicketDAO;
import dao.UserDAO;
import service.DialogHelper;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

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
    @FXML private Button notifBtn;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label profileBioLabel;
    @FXML private Label messageLabel;
    @FXML private ImageView profileImageView;

    // Profile edit fields
    @FXML private VBox editFieldsBox;
    @FXML private TextField editNameField;
    @FXML private TextField editEmailField;
    @FXML private TextArea editBioField;

    // Dynamic dashboard fields
    @FXML private Label cvCountLabel;
    @FXML private Label jobCountLabel;
    @FXML private Label profilePercentLabel;
    @FXML private VBox recentCvsContainer;
    @FXML private TextField searchField;

    private AuthService authService = new AuthService();
    private CvService cvService = new CvService();
    private CVDAO cvDAO = new CVDAO();
    private JobOfferDAO jobDAO = new JobOfferDAO();
    private UserDAO userDAO = new UserDAO();
    private NotificationDAO notifDAO = new NotificationDAO();
    private SupportTicketDAO ticketDAO = new SupportTicketDAO();

    /** Static field used to pass CV id from dashboard to CVController for editing */
    public static int editCvId = -1;

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
            if (profileBioLabel != null) {
                profileBioLabel.setText(user.getBio() != null && !user.getBio().isEmpty()
                        ? user.getBio() : "No bio yet.");
            }
            loadProfilePicture(user);
        }

        // Dynamic dashboard data
        if (user != null) {
            loadDashboardStats(user);
            loadRecentCvs(user);
            loadNotificationBadge(user);
        }

        // Search listener
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch(newVal));
        }

        // Hide edit fields by default
        if (editFieldsBox != null) {
            editFieldsBox.setVisible(false);
            editFieldsBox.setManaged(false);
        }
    }

    // ══════════════════════════ DASHBOARD ══════════════════════════

    private void loadDashboardStats(User user) {
        if (cvCountLabel != null) {
            List<Cv> userCvs = cvDAO.findByUserId(user.getId());
            cvCountLabel.setText(String.valueOf(userCvs.size()));
        }
        if (jobCountLabel != null) {
            List<Job> jobs = jobDAO.findAvailable();
            jobCountLabel.setText(String.valueOf(jobs.size()));
        }
        if (profilePercentLabel != null) {
            int filled = 0;
            int total = 4;
            if (user.getName() != null && !user.getName().isEmpty()) filled++;
            if (user.getEmail() != null && !user.getEmail().isEmpty()) filled++;
            if (user.getBio() != null && !user.getBio().isEmpty()) filled++;
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

        for (Cv cv : cvs) {
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

        // Preview button — opens PDF
        Button previewBtn = new Button("👁 Preview");
        previewBtn.getStyleClass().add("info-btn");
        previewBtn.setStyle("-fx-padding: 4 12; -fx-font-size: 11;");
        previewBtn.setOnAction(e -> handlePreviewCv(cv));

        // Edit button
        Button editBtn = new Button("✏ Edit");
        editBtn.getStyleClass().add("small-btn");
        editBtn.setStyle("-fx-padding: 4 12; -fx-font-size: 11;");
        editBtn.setOnAction(e -> handleEditCv(cv));

        // Delete button
        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().add("danger-btn");
        deleteBtn.setStyle("-fx-padding: 4 12; -fx-font-size: 11;");
        deleteBtn.setOnAction(e -> handleDeleteCv(cv));

        row.getChildren().addAll(icon, info, previewBtn, editBtn, deleteBtn);
        return row;
    }

    private void handlePreviewCv(Cv cv) {
        User user = AuthService.getCurrentUser();
        String picPath = null;
        if (user != null && user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            File f = new File(user.getProfilePicture());
            if (f.exists()) picPath = user.getProfilePicture();
        }

        String pdfPath = cvService.generatePreviewPdf(cv, picPath);
        if (pdfPath != null) {
            try {
                Desktop.getDesktop().open(new File(pdfPath));
            } catch (IOException ex) {
                ex.printStackTrace();
                // Fallback: show text preview
                showTextPreview(cv);
            }
        } else {
            showTextPreview(cv);
        }
    }

    private void showTextPreview(Cv cv) {
        Alert preview = new Alert(Alert.AlertType.INFORMATION);
        DialogHelper.styleAlert(preview);
        preview.setTitle("CV Preview");
        preview.setHeaderText(cv.getFullName() != null ? cv.getFullName() : "CV #" + cv.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("Email: ").append(safe(cv.getEmail())).append("\n");
        sb.append("Phone: ").append(safe(cv.getPhone())).append("\n");
        sb.append("Address: ").append(safe(cv.getAddress())).append("\n\n");
        sb.append("── Objective ──\n").append(safe(cv.getObjective())).append("\n\n");
        sb.append("── Education ──\n").append(safe(cv.getEducation())).append("\n\n");
        sb.append("── Experience ──\n").append(safe(cv.getExperience())).append("\n\n");
        sb.append("── Skills ──\n").append(safe(cv.getSkills())).append("\n\n");
        sb.append("── Languages ──\n").append(safe(cv.getLanguages()));

        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(18);
        ta.setPrefColumnCount(50);
        preview.getDialogPane().setContent(ta);
        preview.getDialogPane().setPrefWidth(550);
        preview.showAndWait();
    }

    private void handleEditCv(Cv cv) {
        editCvId = cv.getId();
        loadScene("/view/cv.fxml");
    }

    private void handleDeleteCv(Cv cv) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        DialogHelper.styleAlert(confirm);
        confirm.setTitle("Delete CV");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will permanently delete the CV \"" +
                (cv.getFullName() != null ? cv.getFullName() : "CV #" + cv.getId()) + "\". This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (cvService.deleteCv(cv.getId())) {
                User user = AuthService.getCurrentUser();
                if (user != null) {
                    loadDashboardStats(user);
                    loadRecentCvs(user);
                }
            }
        }
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

    // ══════════════════════════ NOTIFICATIONS ══════════════════════════

    private void loadNotificationBadge(User user) {
        if (notifBtn == null) return;
        int notifCount = notifDAO.countUnreadForUser(user.getId());
        notifBtn.setText(notifCount > 0 ? "🔔 " + notifCount : "🔔");
    }

    @FXML
    private void handleShowNotifications() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        List<Notification> notifs = notifDAO.findByUserId(user.getId());
        List<SupportTicket> tickets = ticketDAO.findByUserId(user.getId());
        
        boolean hasTicketResponses = false;
        for (SupportTicket t : tickets) {
            if (t.getAdminResponse() != null && !t.getAdminResponse().isEmpty()) {
                hasTicketResponses = true;
                break;
            }
        }

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        DialogHelper.styleAlert(dialog);
        dialog.setTitle("Notifications");
        dialog.setHeaderText("Your Notifications & Responses");

        if (notifs.isEmpty() && !hasTicketResponses) {
            dialog.setContentText("No notifications yet.");
            dialog.showAndWait();
            return;
        }

        VBox content = new VBox(8);
        content.setPadding(new Insets(10));
        for (Notification n : notifs) {
            String icon = "accepted".equals(n.getType()) ? "✅" : "❌";
            String text = icon + " " + (n.getJobTitle() != null ? n.getJobTitle() : "Job") +
                    " — " + (n.getMessage() != null ? n.getMessage() : n.getType());
            Label lbl = new Label(text);
            lbl.setWrapText(true);
            lbl.setStyle(n.isRead() ? "-fx-text-fill: #888;" : "-fx-text-fill: white; -fx-font-weight: bold;");
            content.getChildren().add(lbl);
            if (!n.isRead()) notifDAO.markAsRead(n.getId());
        }

        // Also show support ticket responses
        for (SupportTicket t : tickets) {
            if (t.getAdminResponse() != null && !t.getAdminResponse().isEmpty()) {
                Label lbl = new Label("💬 Support: \"" + t.getSubject() + "\" — " + t.getAdminResponse());
                lbl.setWrapText(true);
                lbl.setStyle("-fx-text-fill: #74b9ff;");
                content.getChildren().add(lbl);
            }
        }

        ScrollPane sp = new ScrollPane(content);
        sp.setPrefHeight(300);
        sp.setPrefWidth(450);
        sp.setFitToWidth(true);
        dialog.getDialogPane().setContent(sp);
        dialog.showAndWait();

        loadNotificationBadge(user);
    }

    // ══════════════════════════ PROFILE ══════════════════════════

    private void loadProfilePicture(User user) {
        if (profileImageView == null) return;
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            File picFile = new File(user.getProfilePicture());
            if (picFile.exists()) {
                profileImageView.setImage(new Image(picFile.toURI().toString()));
                return;
            }
        }
        profileImageView.setImage(null);
    }

    @FXML
    private void handleUploadPicture() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Profile Picture");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        try {
            Path dir = Path.of("profile_pictures");
            Files.createDirectories(dir);
            String ext = file.getName().substring(file.getName().lastIndexOf('.'));
            Path dest = dir.resolve("user_" + user.getId() + ext);
            Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            user.setProfilePicture(dest.toString());
            userDAO.updateProfilePicture(user.getId(), dest.toString());
            AuthService.setCurrentUser(user);

            loadProfilePicture(user);
            msg("✔ Profile picture updated!");
        } catch (IOException e) {
            e.printStackTrace();
            msg("✖ Error uploading picture.");
        }
    }

    @FXML
    private void handleToggleEdit() {
        User user = AuthService.getCurrentUser();
        if (user == null || editFieldsBox == null) return;

        boolean showing = editFieldsBox.isVisible();
        if (!showing) {
            // Show edit fields, populate with current data
            editFieldsBox.setVisible(true);
            editFieldsBox.setManaged(true);
            if (editNameField != null) editNameField.setText(user.getName());
            if (editEmailField != null) editEmailField.setText(user.getEmail());
            if (editBioField != null) editBioField.setText(user.getBio() != null ? user.getBio() : "");
        } else {
            editFieldsBox.setVisible(false);
            editFieldsBox.setManaged(false);
        }
    }

    @FXML
    private void handleSaveProfile() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        String newName = editNameField != null ? editNameField.getText().trim() : user.getName();
        String newEmail = editEmailField != null ? editEmailField.getText().trim() : user.getEmail();
        String newBio = editBioField != null ? editBioField.getText().trim() : "";

        if (newName.isEmpty()) { msg("⚠ Name cannot be empty."); return; }
        if (newName.length() < 3) { msg("⚠ Name must be at least 3 characters."); return; }
        if (newEmail.isEmpty()) { msg("⚠ Email cannot be empty."); return; }
        if (!newEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            msg("⚠ Please enter a valid email."); return;
        }

        // Check if email changed and is taken
        if (!newEmail.equals(user.getEmail())) {
            User existing = userDAO.findByEmail(newEmail);
            if (existing != null) { msg("⚠ Email already in use."); return; }
        }

        user.setName(newName);
        user.setEmail(newEmail);
        user.setBio(newBio);

        if (userDAO.update(user)) {
            AuthService.setCurrentUser(user);
            if (profileNameLabel != null) profileNameLabel.setText(newName);
            if (profileEmailLabel != null) profileEmailLabel.setText(newEmail);
            if (profileBioLabel != null) profileBioLabel.setText(newBio.isEmpty() ? "No bio yet." : newBio);
            if (editFieldsBox != null) { editFieldsBox.setVisible(false); editFieldsBox.setManaged(false); }
            msg("✔ Profile updated successfully!");
        } else {
            msg("✖ Error updating profile.");
        }
    }

    @FXML
    private void handleSendSupport() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        // Create a dialog for support ticket
        Dialog<ButtonType> dialog = new Dialog<>();
        DialogHelper.styleDialog(dialog);
        dialog.setTitle("Help & Support");
        dialog.setHeaderText("Send a message to the administrator");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setPrefWidth(400);

        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject (e.g. 'Login issue', 'Bug report')");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Describe your problem in detail...");
        messageArea.setPrefRowCount(5);
        messageArea.setWrapText(true);

        content.getChildren().addAll(
                new Label("Subject:"), subjectField,
                new Label("Message:"), messageArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String subject = subjectField.getText().trim();
            String message = messageArea.getText().trim();

            if (subject.isEmpty()) { msg("⚠ Subject is required."); return; }
            if (message.isEmpty()) { msg("⚠ Message is required."); return; }

            SupportTicket ticket = new SupportTicket();
            ticket.setUserId(user.getId());
            ticket.setSubject(subject);
            ticket.setMessage(message);

            if (ticketDAO.insert(ticket)) {
                msg("✔ Support ticket sent! Admin will respond soon.");
            } else {
                msg("✖ Error sending ticket.");
            }
        }
    }

    @FXML
    private void handleDeleteProfile() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        Alert warning = new Alert(Alert.AlertType.WARNING);
        DialogHelper.styleAlert(warning);
        warning.setTitle("⚠ Delete Profile");
        warning.setHeaderText("This action is IRREVERSIBLE!");
        warning.setContentText(
                "Deleting your profile will permanently remove:\n\n" +
                "• Your account and login credentials\n" +
                "• ALL your CVs (" + cvDAO.findByUserId(user.getId()).size() + " CVs)\n" +
                "• All your job applications and notifications\n\n" +
                "Are you absolutely sure you want to delete your profile?"
        );
        warning.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = warning.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            Alert finalConfirm = new Alert(Alert.AlertType.CONFIRMATION);
            DialogHelper.styleAlert(finalConfirm);
            finalConfirm.setTitle("Final Confirmation");
            finalConfirm.setHeaderText("Last chance to cancel");
            finalConfirm.setContentText("Click OK to permanently delete your account.");

            Optional<ButtonType> finalResult = finalConfirm.showAndWait();
            if (finalResult.isPresent() && finalResult.get() == ButtonType.OK) {
                if (user.getProfilePicture() != null) {
                    new File(user.getProfilePicture()).delete();
                }
                userDAO.delete(user.getId());
                AuthService.logout();
                loadScene("/view/landing.fxml");
            }
        }
    }

    // ══════════════════════════ LOGIN ══════════════════════════

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String pass = passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) { msg("⚠ Please fill in all fields."); return; }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) { msg("⚠ Please enter a valid email address."); return; }
        if (pass.length() < 4) { msg("⚠ Password must be at least 4 characters."); return; }

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

    // ══════════════════════════ REGISTER ══════════════════════════

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = regEmailField.getText().trim();
        String pass = regPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (name.isEmpty()) { msg("⚠ Full Name is required."); return; }
        if (name.length() < 3) { msg("⚠ Full Name must be at least 3 characters."); return; }
        if (!name.matches("^[A-Za-zÀ-ÿ\\s'-]+$")) { msg("⚠ Full Name can only contain letters."); return; }
        if (email.isEmpty()) { msg("⚠ Email is required."); return; }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) { msg("⚠ Please enter a valid email."); return; }
        if (pass.isEmpty()) { msg("⚠ Password is required."); return; }
        if (pass.length() < 6) { msg("⚠ Password must be at least 6 characters."); return; }
        if (!pass.equals(confirm)) { msg("⚠ Passwords don't match."); return; }

        if (authService.register(name, email, pass)) {
            loadScene("/view/login.fxml");
        } else {
            msg("✖ Email already exists.");
        }
    }

    // ══════════════════════════ NAVIGATION ══════════════════════════

    @FXML private void goToRegister() { loadScene("/view/register.fxml"); }
    @FXML private void goToLogin() { loadScene("/view/login.fxml"); }
    @FXML private void goToCv() { editCvId = -1; loadScene("/view/cv.fxml"); }
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

    private String safe(String s) { return s == null ? "" : s; }

    private void loadScene(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
