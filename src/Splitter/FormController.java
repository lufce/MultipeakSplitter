package Splitter;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class FormController {
	
	@FXML Button bt_execute;

	@FXML
	protected void onExecuteClick(ActionEvent e) {
		System.out.println("clicked");
	}
	
	@FXML
	protected void onExecuteOver(ActionEvent e) {
		bt_execute.setText("overed");
	}
}
