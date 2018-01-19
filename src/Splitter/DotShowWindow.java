package Splitter;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class DotShowWindow extends Application {
	
	Label label;
	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage stage) throws Exception{
		label = new Label("Hellow");
		BorderPane pane = new BorderPane();
		
		pane.setTop(label);
		
		Scene scene = new Scene(pane, 320, 120);
		stage.setScene(scene);
		stage.show();
	}
}
