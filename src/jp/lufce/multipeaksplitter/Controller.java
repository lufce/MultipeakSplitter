package jp.lufce.multipeaksplitter;

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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.stage.FileChooser;

public class Controller implements Initializable {
	private String crlf = System.getProperty("line.separator");
	
	private MultipeakDotplot multiDot;
	private MultipeakDotplot revcomMultiDot;
	private Ab1Sequence sample;
	private ReferenceSequence refseq;
	private int window;
	private int cutoff;
	
	@FXML protected TabPane tabPane1;
	
	@FXML protected Button bSample;
	@FXML protected Button bReference;
	@FXML protected Button bMakeDotplot;
	@FXML protected TextArea taSamplePath;
	@FXML protected TextArea taReferencePath;
	@FXML protected TextArea taLog;
	
	private File sampleFile;
	private File referenceFile;
	
	@FXML protected Tab tabSequence;
	@FXML protected TextField tfCutoff;
	@FXML protected TextField tfWindow;
	@FXML protected Button bRemake;
	@FXML protected Button bResetView;
	@FXML protected CheckBox checkRevcom;
	@FXML protected TextArea taSelectedForwardSequence;
	@FXML protected TextArea taSelectedReverseSequence;
	
	@FXML protected Canvas cv1;
	private double CanvasHeight;
	private double CanvasWidth;
	private GraphicsContext gc1;
	
	@FXML protected Canvas cvReference;
	@FXML protected Canvas cvSample;
	@FXML protected Slider sliderSamplePosition;
	@FXML protected Slider sliderSampleScale;
	private int sliderSampleValue;
	private GraphicsContext gcSample;
	private int sampleCanvasHeight;
	private int sampleCanvasWidth;
	@FXML protected TextArea taReferenceSequence;
	
	private int[][] multiIntensity;
	private int sampleDrawInterval = 1;
	private double sampleDrawScale = 1;
	
	@FXML protected Label lb_mxx;
	@FXML protected Label lb_mxy;
	@FXML protected Label lb_tx;
	@FXML protected Label lb_myx;
	@FXML protected Label lb_myy;
	@FXML protected Label lb_ty;
	
	private boolean[][] windowedDotmap;
	private boolean[][] windowedRevcomDotmap;
	private boolean revcomFlag = false;
	//TODO revcomFlagを使って条件分岐をし続けると処理が分散して面倒な気がする。一括でできないだろうか？
	private int dotsize = 1;
	private double scale = 1.0;
	
	private double MouseX;
	private double MouseY;
	private double MousePreX;
	private double MousePreY;
	private double MouseDeltaX;
	private double MouseDeltaY;
	
	private int DotX;
	private int DotY;
	private int forwardSequenceLength = 0;
	private int[] forwardStartPoint = new int[2];
	private int reverseSequenceLength = 0;
	private int[] reverseStartPoint = new int[2];
	
	private boolean drag = false;
	
	private Affine aff = new Affine();
	private static final Affine IDENTITY_TRANSFORM = new Affine(1f,0f,0f,0f,1f,0);
	
	@FXML
	protected void bSampleClick(ActionEvent e){
		FileChooser filechooser = new FileChooser();
		sampleFile = filechooser.showOpenDialog(null);
		if(sampleFile != null) {
			taSamplePath.setText("");
			taSamplePath.setText(sampleFile.getAbsolutePath());	
		}
	}
	
	@FXML
	protected void bReferenceClick(ActionEvent e) {
		FileChooser filechooser = new FileChooser();
		referenceFile = filechooser.showOpenDialog(null);
		if(referenceFile != null) {
			taReferencePath.setText("");
			taReferencePath.setText(referenceFile.getAbsolutePath());	
		}
	}
	
