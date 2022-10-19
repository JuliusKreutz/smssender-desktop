module dev.kreutz.smssender.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires smssender.shared;
    requires MaterialFX;

    opens dev.kreutz.smssender.desktop to javafx.fxml;
    exports dev.kreutz.smssender.desktop;
}
