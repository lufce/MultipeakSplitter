package Splitter;

import java.io.File;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.stage.FileChooser;

public class Controller implements Initializable {
	//private String crlf = System.getProperty("line.separator");
	
	@FXML Button bSample;
	@FXML Button bReference;
	@FXML Button bMakeDotplot;
	@FXML TextArea taSample;
	@FXML TextArea taReference;
	@FXML TextArea taLog;
	
	File sampleFile;
	File referenceFile;
	
	@FXML Label lb_mxx;
	@FXML Label lb_mxy;
	@FXML Label lb_tx;
	@FXML Label lb_myx;
	@FXML Label lb_myy;
	@FXML Label lb_ty;
	
	@FXML Canvas cv1;
	double CanvasHeight;
	double CanvasWidth;
	GraphicsContext gc;
	
	boolean[][] dot;
	int dotsize = 1;
	int RefLength = 90;
	int SampleLength = 180;
	double scaled_dotsize;
	double scale = 1.0;
	double scale_step = 1.1;
	
	double MouseX;
	double MouseY;
	double MousePreX;
	double MousePreY;
	double MouseDeltaX;
	double MouseDeltaY;
	double Max=0;
	
	int DotX;
	int DotY;
	int SequenceLength = 0;
	int[] StartPoint = new int[2];
	
	boolean drag = false;
	
	Affine aff = new Affine();
	private static final Affine IDENTITY_TRANSFORM = new Affine(1f,0f,0f,0f,1f,0);
	
	@FXML
	protected void bSampleClick(ActionEvent e){
		FileChooser filechooser = new FileChooser();
		sampleFile = filechooser.showOpenDialog(null);
		if(sampleFile != null) {
			taSample.setText("");
			taSample.setText(sampleFile.getAbsolutePath());	
		}
	}
	
	@FXML
	protected void bReferenceClick(ActionEvent e) {
		FileChooser filechooser = new FileChooser();
		referenceFile = filechooser.showOpenDialog(null);
		if(referenceFile != null) {
			taReference.setText("");
			taReference.setText(referenceFile.getAbsolutePath());	
		}
	}
	
	@FXML
	protected void bMakeDotplotClick(ActionEvent e) {
		try {
			MultipeakDotplot multiDot = new MultipeakDotplot(new File(taSample.getText()),new File(taReference.getText()));
			multiDot.makeMultipeakDotplot(10, 7);
			dot = multiDot.getWindowedDotPlot();
			this.DrawDotMap(dot);
		}catch(Exception exception) {
			taLog.setText(exception.getMessage());
		}
		
	}
	
	@FXML
	protected void cv1MouseMoved(MouseEvent e) {
		MouseX = e.getX();
		MouseY = e.getY();
	}
	
	@FXML
	protected void cv1MouseClicked(MouseEvent e) {
		if(drag == false) {
			
			double ClickX = e.getX();
			double ClickY = e.getY();
			
			DotX = (int)Math.ceil((ClickX - aff.getTx() ) / scaled_dotsize);
			DotY = (int)Math.ceil((ClickY - aff.getTy() ) / scaled_dotsize);
			
			if (DotX > 0 && DotY > 0) {
				this.SearchSequence();
				this.EraseCanvas();
				gc.setTransform(aff);
				this.DrawDotMap(dot);
				this.HighlightSelectedDotSequence();
			}
		}else {
			drag = false;
		}
	}
	
	@FXML
	protected void cv1MousePressed(MouseEvent e) {
		MousePreX = e.getX();
		MousePreY = e.getY();
	}
	
	@FXML
	protected void cv1MouseDragged(MouseEvent e) {
		MouseX = e.getX();
		MouseY = e.getY();
		
		MouseDeltaX = (MouseX-MousePreX) / aff.getMxx();
		MouseDeltaY = (MouseY-MousePreY) / aff.getMxx();
		
		MousePreX = MouseX;
		MousePreY = MouseY;
		
		this.EraseCanvas();
		aff.appendTranslation(MouseDeltaX,MouseDeltaY);

		gc.setTransform(aff);
		this.DrawDotMap(dot);
		this.HighlightSelectedDotSequence();
		
		drag = true;
	}
	
