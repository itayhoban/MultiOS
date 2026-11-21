package com.multios.ui;

import com.multios.api.AppEntry;
import com.multios.core.AppRunStatus;
import com.multios.core.MultiOsEngine;
import com.multios.core.ProcessOutputStream;
import com.multios.core.ProcessSession;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

/**
 * JavaFX desktop shell for MultiOs. All platform behavior is delegated to {@link MultiOsEngine}.
 */
public final class MultiOsApp extends Application {

    private MultiOsEngine engine;
    private FlowPane libraryPane;
    private Label emptyStateLabel;
    private TextArea consoleArea;

    @Override
    public void start(Stage primaryStage) {
        engine = new MultiOsEngine();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        root.setTop(buildTopBar(primaryStage));

        libraryPane = new FlowPane(14, 14);
        libraryPane.getStyleClass().add("library-pane");
        libraryPane.setPadding(new Insets(16));
        libraryPane.setPrefWrapLength(900);

        emptyStateLabel = new Label("No software yet. Click “+ Add Software” to register an app folder.");
        emptyStateLabel.getStyleClass().add("empty-state");

        ScrollPane libraryScroll = new ScrollPane(libraryPane);
        libraryScroll.setFitToWidth(true);
        libraryScroll.setFitToHeight(true);

        VBox consolePanel = buildConsolePanel();

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.getItems().addAll(libraryScroll, consolePanel);
        splitPane.setDividerPositions(0.68);
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1100, 720);
        var theme = Objects.requireNonNull(
                MultiOsApp.class.getResource("/com/multios/ui/dark-theme.css"),
                "dark-theme.css missing"
        );
        scene.getStylesheets().add(theme.toExternalForm());

        primaryStage.setTitle("MultiOs");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> shutdown());
        primaryStage.show();

        refreshLibrary();
        appendConsoleLine("SYSTEM", "Runtime Vault: " + engine.getRuntimeVaultDir());
        appendConsoleLine("SYSTEM", "Registered platforms: "
                + engine.listRegisteredPlatforms().stream().map(PlatformLabels::displayName).toList());
    }

    private HBox buildTopBar(Stage stage) {
        Label title = new Label("MultiOs");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Meta-OS Runtime Orchestrator");
        subtitle.getStyleClass().add("subtitle");

        VBox branding = new VBox(2, title, subtitle);
        branding.setAlignment(Pos.CENTER_LEFT);

        Button addButton = new Button("+ Add Software");
        addButton.getStyleClass().add("primary");
        addButton.setOnAction(event -> {
            AddSoftwareDialog.show(stage, engine).ifPresent(entry -> {
                appendConsoleLine("SYSTEM", "Added \"" + entry.displayName() + "\" ("
                        + PlatformLabels.displayName(entry.platformType()) + ")");
                refreshLibrary();
            });
        });

        Button settingsButton = new Button("Settings");
        settingsButton.setOnAction(event -> {
            SettingsDialog.show(stage, engine);
            appendConsoleLine("SYSTEM", "Runtime Vault: " + engine.getRuntimeVaultDir());
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, branding, spacer, addButton, settingsButton);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        return topBar;
    }

    private VBox buildConsolePanel() {
        Label consoleTitle = new Label("Live Console");
        consoleTitle.getStyleClass().add("console-title");

        Button clearButton = new Button("Clear Logs");
        clearButton.setOnAction(event -> consoleArea.clear());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, consoleTitle, spacer, clearButton);
        header.setAlignment(Pos.CENTER_LEFT);

        consoleArea = new TextArea();
        consoleArea.getStyleClass().add("console-area");
        consoleArea.setEditable(false);
        consoleArea.setWrapText(false);
        VBox.setVgrow(consoleArea, Priority.ALWAYS);

        VBox panel = new VBox(8, header, consoleArea);
        panel.getStyleClass().add("console-panel");
        panel.setPrefHeight(220);
        return panel;
    }

    private void refreshLibrary() {
        libraryPane.getChildren().clear();
        List<AppEntry> apps = engine.listApps();
        if (apps.isEmpty()) {
            libraryPane.getChildren().add(emptyStateLabel);
            return;
        }
        for (AppEntry app : apps) {
            libraryPane.getChildren().add(buildAppCard(app));
        }
    }

    private VBox buildAppCard(AppEntry app) {
        Label nameLabel = new Label(app.displayName());
        nameLabel.getStyleClass().add("app-card-title");

        Label platformBadge = new Label(PlatformLabels.displayName(app.platformType()));
        platformBadge.getStyleClass().add("badge");

        AppRunStatus status = engine.getAppRunStatus(app.id());
        Label statusBadge = new Label(status.name());
        statusBadge.getStyleClass().addAll("badge", statusStyleClass(status));

        HBox badges = new HBox(8, platformBadge, statusBadge);

        Button launchButton = new Button("Launch");
        launchButton.getStyleClass().add("primary");
        launchButton.setDisable(status == AppRunStatus.RUNNING);
        launchButton.setOnAction(event -> launchApp(app));

        Button stopButton = new Button("Stop");
        stopButton.setDisable(status != AppRunStatus.RUNNING);
        stopButton.setOnAction(event -> {
            engine.stop(app.id());
            appendConsoleLine("SYSTEM", "Stop requested for \"" + app.displayName() + "\"");
            refreshLibrary();
        });

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().add("danger");
        removeButton.setOnAction(event -> {
            engine.deleteApp(app.id());
            appendConsoleLine("SYSTEM", "Removed \"" + app.displayName() + "\"");
            refreshLibrary();
        });

        HBox actions = new HBox(8, launchButton, stopButton, removeButton);

        VBox card = new VBox(10, nameLabel, badges, actions);
        card.getStyleClass().add("app-card");
        card.setPrefWidth(280);
        return card;
    }

    private void launchApp(AppEntry app) {
        try {
            appendConsoleLine("SYSTEM", "Launching \"" + app.displayName() + "\"…");
            ProcessSession session = engine.launch(app, (stream, line) ->
                    Platform.runLater(() -> appendConsoleLine(streamLabel(stream), line)));
            session.onExit().whenComplete((exitCode, error) -> Platform.runLater(() -> {
                if (error != null) {
                    appendConsoleLine("SYSTEM", "\"" + app.displayName() + "\" failed: " + error.getMessage());
                } else {
                    appendConsoleLine("SYSTEM", "\"" + app.displayName() + "\" exited with code " + exitCode);
                }
                refreshLibrary();
            }));
            refreshLibrary();
        } catch (RuntimeException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Launch Failed");
            alert.setHeaderText("Could not launch \"" + app.displayName() + "\"");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
            appendConsoleLine("SYSTEM", "Launch failed: " + ex.getMessage());
            refreshLibrary();
        }
    }

    private void appendConsoleLine(String channel, String line) {
        consoleArea.appendText("[" + channel + "] " + line + System.lineSeparator());
    }

    private static String streamLabel(ProcessOutputStream stream) {
        return stream == ProcessOutputStream.STDOUT ? "STDOUT" : "STDERR";
    }

    private static String statusStyleClass(AppRunStatus status) {
        return switch (status) {
            case IDLE -> "status-idle";
            case RUNNING -> "status-running";
            case EXITED -> "status-exited";
        };
    }

    private void shutdown() {
        if (engine != null) {
            engine.close();
            engine = null;
        }
    }

    @Override
    public void stop() {
        shutdown();
    }
}
