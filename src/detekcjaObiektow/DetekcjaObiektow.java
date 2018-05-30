package detekcjaObiektow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.stage.Stage;
import org.opencv.core.Core;

public class DetekcjaObiektow extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        Parent root = FXMLLoader.load(getClass().getResource("FXMLUI.fxml"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLUI.fxml"));      
        
        FXMLUIController controller = loader.getController();
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.show();       
    }
    public static void main(String[] args) {        
        launch(args);       
    }
    
}
