package hellopicade;

import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * A tiny tool to check the basic setup of Picade Console.
 *
 * Only 5 keys should be recognized: UP, DOWN, LEFT, RIGHT, ESCAPE, ENTER
 *
 * These standard keys, even connected via GPIO, are mapped to standard JavaFX KeyEvents. Therefore the game should behave exactly the same
 * if you use one of them on desktop or on Picade Console without any further configuration or programming.
 *
 * Use these keys to move Duke, increase speed or reset all.
 *
 * All recognized KeyEvents are printed to console. There shouldn't be any other key.
 *
 * Please note: This example is meant to test the fundamental Picade configuration. It's NOT a template for starting your game development.
 *
 * @author Dieter Holz
 */
public class HelloPicade extends Application {

    // the number of pixels Duke moves on a single KeyEvent
    private final IntegerProperty speed = new SimpleIntegerProperty(5);

    @Override
    public void start(Stage stage) {
        Label lbl = new Label();
        lbl.textProperty().bind(speed.asString("Speed: %d"));

        ImageView imgView = new ImageView(new Image(HelloPicade.class.getResourceAsStream("openduke.png")));
        imgView.setFitHeight(200);
        imgView.setPreserveRatio(true);

        VBox rootPane = new VBox(50, imgView, lbl);
        rootPane.setAlignment(Pos.CENTER);

        rootPane.getStylesheets().add(HelloPicade.class.getResource("styles.css").toExternalForm());

        Scene scene = new Scene(rootPane, 640, 480);
        scene.addEventFilter(KeyEvent.KEY_PRESSED,
                                     event -> {
                                         // any KeyEvent will be printed, there shouldn't be any other than THE six
                                         System.out.println(event.getCode() + " pressed");
                                         switch (event.getCode()){
                                             case LEFT:
                                                 imgView.setTranslateX(imgView.getTranslateX() - speed.get());
                                                 break;
                                             case RIGHT:
                                                 imgView.setTranslateX(imgView.getTranslateX() + speed.get());
                                                 break;
                                             case UP:
                                                 imgView.setTranslateY(imgView.getTranslateY() - speed.get());
                                                 break;
                                             case DOWN:
                                                 imgView.setTranslateY(imgView.getTranslateY() + speed.get());
                                                 break;
                                             case ENTER:
                                                 speed.set(speed.get() + 5);
                                                 break;
                                             case ESCAPE:
                                                 imgView.setTranslateX(0);
                                                 imgView.setTranslateY(0);
                                                 speed.set(5);
                                                 break;
                                         }
                                     });

         scene.addEventFilter(KeyEvent.KEY_RELEASED,
                                     event -> System.out.println(event.getCode() + " released "));

        stage.setTitle("Picade KeyEvent Test");
        stage.setScene(scene);
        stage.show();
    }


    public static void main(String[] args) {
        launch();
    }

}