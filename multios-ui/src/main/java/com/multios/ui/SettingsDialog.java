package com.multios.ui;

import com.multios.core.MultiOsEngine;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;

/**
 * Dialog for choosing the Runtime Vault directory used by all plugins.
 */
final class SettingsDialog {

    private SettingsDialog() {
    }

    static void show(Window owner, MultiOsEngine engine) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Settings");
        dialog.setHeaderText("Runtime Vault");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());

        TextField vaultField = new TextField(engine.getRuntimeVaultDir().toString());
        vaultField.setEditable(false);
        HBox.setHgrow(vaultField, Priority.ALWAYS);

        Button browseButton = new Button("Browse…");
        browseButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Runtime Vault Folder");
            Path current = engine.getRuntimeVaultDir();
            File initial = current.toFile();
            if (initial.isDirectory()) {
                chooser.setInitialDirectory(initial);
            } else if (initial.getParentFile() != null && initial.getParentFile().isDirectory()) {
                chooser.setInitialDirectory(initial.getParentFile());
            }
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                vaultField.setText(selected.toPath().toAbsolutePath().normalize().toString());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Runtime Vault folder"), 0, 0);
        grid.add(new HBox(8, vaultField, browseButton), 0, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("primary");

        dialog.showAndWait().ifPresent(result -> {
            if (result.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    Path selected = Path.of(vaultField.getText().trim());
                    engine.updateRuntimeVaultDir(selected);
                } catch (RuntimeException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(owner);
                    alert.setTitle("Settings Error");
                    alert.setHeaderText("Could not save Runtime Vault path");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            }
        });
    }
}
