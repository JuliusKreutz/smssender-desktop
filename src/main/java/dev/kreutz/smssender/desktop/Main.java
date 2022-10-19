package dev.kreutz.smssender.desktop;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        if (!getLocalVersion().equals(getRemoteVersion())) {
            MFXGenericDialog dialogContent = MFXGenericDialogBuilder.build().setContentText("There is a new version available. Please download and install it!").makeScrollable(true).get();
            MFXStageDialog dialog = MFXGenericDialogBuilder.build(dialogContent).toStageDialogBuilder().setDraggable(true).setTitle("Update").get();

            dialogContent.addActions(Map.entry(new MFXButton("Download"), event -> {
                getHostServices().showDocument("https://github.com/JuliusKreutz/smssender-desktop/releases/latest");
                System.exit(0);
            }), Map.entry(new MFXButton("Cancel"), event -> dialog.close()));

            dialogContent.setMinSize(400, 200);
            dialog.showAndWait();
        }

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResourceAsStream("img/icon.png"))));
        stage.setTitle("SmsSender");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    private String getLocalVersion() throws IOException {
        return new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getResourceAsStream("version.txt")))).readLine();
    }

    private String getRemoteVersion() throws IOException {
        URL website = new URL("https://api.github.com/repos/JuliusKreutz/smssender-desktop/releases/latest");
        URLConnection connection = website.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        Matcher matcher = Pattern.compile("\"tag_name\":\"([^\"]+)\"").matcher(in.readLine());

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    public static void main(String[] args) {
        launch();
    }
}