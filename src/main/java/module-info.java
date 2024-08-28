
module simple.musicplayer {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.fxml;
    requires rxcontrols;
    requires dom4j;

    exports edu.szu.musicplayer;

    opens edu.szu.musicplayer to javafx.fxml;

}