package jp.lufce.multipeaksplitter;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import javafx.stage.FileChooser;

public class Controller implements Initializable {
	
	final private String CRLF = System.getProperty("line.separator");
	final private double GRID_LINE_WIDTH = 0.025;
	final private double SAMPLE_SCALE_MAX = 20;
	final private double SAMPLE_SCALE_MIN = 0.2;
	final private double SAMPLE_SCALE_DEFAULT = 1;
	final private double SAMPLE_WAVELINE_WIDTH = 0.5;
	final private Color[] BASE_COLOR = {Color.RED, Color.GREEN, Color.ORANGE, Color.BLACK};
	final private String[] BASE = {"A","C","G","T"};
	// The index 0 to 3 associates with A, C, G, and T, respectively.
	
	private Alert alertDialog;
	
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
	@FXML protected TextField tfSampleZoom;
	private int sliderSampleValue;
	private GraphicsContext gcSample;
	private int sampleCanvasHeight;
	private int sampleCanvasWidth;
	private int sampleDrawedBaseStart = 0;
	private int sampleDrawedBaseEnd = 0;
	private int sampleWaveStart;
	private int sampleWaveEnd;
	@FXML protected TextArea taReferenceSequence;
	
	private int[][] multiIntensity;
	private int sampleDrawInterval = 1;
	private double sampleDrawScale = SAMPLE_SCALE_DEFAULT;
	
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
	
	
//========================== File input =============================
	
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
	
//======================== Making dotplot map ================================

	private void updateDotmapScreen(boolean highlighting) {
		this.EraseCanvas(gc1);
		this.DrawDotMap();
		
		if(highlighting == true) {
			this.HighlightSelectedDotSequence();
		}
	}
	
	@FXML
	protected void bMakeDotplotClick(ActionEvent e) {
		//TODO setValueが例外を投げるようになったら、ちゃんと例外処理する。
		
		taLog.appendText("start making dotplot..." + CRLF);
		try {
			sample = new Ab1Sequence(new File(taSamplePath.getText()));
			refseq = new ReferenceSequence(new File(taReferencePath.getText()));
			
			if(this.setValueOfCutoffWindow() == false) {
				taLog.appendText("setValue failed"+CRLF);
				return;
			}
			
			sample.makeMultipeak(cutoff);
			multiDot = new MultipeakDotplot(sample.getMultipeakMap(), refseq.getRefseqMap(), window);
			revcomMultiDot = new MultipeakDotplot(sample.getRevcomMultipeakMap(), refseq.getRefseqMap(), window);
			
			windowedDotmap = multiDot.getWindowedDotPlot();
			windowedRevcomDotmap = revcomMultiDot.getWindowedDotPlot();
			
			this.setReferenceSequenceString();
			taLog.appendText("end making dotplot"+CRLF);
			
			multiIntensity = sample.getMultiAllIntensity();
			this.DrawSampleWave(0);
			
			sliderSamplePosition.setMax(Math.floor((sample.getTraceLength() - sliderSamplePosition.getWidth()) / sampleDrawInterval));
			sliderSamplePosition.valueProperty().addListener( (a, b, c) -> this.sliderSamplePositionSlide(c.intValue()) );
			sliderSamplePosition.setVisible(true);
			
			sliderSampleScale.setVisible(true);
			
			tfSampleZoom.setText(String.valueOf(sampleDrawScale));
			tfSampleZoom.setVisible(true);
			
			tabPane1.getSelectionModel().select(tabSequence);
			
			forwardSequenceLength = 0;
			reverseSequenceLength = 0;
			

			this.updateDotmapScreen(false);

		}catch(Exception exception) {
			taLog.setText(exception.getMessage());
		}
	}
	
