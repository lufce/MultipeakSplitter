package Splitter;

import java.net.URL;

import javafx.event.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Application;
import javafx.scene.layout.Pane;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class DotShowWindow extends Application {
	@FXML private Button bt_execute;
	@FXML private TextField tf_window, tf_cutoff;
	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage stage) throws Exception{
		try {
			
			URL path = getClass().getResource("ui.fxml");
			System.out.println(path.toString());
			FXMLLoader loader = new FXMLLoader(path);
			Pane pane = (Pane) loader.load();
			Scene scene = new Scene(pane,500,400);
			
			stage.setScene(scene);
			stage.show();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	protected void onExecuteClick(ActionEvent e) {
		System.out.println("clicked");
	}
	
	@FXML
	protected void onExecuteOver(ActionEvent e) {
		bt_execute.setText("overed");
	}
	
}
