package dev.kreutz.smssender.desktop;

import dev.kreutz.smssender.shared.Const;
import io.github.palexdev.materialfx.beans.NumberRange;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXCheckListView;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainViewController implements Initializable {
    @FXML
    public TextArea textArea;
    @FXML
    public MFXButton sendButton;
    @FXML
    public MFXButton errorButton;
    @FXML
    public VBox phones;
    @FXML
    public MFXCheckListView<String> groupsList;

    private final Map<Phone, PhoneButtonController> phoneControllers = new HashMap<>();
    private final List<String> errors = new ArrayList<>();

    private Phone selected;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new Thread(this::listenToConnections).start();

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress address = InetAddress.getByName(Const.MULTICAST_ADDRESS);
                DatagramPacket packet = new DatagramPacket(new byte[0], 0, address, Const.MULTICAST_PORT);
                socket.send(packet);
            } catch (IOException ignored) {
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void listenToConnections() {
        try (ServerSocket serverSocket = new ServerSocket(Const.TCP_PORT)) {
            while (!Thread.interrupted()) {
                try {
                    Socket socket = serverSocket.accept();

                    FXMLLoader loader = new FXMLLoader(Main.class.getResource("fxml/phone-button.fxml"));
                    loader.load();

                    Phone phone = new Phone(socket);
                    PhoneButtonController controller = loader.getController();
                    phoneControllers.put(phone, controller);

                    Platform.runLater(() -> {
                        controller.button.setText(phone.getName());
                        controller.button.setOnAction(e -> {
                            if (selected != null) phoneControllers.get(selected).button.setSelected(false);
                            selected = phone;

                            controller.button.setSelected(true);
                            groupsList.setItems(FXCollections.observableList(new ArrayList<>(phone.getGroups())));

                            sendButton.setDisable(phone.isBusy());
                        });

                        controller.bar.getRanges1().add(NumberRange.of(0.0, 0.50));
                        controller.bar.getRanges2().add(NumberRange.of(0.51, 0.99));
                        controller.bar.getRanges3().add(NumberRange.of(0.99, 1.0));

                        phones.getChildren().add(controller.button);
                    });
                } catch (Exception ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    @FXML
    public void send() {
        Phone phone = selected;

        if (phone == null) return;

        if (!phone.setText(textArea.getText())) return;

        phone.setBusy(true);
        sendButton.setDisable(true);

        PhoneButtonController controller = phoneControllers.get(phone);
        controller.bar.setProgress(0);

        new Thread(() -> {
            Set<String> groups = new HashSet<>(groupsList.getSelectionModel().getSelectedValues());

            Set<String> numbers = phone.getNumbers(groups).stream().map(n -> n.replaceAll("\\s+", "")).map(n -> {
                if (n.startsWith("04")) {
                    return n.replaceFirst("0", "+61");
                }

                return n;
            }).collect(Collectors.toSet());

            int i = 1;
            for (String number : numbers) {
                if (!phone.sendSms(number)) {
                    Platform.runLater(() -> errorButton.setVisible(true));
                    errors.add("ERROR: Sending sms from " + phone.getName() + " to " + number);
                }
                controller.bar.setProgress((double) i++ / (double) numbers.size());
            }

            phone.setBusy(false);

            if (phone == selected) Platform.runLater(() -> sendButton.setDisable(false));
        }).start();
    }

    @FXML
    public void clear() {
        groupsList.getSelectionModel().clearSelection();
    }

    @FXML
    public void errors() {
        String errorsJoined = String.join("\n", errors);

        MFXGenericDialog dialogContent = MFXGenericDialogBuilder.build().setContentText(errorsJoined).makeScrollable(true).get();

        MFXStageDialog dialog = MFXGenericDialogBuilder.build(dialogContent).toStageDialogBuilder().setDraggable(true).setTitle("Errors").get();

        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(errorsJoined);
        dialogContent.addActions(Map.entry(new MFXButton("Copy"), event -> Clipboard.getSystemClipboard().setContent(clipboardContent)), Map.entry(new MFXButton("Ok"), event -> dialog.close()));

        dialogContent.setMinSize(400, 200);
        dialog.showDialog();
    }
}