	@FXML
	protected void bRemakeClick(ActionEvent e) {
		//TODO setValueが例外を投げるようになったら、ちゃんと例外処理する。
		
		if (multiDot == null) {
			taLog.appendText("Remake failed" + CRLF);
		}else {
			taLog.appendText("start remaking dotplot"+CRLF);
			if(this.setValueOfCutoffWindow()==false) {
				taLog.appendText("setValue failed" + CRLF);
				return;
			}
			sample.makeMultipeak(cutoff);
			multiDot = new MultipeakDotplot(sample.getMultipeakMap(), refseq.getRefseqMap(), window);
			windowedDotmap = multiDot.getWindowedDotPlot();
			this.EraseCanvas(gc1);
			this.DrawDotMap();
			taLog.appendText("end remaking dotplot"+CRLF);
			this.DrawSampleWave(sampleWaveStart);
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
			taLog.appendText("invalid cutoff value" + CRLF);
			invalid = true;
		}
		
		if( refseq.getSequenceLength() < window || sample.getSequenceLength() < window) {
			taLog.appendText("window size is beyond sequence length" + CRLF);
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
	
	private void EraseCanvas(GraphicsContext gc) {
		
		if(revcomFlag && gc.equals(gc1)) {
			gc.setFill(Color.BLACK);
		}else {
			gc.setFill(Color.WHITE);
		}
		
		gc.setTransform(IDENTITY_TRANSFORM);
		gc.fillRect(0, 0, CanvasWidth, CanvasHeight);
	}
	
	private void DrawDotMap() {
//		scaled_dotsize = aff.getMxx()*dotsize;
		
		boolean[][] map;
		
		gc1.setLineWidth(GRID_LINE_WIDTH);
		gc1.setTransform(aff);
		
		//Highlight sample wave range
		this.calculateRangeOfDrawedBase(sample.getBasecalls(), sampleWaveStart ,sampleWaveEnd);
		
		gc1.setFill(Color.YELLOW);
		gc1.fillRect(sampleDrawedBaseStart, 0, (sampleDrawedBaseEnd - sampleDrawedBaseStart + 1)*dotsize, refseq.getSequenceLength()*dotsize);
		
		
		//Draw dot as filled rectangles.
		if(revcomFlag) {
			gc1.setFill(Color.WHITE);
			gc1.setStroke(Color.WHITE);
			map = windowedRevcomDotmap;
		}else {
			gc1.setFill(Color.BLACK);
			gc1.setStroke(Color.BLACK);
			map = windowedDotmap;
		}
		
		for(int m = 0; m < map.length; m++) {
			for(int n = 0; n < map[0].length; n++) {
				if(map[m][n]) {
					//gc.fillRect(m*scaled_dotsize, n*scaled_dotsize, scaled_dotsize, scaled_dotsize);
					gc1.fillRect(m*dotsize, n*dotsize, dotsize, dotsize);
				}
			}
		}
		
		//Draw lines for grids.
		for(int m = 1; m < map.length ; m++) {
			gc1.strokeLine(m*dotsize, 0, m*dotsize, map[0].length * dotsize);
		}
		
		for(int n = 1; n < map[0].length; n++) {
			gc1.strokeLine(0, n*dotsize, map.length * dotsize, n*dotsize);
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
	
//======================= Mouse-action to move the dotmap position ===============================
	
	@FXML
	protected void cv1MouseMoved(MouseEvent e) {
		MouseX = e.getX();
		MouseY = e.getY();
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
	
//======================== For extraction of selected sequence =======================
	
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
	
//======================= Refseq viewer==================================
	
	private void setReferenceSequenceString() {
		taReferenceSequence.setText("");
		for(int n=0; n < refseq.getSequenceLength(); n++) {
			taReferenceSequence.appendText(refseq.getSeqString().charAt(n) + CRLF);
		}
	}
	
//======================== Sample wave viewer ===========================
	
	private void DrawSampleWave(int start) {
		//TODO RevComに対応させる
		
		sampleWaveStart = start;
		sampleWaveEnd = sampleWaveStart + sampleCanvasWidth * sampleDrawInterval;
		
		double[][] drawIntensity = this.getDrawIntensity(sampleWaveStart);
		double localMax = this.getMaxIntensity(drawIntensity);
		boolean[][] multiMap = sample.getMultipeakMap();
		int[] basecall = sample.getBasecalls();
		int pointer = sampleDrawedBaseStart;
		
		drawIntensity = this.convertDrawIntensity(drawIntensity, localMax);
		
		this.EraseCanvas(gcSample);
		
		gcSample.setLineWidth(SAMPLE_WAVELINE_WIDTH);
		
		for(int m = 0; m < drawIntensity[0].length - 1; m++) {
			for(int n = 0; n < 4; n++) {
				gcSample.setStroke(BASE_COLOR[n]);
				gcSample.strokeLine(m, drawIntensity[n][m], m+1, drawIntensity[n][m+1]);
			}
			
			if(basecall[pointer] == m + sampleWaveStart) {
				for(int n = 0; n < 4; n++) {
					if(multiMap[n][pointer]) {
						gcSample.setFill(BASE_COLOR[n]);
						gcSample.fillText(BASE[n], m - 5, 110 + 10 * n);
					}
				}
				
				pointer++;
			}
		}
	}
	
	private double[][] convertDrawIntensity(double[][] draw, double localMax){
		for(int m = 0; m < draw[0].length; m++) {
			for(int n = 0; n < 4; n++) {
				draw[n][m] = (1 - draw[n][m] / localMax * sampleDrawScale) * sampleCanvasHeight/2;
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

		if(sampleWaveEnd > multiIntensity[0].length) {
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
	
	private void sliderSamplePositionSlide(int start) {
//		sliderSampleValue = (int)Math.round(sliderSamplePosition.getValue());
		sliderSampleValue = start;
		this.EraseCanvas(gc1);
		this.DrawDotMap();
		this.HighlightSelectedDotSequence();
		this.DrawSampleWave(sliderSampleValue);
	}
	
	private void sliderSampleScaleSlide() {
		sampleDrawScale = sliderSampleScale.getValue();
		tfSampleZoom.setText(String.format("%1$.1f", sampleDrawScale));
		this.DrawSampleWave(sliderSampleValue);
	}
	
	@FXML
	protected void tfSampleZoomKeyPressed(KeyEvent e) {
		double scale;
		
		if(e.getCode() == KeyCode.ENTER) {
			
			try {
				scale = Double.parseDouble(tfSampleZoom.getText());
				
				if(SAMPLE_SCALE_MIN <= scale && scale <= SAMPLE_SCALE_MAX) {
					sampleDrawScale = scale;
					sliderSampleScale.setValue(scale);
					this.DrawSampleWave(sliderSampleValue);
				}else {
					alertDialog = new Alert(AlertType.INFORMATION);
					alertDialog.setTitle("Zoom value is out of range.");
					alertDialog.setHeaderText(null);
					alertDialog.setContentText("Zoom value should be between " + SAMPLE_SCALE_MIN + " to " + SAMPLE_SCALE_MAX + ".");
					alertDialog.show();
				}
			}catch(NumberFormatException exception) {
				alertDialog = new Alert(AlertType.INFORMATION);
				alertDialog.setTitle("Invalid number");
				alertDialog.setHeaderText(null);
				alertDialog.setContentText("Zoom value is not number.");
				alertDialog.show();
			}
		}
	}
	
	private void calculateRangeOfDrawedBase(int[] basecall, int start, int end) {
		
		if( start <= basecall[0] ) {
			sampleDrawedBaseStart = 0;
		}else {
			for(int m = 1; m < basecall.length; m++) {
				if(basecall[m-1] < start && start <= basecall[m]) {
					sampleDrawedBaseStart = m;
				}
			}
		}
		
		if( basecall[basecall.length - 1] <= end ) {
			sampleDrawedBaseEnd = basecall.length - 1;
		}else {
			for(int m = sampleDrawedBaseStart; m < basecall.length - 1; m++) {
				if(basecall[m] <= end && end < basecall[m+1]) {
					sampleDrawedBaseEnd = m;
				}
			}
		}
	}
	
//==================== Reverse complement processing ===================
	
	@FXML
	protected void checkRevcomClick() {
		revcomFlag = checkRevcom.isSelected();
		this.EraseCanvas(gc1);
		this.DrawDotMap();
	}
	
//===================== For debug or etc. ==========================
	
	private void DrawAffine(Affine aff) {
		lb_mxx.setText(Double.toString((double)(Math.round(aff.getMxx() *1000))/1000 ));
		lb_mxy.setText(Double.toString((double)(Math.round(aff.getMxy() *1000))/1000 ));
		lb_tx.setText(Double.toString((double)(Math.round(aff.getTx() *1000))/1000 ));
		lb_myx.setText(Double.toString((double)(Math.round(aff.getMyx() *1000))/1000 ));
		lb_myy.setText(Double.toString((double)(Math.round(aff.getMyy() *1000))/1000 ));
		lb_ty.setText(Double.toString((double)(Math.round(aff.getTy() *1000))/1000 ));
	}
	
	
	@Override
	public void initialize(URL location, ResourceBundle rb) {
		gc1 = cv1.getGraphicsContext2D();
		gcSample = cvSample.getGraphicsContext2D();
		
		CanvasHeight = cv1.getHeight();
		CanvasWidth = cv1.getWidth();
		
		sampleCanvasHeight = (int)Math.floor(cvSample.getHeight());
		sampleCanvasWidth = (int)Math.floor(cvSample.getWidth());
		
		tfSampleZoom.setVisible(false);
		
		sliderSamplePosition.setVisible(false);
		
		sliderSampleScale.setValue(sampleDrawScale);
		sliderSampleScale.setMin(SAMPLE_SCALE_MIN);
		sliderSampleScale.setMax(SAMPLE_SCALE_MAX);
		sliderSampleScale.valueProperty().addListener((a, b, c) -> this.sliderSampleScaleSlide() );
		sliderSampleScale.setVisible(false);
		
		EraseCanvas(gc1);
		EraseCanvas(gcSample);
		
		taLog.appendText("Initialization finished."+CRLF);
		
		taReferenceSequence.setEditable(false);
		
		gcSample.setFont(new Font("Arial", 12));

	}
}
