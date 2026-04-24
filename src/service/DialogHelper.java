package service;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

/**
 * Utility to apply dark theme styling to JavaFX dialogs.
 * Call styleDialog() on any Alert or Dialog to match the app's premium dark theme.
 */
public class DialogHelper {

    private static final String STYLESHEET = "/css/style.css";

    /** Apply dark theme to an Alert */
    public static void styleAlert(Alert alert) {
        stylePane(alert.getDialogPane());
    }

    /** Apply dark theme to any Dialog */
    public static <T> void styleDialog(Dialog<T> dialog) {
        stylePane(dialog.getDialogPane());
    }

    /** Apply dark theme to a DialogPane directly */
    public static void stylePane(DialogPane pane) {
        try {
            String css = DialogHelper.class.getResource(STYLESHEET).toExternalForm();
            pane.getStylesheets().add(css);
        } catch (Exception e) {
            // Fallback inline styling if CSS resource not found
            pane.setStyle("-fx-background-color: #0e1225;");
        }
        pane.setStyle("-fx-background-color: #0e1225;");
    }
}
