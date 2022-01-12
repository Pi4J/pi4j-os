package hellofx;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * A tiny tool to check the basic setup of JavaFX.
 *
 * It's a long way from here to a real application.
 *
 * It's not meant to be any kind of template to start your development.
 *
 * Initially copied from
 * https://github.com/openjfx/samples/blob/master/CommandLine/Modular/CLI/hellofx/src/hellofx/HelloFX.java
 *
 * @author Dieter Holz
 */
public class HelloFX extends Application {

    @Override
    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");

        MediaPlayer mediaPlayer = new MediaPlayer(new Media(getClass().getResource("bgm.mp3").toExternalForm()));
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setAutoPlay(true);
        MediaView mediaView = new MediaView(mediaPlayer);

        Label lbl = new Label("JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");

        AudioClip dropSound = new AudioClip(getClass().getResource("drop.wav").toExternalForm());
        Button btn = new Button("Say Hello");
        btn.setOnAction(event -> {
            lbl.setText("Hello");
            dropSound.stop();
            dropSound.play();
        });

        ImageView imgView = new ImageView(new Image(HelloFXOS.class.getResourceAsStream("openduke.png")));
        imgView.setFitHeight(200);
        imgView.setPreserveRatio(true);

        VBox rootPane = new VBox(50, mediaView, imgView, lbl, btn);
        rootPane.setAlignment(Pos.CENTER);

        rootPane.getStylesheets().add(HelloFXOS.class.getResource("styles.css").toExternalForm());

        Scene scene = new Scene(rootPane, 640, 480);

        //if started in DRM, make stage full-screen
        if (System.getProperty("monocle.egl.lib") != null) {
            Rectangle2D bounds = Screen.getPrimary().getBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        }
        stage.setTitle("Plain JavaFX App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("++++++++++++ shutdown hook ++++++++++++");
        }));
        launch();
    }

}