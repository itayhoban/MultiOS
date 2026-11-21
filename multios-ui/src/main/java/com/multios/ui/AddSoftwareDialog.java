package com.multios.ui;

import com.multios.api.AppEntry;
import com.multios.api.PlatformType;
import com.multios.core.MultiOsEngine;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dialog that adds a software folder + platform + entry point via {@link MultiOsEngine}.
 */
final class AddSoftwareDialog {

    private AddSoftwareDialog() {
    }

    static Optional<AppEntry> show(Window owner, MultiOsEngine engine) {
        List<PlatformType> platforms = engine.listRegisteredPlatforms();
        if (platforms.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(owner);
            alert.setTitle("No Plugins");
            alert.setHeaderText("No platform plugins are registered");
            alert.setContentText("Ensure plugin modules are on the runtime classpath.");
            alert.showAndWait();
            return Optional.empty();
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Add Software");
        dialog.setHeaderText("Register software from a local folder");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());

        TextField folderField = new TextField();
        folderField.setPromptText("Select software folder…");
        folderField.setEditable(false);
        HBox.setHgrow(folderField, Priority.ALWAYS);

        TextField displayNameField = new TextField();
        displayNameField.setPromptText("Display name");

        ComboBox<PlatformType> platformCombo = new ComboBox<>(FXCollections.observableArrayList(platforms));
        platformCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PlatformType platformType) {
                return platformType == null ? "" : PlatformLabels.displayName(platformType);
            }

            @Override
            public PlatformType fromString(String string) {
                return null;
            }
        });
        platformCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PlatformType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : PlatformLabels.displayName(item));
            }
        });
        platformCombo.getSelectionModel().selectFirst();

        ComboBox<Path> entryPointCombo = new ComboBox<>();
        entryPointCombo.setMaxWidth(Double.MAX_VALUE);
        entryPointCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Path path) {
                if (path == null) {
                    return "";
                }
                Path folder = folderPathOrNull(folderField.getText());
                if (folder != null) {
                    try {
                        return folder.relativize(path).toString();
                    } catch (IllegalArgumentException ignored) {
                        // Fall through to absolute path.
                    }
                }
                return path.toString();
            }

            @Override
            public Path fromString(String string) {
                return null;
            }
        });

        Button browseFolderButton = new Button("Browse…");
        browseFolderButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Software Folder");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                Path folder = selected.toPath().toAbsolutePath().normalize();
                folderField.setText(folder.toString());
                if (displayNameField.getText().isBlank()) {
                    Path name = folder.getFileName();
                    displayNameField.setText(name != null ? name.toString() : folder.toString());
                }
                refreshEntryPoints(engine, platformCombo.getValue(), folder, entryPointCombo);
            }
        });

        Button browseEntryButton = new Button("Browse file…");
        browseEntryButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Entry Point");
            Path folder = folderPathOrNull(folderField.getText());
            if (folder != null && Files.isDirectory(folder)) {
                chooser.setInitialDirectory(folder.toFile());
            }
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                Path entry = selected.toPath().toAbsolutePath().normalize();
                List<Path> items = new ArrayList<>(entryPointCombo.getItems());
                if (!items.contains(entry)) {
                    items.add(0, entry);
                    entryPointCombo.setItems(FXCollections.observableArrayList(items));
                }
                entryPointCombo.getSelectionModel().select(entry);
            }
        });

        platformCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            Path folder = folderPathOrNull(folderField.getText());
            if (folder != null && newValue != null) {
                refreshEntryPoints(engine, newValue, folder, entryPointCombo);
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        grid.add(new Label("Folder"), 0, 0);
        grid.add(new HBox(8, folderField, browseFolderButton), 0, 1);
        grid.add(new Label("Platform"), 0, 2);
        grid.add(platformCombo, 0, 3);
        grid.add(new Label("Entry point"), 0, 4);
        grid.add(new HBox(8, entryPointCombo, browseEntryButton), 0, 5);
        grid.add(new Label("Display name"), 0, 6);
        grid.add(displayNameField, 0, 7);

        GridPane.setHgrow(folderField, Priority.ALWAYS);
        GridPane.setHgrow(entryPointCombo, Priority.ALWAYS);
        GridPane.setHgrow(displayNameField, Priority.ALWAYS);
        HBox.setHgrow(entryPointCombo, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(620);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        saveButton.setText("Save");
        saveButton.getStyleClass().add("primary");
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String validationError = validate(folderField, platformCombo, entryPointCombo, displayNameField);
            if (validationError != null) {
                event.consume();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.initOwner(owner);
                alert.setTitle("Missing Information");
                alert.setHeaderText("Cannot save software entry");
                alert.setContentText(validationError);
                alert.showAndWait();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return Optional.empty();
        }

        Path folder = Path.of(folderField.getText().trim()).toAbsolutePath().normalize();
        Path entryPoint = entryPointCombo.getValue().toAbsolutePath().normalize();
        PlatformType platform = platformCombo.getValue();
        String displayName = displayNameField.getText().trim();

        AppEntry entry = new AppEntry(
                UUID.randomUUID().toString(),
                displayName,
                folder,
                entryPoint,
                platform,
                null
        );
        engine.saveApp(entry);
        return Optional.of(entry);
    }

    private static void refreshEntryPoints(
            MultiOsEngine engine,
            PlatformType platform,
            Path folder,
            ComboBox<Path> entryPointCombo
    ) {
        List<Path> candidates = engine.findEntryPointCandidates(platform, folder);
        entryPointCombo.setItems(FXCollections.observableArrayList(candidates));
        if (!candidates.isEmpty()) {
            entryPointCombo.getSelectionModel().selectFirst();
        } else {
            entryPointCombo.getSelectionModel().clearSelection();
        }
    }

    private static Path folderPathOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return Path.of(text.trim());
    }

    private static String validate(
            TextField folderField,
            ComboBox<PlatformType> platformCombo,
            ComboBox<Path> entryPointCombo,
            TextField displayNameField
    ) {
        if (folderField.getText() == null || folderField.getText().isBlank()) {
            return "Please select a software folder.";
        }
        if (platformCombo.getValue() == null) {
            return "Please select a platform.";
        }
        if (entryPointCombo.getValue() == null) {
            return "Please select or browse to an entry-point file.";
        }
        if (displayNameField.getText() == null || displayNameField.getText().isBlank()) {
            return "Please enter a display name.";
        }
        return null;
    }
}