	@FXML
	protected void bMakeDotplotClick(ActionEvent e) {
		//TODO setValueが例外を投げるようになったら、ちゃんと例外処理する。
		
		taLog.appendText("start making dotplot..." + crlf);
		try {
			sample = new Ab1Sequence(new File(taSamplePath.getText()));
			refseq = new ReferenceSequence(new File(taReferencePath.getText()));
			
			if(this.setValueOfCutoffWindow() == false) {
				taLog.appendText("setValue failed"+crlf);
				return;
			}
			
			sample.makeMultipeak(cutoff);
			multiDot = new MultipeakDotplot(sample.getMultipeakMap(), refseq.getRefseqMap(), window);
			revcomMultiDot = new MultipeakDotplot(sample.getRevcomMultipeakMap(), refseq.getRefseqMap(), window);
			
			windowedDotmap = multiDot.getWindowedDotPlot();
			windowedRevcomDotmap = revcomMultiDot.getWindowedDotPlot();
			
			this.EraseCanvas(gc1);
			
			if(revcomFlag) {
				this.DrawDotMap();
			}else {
				this.DrawDotMap();
			}
			
			this.setReferenceSequenceString();
			taLog.appendText("end making dotplot"+crlf);
			
			multiIntensity = sample.getMultiAllIntensity();
			this.DrawSampleWave(0);
			
			sliderSamplePosition.setMax(Math.floor((sample.getTraceLength() - sliderSamplePosition.getWidth()) / sampleDrawInterval));
			sliderSamplePosition.valueProperty().addListener( (a, b, c) -> this.sliderSamplePositionSlide() );
			sliderSamplePosition.setVisible(true);
			
			sliderSampleScale.setVisible(true);
			
			tabPane1.getSelectionModel().select(tabSequence);

		}catch(Exception exception) {
			taLog.setText(exception.getMessage());
		}
	}
	
	@FXML
	protected void bRemakeClick(ActionEvent e) {
		//TODO setValueが例外を投げるようになったら、ちゃんと例外処理する。
		
		if (multiDot == null) {
			taLog.appendText("Remake failed" + crlf);
		}else {
			taLog.appendText("start remaking dotplot"+crlf);
			if(this.setValueOfCutoffWindow()==false) {
				taLog.appendText("setValue failed" + crlf);
				return;
			}
			sample.makeMultipeak(cutoff);
			multiDot = new MultipeakDotplot(sample.getMultipeakMap(), refseq.getRefseqMap(), window);
			windowedDotmap = multiDot.getWindowedDotPlot();
			this.EraseCanvas(gc1);
			this.DrawDotMap();
			taLog.appendText("end remaking dotplot"+crlf);
			
			tabPane1.getSelectionModel().select(tabSequence);
		}
	}
	
	@FXML
	protected void bResetViewClick() {
		aff = IDENTITY_TRANSFORM.clone();
		this.EraseCanvas(gc1);
		this.DrawDotMap();
	}
	
	private boolean setValueOfCutoffWindow() {
		//TODO これも例外処理に変えないといけない
		
		boolean invalid = false;
		
		window = Integer.parseInt(tfWindow.getText());
		cutoff = Integer.parseInt(tfCutoff.getText());
		
		if(cutoff < 0 || 100 <= cutoff) {
			taLog.appendText("invalid cutoff value" + crlf);
			invalid = true;
		}
		
		if( refseq.getSequenceLength() < window || sample.getSequenceLength() < window) {
			taLog.appendText("window size is beyond sequence length" + crlf);
			invalid = true;
		}
		
		if(window < 0) {
			taLog.appendText("invalid window value");
			invalid = true;
		}
		
		if(invalid == false) {
			tfWindow.setText(String.valueOf(window));
			tfCutoff.setText(String.valueOf(cutoff));
			
			return true;
		}else {
			return false;
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
			
			DotX = (int)Math.ceil((ClickX - aff.getTx() ) / (dotsize * aff.getMxx()) );
			DotY = (int)Math.ceil((ClickY - aff.getTy() ) / (dotsize * aff.getMxx()) );
			
			if (DotX > 0 && DotY > 0) {
				this.SearchSequence();
				this.EraseCanvas(gc1);
				gc1.setTransform(aff);
				this.DrawDotMap();
				this.HighlightSelectedDotSequence();
				
				taSelectedForwardSequence.setText(refseq.getSeqSubstring(forwardStartPoint[1], forwardSequenceLength));
				taSelectedReverseSequence.setText(refseq.getSeqSubstring(reverseStartPoint[1], reverseSequenceLength));
			}
		}else {
			drag = false;
		}
	}
	
	@FXML
	protected void cv1MousePressed(MouseEvent e) {
		MousePreX = e.getX();
		MousePreY = e.getY();
		
		taLog.setText("x: "+ MousePreX + "  y: "+ MousePreY);
	}
	
	@FXML
	protected void cv1MouseDragged(MouseEvent e) {
		MouseX = e.getX();
		MouseY = e.getY();
		
		MouseDeltaX = (MouseX-MousePreX) / aff.getMxx();
		MouseDeltaY = (MouseY-MousePreY) / aff.getMxx();
		
		MousePreX = MouseX;
		MousePreY = MouseY;
		
		this.EraseCanvas(gc1);
		aff.appendTranslation(MouseDeltaX,MouseDeltaY);

		gc1.setTransform(aff);
		this.DrawDotMap();
		this.HighlightSelectedDotSequence();
		
		drag = true;
	}
	
