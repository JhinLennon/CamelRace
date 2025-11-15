package tarea.camelrace;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class CamelApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                CamelApplication.class.getResource("camel-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 967, 606);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }


}
