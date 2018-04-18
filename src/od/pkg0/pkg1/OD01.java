/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package od.pkg0.pkg1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.opencv.core.Core;

/**
 *
 * @author Piotr
 */
public class OD01 extends Application {
    
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        launch(args);
        
        
    }
    
}