	void EraseCanvas() {
		gc.setFill(Color.WHITE);
		gc.setTransform(IDENTITY_TRANSFORM);
		gc.fillRect(0, 0, CanvasWidth, CanvasHeight);
	}
	
//	void DrawMousePoint(double x, double y) {
//		gc.setFill(Color.BLACK);
//		gc.fillRect(x, y, 2, 2);
//	}
	
	void RandomMap(boolean[][] map) {
		Random random = new Random();
		
		for(int m = 0; m < map.length; m++) {
			for(int n = 0; n < map[0].length; n++) {
				if(random.nextInt(2) >= 1) {
					map[m][n] = true;
				}else {
					map[m][n] = false;
				}
			}
		}
	}
	
	private void DrawDotMap(boolean[][] map) {
		scaled_dotsize = aff.getMxx()*dotsize;
		
		gc.setFill(Color.BLACK);
		
		for(int m = 0; m < map.length; m++) {
			for(int n = 0; n < map[0].length; n++) {
				if(map[m][n]) {
					//gc.fillRect(m*scaled_dotsize, n*scaled_dotsize, scaled_dotsize, scaled_dotsize);
					gc.fillRect(m*dotsize, n*dotsize, dotsize, dotsize);
				}
			}
		}
		
		DrawAffine(aff);
	}
	
	private void HighlightSelectedDotSequence() {
		if(SequenceLength > 0) {
			gc.setFill(Color.RED);
			
			for(int N = 0; N < SequenceLength; N++) {
				gc.fillRect( (StartPoint[0]-1 + N)*dotsize, (StartPoint[1]-1 + N)*dotsize, dotsize, dotsize);
			}
		}
	}
	
	@FXML
	protected void cv1Scroll(ScrollEvent e) {
	//四隅の余白部分が均等になるように縮小されるよう修正しないといけない。
		
		this.EraseCanvas();
//		gc.setTransform(aff);
		scale = e.getDeltaY() >=0 ? 1.05 : 1/1.05;
		aff.appendScale(scale, scale, MouseX * aff.getMxx(), MouseY * aff.getMxx());
//		aff.append(scale, 0, (1-scale)*MouseX, 0, scale, (1-scale)*MouseY);
		gc.setTransform(aff);
		
		this.DrawDotMap(dot);
		this.HighlightSelectedDotSequence();
		
	}
	
	private void DrawAffine(Affine aff) {
		lb_mxx.setText(Double.toString((double)(Math.round(aff.getMxx() *1000))/1000 ));
		lb_mxy.setText(Double.toString((double)(Math.round(aff.getMxy() *1000))/1000 ));
		lb_tx.setText(Double.toString((double)(Math.round(aff.getTx() *1000))/1000 ));
		lb_myx.setText(Double.toString((double)(Math.round(aff.getMyx() *1000))/1000 ));
		lb_myy.setText(Double.toString((double)(Math.round(aff.getMyy() *1000))/1000 ));
		lb_ty.setText(Double.toString((double)(Math.round(aff.getTy() *1000))/1000 ));
	}
	
	private void SearchSequence() {
		SequenceLength = 0;
		
		int X = DotX - 1;
		int Y = DotY - 1;
		
		if(dot[X][Y]) {
			while(X >= 0 && Y >= 0 && dot[X][Y]) {
				SequenceLength++;
				X--;
				Y--;
			}
			
			StartPoint[0] = X + 2;
			StartPoint[1] = Y + 2;
			
			X = DotX;
			Y = DotY;
			
			while(X < dot.length && Y < dot[0].length && dot[X][Y]) {
				SequenceLength++;
				X++;
				Y++;
			}
		}else {
			SequenceLength = 0;
		}
		
		
	}
	
	@Override
	public void initialize(URL location, ResourceBundle rb) {
		gc = cv1.getGraphicsContext2D();
		
		CanvasHeight = cv1.getHeight();
		CanvasWidth = cv1.getWidth();
		
		EraseCanvas();

	}
}
