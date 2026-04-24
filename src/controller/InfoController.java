package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class InfoController {

    @FXML private VBox rootPane;

    @FXML
    private void goToLanding() {
        loadScene("/view/landing.fxml");
    }

    @FXML
    private void goToLogin() {
        loadScene("/view/login.fxml");
    }

    @FXML
    private void goToRegister() {
        loadScene("/view/register.fxml");
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
}