	private void EraseCanvas(GraphicsContext gc) {
		
		if(revcomFlag && gc.equals(gc1)) {
			gc.setFill(Color.BLACK);
		}else {
			gc.setFill(Color.WHITE);
		}
		
		gc.setTransform(IDENTITY_TRANSFORM);
		gc.fillRect(0, 0, CanvasWidth, CanvasHeight);
	}
	
//	void DrawMousePoint(double x, double y) {
//		gc.setFill(Color.BLACK);
//		gc.fillRect(x, y, 2, 2);
//	}
	
	private void RandomMap(boolean[][] map) {
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
	
	private void DrawDotMap() {
//		scaled_dotsize = aff.getMxx()*dotsize;
		
		boolean[][] map;
		
		if(revcomFlag) {
			gc1.setFill(Color.WHITE);
			map = windowedRevcomDotmap;
		}else {
			gc1.setFill(Color.BLACK);
			map = windowedDotmap;
		}
		
		gc1.setTransform(aff);
		
		for(int m = 0; m < map.length; m++) {
			for(int n = 0; n < map[0].length; n++) {
				if(map[m][n]) {
					//gc.fillRect(m*scaled_dotsize, n*scaled_dotsize, scaled_dotsize, scaled_dotsize);
					gc1.fillRect(m*dotsize, n*dotsize, dotsize, dotsize);
				}
			}
		}
		
		DrawAffine(aff);
	}
	
	private void HighlightSelectedDotSequence() {
		if(forwardSequenceLength > 0) {
			gc1.setFill(Color.RED);
			
			for(int N = 0; N < forwardSequenceLength; N++) {
				gc1.fillRect( (forwardStartPoint[0]-1 + N)*dotsize, (forwardStartPoint[1]-1 + N)*dotsize, dotsize, dotsize);
			}
		}
		if(reverseSequenceLength > 0) {
			gc1.setFill(Color.RED);
			
			for(int N = 0; N < reverseSequenceLength; N++) {
				gc1.fillRect( (reverseStartPoint[0]-1 - N)*dotsize, (reverseStartPoint[1]-1 + N)*dotsize, dotsize, dotsize);
			}
		}
	}
	
	@FXML
	protected void cv1Scroll(ScrollEvent e) {
	//TODO 四隅の余白部分が均等になるように縮小されるよう修正しないといけない。
		
		this.EraseCanvas(gc1);
//		gc.setTransform(aff);
		scale = e.getDeltaY() >=0 ? 1.05 : 1/1.05;
		aff.appendScale(scale, scale, (int)Math.ceil((MouseX - aff.getTx() ) / (dotsize * aff.getMxx()) ), (int)Math.ceil((MouseX - aff.getTy() ) / (dotsize * aff.getMyy()) ));
//		aff.append(scale, 0, (1-scale)*MouseX, 0, scale, (1-scale)*MouseY);
		gc1.setTransform(aff);
		
		this.DrawDotMap();
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
		forwardSequenceLength = 0;
		reverseSequenceLength = 0;
		
		int X = DotX - 1;
		int Y = DotY - 1;
		
		if(X >= 0 && X < windowedDotmap.length && Y >= 0 && Y < windowedDotmap[0].length && windowedDotmap[X][Y]) {
			while(X >= 0 && X < windowedDotmap.length && Y >= 0 && Y < windowedDotmap[0].length && windowedDotmap[X][Y] ) {
				forwardSequenceLength++;
				X--;
				Y--;
			}
			
			forwardStartPoint[0] = X + 2;
			forwardStartPoint[1] = Y + 2;
			
			X = DotX;
			Y = DotY;
			
			while(X >= 0 && X < windowedDotmap.length && Y >= 0 && Y < windowedDotmap[0].length && windowedDotmap[X][Y]) {
				forwardSequenceLength++;
				X++;
				Y++;
			}
			
			X = DotX - 1;
			Y = DotY - 1;
			
			while(X >= 0 && X < windowedDotmap.length && Y >= 0 && Y < windowedDotmap[0].length && windowedDotmap[X][Y]) {
				reverseSequenceLength++;
				X++;
				Y--;
			}
			
			reverseStartPoint[0] = X;
			reverseStartPoint[1] = Y + 2;
			
			X = DotX - 2;
			Y = DotY;
			
			while(X >= 0 && X < windowedDotmap.length && Y >= 0 && Y < windowedDotmap[0].length && windowedDotmap[X][Y]) {
				reverseSequenceLength++;
				X--;
				Y++;
			}
			
		}else {
			forwardSequenceLength = 0;
			reverseSequenceLength = 0;
		}
	}
	
	private void setReferenceSequenceString() {
		taReferenceSequence.setText("");
		for(int n=0; n < refseq.getSequenceLength(); n++) {
			taReferenceSequence.appendText(refseq.getSeqString().charAt(n) + crlf);
		}
	}
	
	private void DrawSampleWave(int start) {
		
		double[][] drawIntensity = this.getDrawIntensity(start);
		double localMax = this.getMaxIntensity(drawIntensity);
		Color[] baseColor = {Color.RED, Color.GREEN, Color.ORANGE, Color.BLACK};
		drawIntensity = this.convertDrawIntensity(drawIntensity, localMax);
		
		this.EraseCanvas(gcSample);
		
		gcSample.setLineWidth(0.5);
		
		for(int m = 0; m < drawIntensity[0].length - 1; m++) {
			for(int n = 0; n < 4; n++) {
				gcSample.setStroke(baseColor[n]);
				gcSample.strokeLine(m, drawIntensity[n][m], m+1, drawIntensity[n][m+1]);
			}
		}
	}
	
	private double[][] convertDrawIntensity(double[][] draw, double localMax){
		for(int m = 0; m < draw[0].length; m++) {
			for(int n = 0; n < 4; n++) {
				draw[n][m] = (1 - draw[n][m] / localMax * sampleDrawScale) * sampleCanvasHeight;
			}
		}
		
		return draw;
	}
	
	private double getMaxIntensity(double[][] multi) {
		double localMax = Double.MIN_VALUE;
		
		for(int m = 0; m < multi[0].length; m++) {
			for(int n = 0; n < 4; n++) {
				if(multi[n][m] > localMax) {
					localMax = multi[n][m];
				}
			}
		}
		
		return localMax;
	}
	
	private double[][] getDrawIntensity(int start) throws ArrayIndexOutOfBoundsException {

		if(start + sampleCanvasWidth * sampleDrawInterval > multiIntensity[0].length) {
			System.out.println("out of bounds");
			throw new ArrayIndexOutOfBoundsException();
		}
		
		double[][] drawIntensity = new double[4][sampleCanvasWidth];
		
		for(int m = start; m < start + sampleCanvasWidth ; m++) {
			for(int n = 0; n < 4; n++) {
				drawIntensity[n][m-start] = multiIntensity[n][m * sampleDrawInterval];
			}
		}
		
		return drawIntensity;
	}
	
	private void sliderSamplePositionSlide() {
		sliderSampleValue = (int)Math.round(sliderSamplePosition.getValue());
		this.DrawSampleWave(sliderSampleValue);
	}
	
	private void sliderSampleScaleSlide() {
		sampleDrawScale = sliderSampleScale.getValue();
		this.DrawSampleWave(sliderSampleValue);
	}
	
	@FXML
	protected void checkRevcomClick() {
		revcomFlag = checkRevcom.isSelected();
		this.EraseCanvas(gc1);
		this.DrawDotMap();
	}
	
	@Override
	public void initialize(URL location, ResourceBundle rb) {
		gc1 = cv1.getGraphicsContext2D();
		gcSample = cvSample.getGraphicsContext2D();
		
		CanvasHeight = cv1.getHeight();
		CanvasWidth = cv1.getWidth();
		
		sampleCanvasHeight = (int)Math.floor(cvSample.getHeight());
		sampleCanvasWidth = (int)Math.floor(cvSample.getWidth());
		
		sliderSamplePosition.setVisible(false);
		
		sliderSampleScale.setValue(sampleDrawScale);
		sliderSampleScale.setMin(0.2);
		sliderSampleScale.setMax(20);
		sliderSampleScale.valueProperty().addListener((a, b, c) -> this.sliderSampleScaleSlide() );
		sliderSampleScale.setVisible(false);
		
		EraseCanvas(gc1);
		EraseCanvas(gcSample);
		
		taLog.appendText("Initialization finished."+crlf);
		
		taReferenceSequence.setEditable(false);

	}
}
