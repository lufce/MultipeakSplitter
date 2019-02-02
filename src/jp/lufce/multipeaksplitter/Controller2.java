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

public class Controller2 implements Initializable {
	//TODO ReveseComplementにチェックを入れたときの描画とかマウスクリック時の処理を完成させる。


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

	//File I/O
	private File lastChosenFile = null;
	private FileChooser fileChooser = new FileChooser();
	//private File sampleFile;
	//private File referenceFile;

	private MultipeakDotplot dotplot;
	private MultipeakDotplot dotplotRevCom;
	private Ab1Sequence sample;
	private TextSequence refseq;

	final private int typeUndef = 0;
	final private int typeFasta = 1;
	final private int typeAb1   = 2;

	private SequenceMaster seqTop;
	private SequenceMaster seqLeft;
	private int topCutoff;
	private int leftCutoff;
	private int window;
	private int threshold;

	@FXML protected TabPane tabPane1;

	@FXML protected Button bTopFile;
	@FXML protected Button bLeftFile;
	@FXML protected Button bMakeDotplot;
	@FXML protected TextArea taTopPath;
	@FXML protected TextArea taLeftPath;
	@FXML protected TextArea taLog;

	@FXML protected Tab tabSequence;
	@FXML protected TextField tfTopCutoff;
	@FXML protected TextField tfLeftCutoff;
	@FXML protected TextField tfWindow;
	@FXML protected TextField tfThreshold;
	@FXML protected Button bRemake;
	@FXML protected Button bResetView;
	@FXML protected CheckBox checkRevcom;
	@FXML protected CheckBox checkMaximize;
	@FXML protected TextArea taSelectedForwardSequence;
	@FXML protected TextArea taSelectedReverseSequence;
	@FXML protected TextField tfForwardIntercept;
	@FXML protected TextField tfReverseIntercept;

	@FXML protected Canvas cvMap;
	private double cvMapHeight;
	private double cvMapWidth;
	private GraphicsContext gcMap;

	@FXML protected Canvas cvLeft;
	@FXML protected Canvas cvTop;
	@FXML protected Slider sliderTopPosition;
	@FXML protected Slider sliderTopScale;
	@FXML protected Slider sliderLeftPosition;
	@FXML protected Slider sliderLeftScale;
	@FXML protected TextField tfTopZoom;
	@FXML protected TextField tfLeftZoom;
	private int sliderTopValue;
	private GraphicsContext gcTop;
	private int cvTopHeight;
	private int cvTopWidth;
	private int topDrawedBaseStart = 0;
	private int topDrawedBaseEnd = 0;
	private int topWaveStart;
	private int topWaveEnd;

	//private int[][] multiIntensity;
	private int topDrawInterval = 1;
	private double topDrawScale = SAMPLE_SCALE_DEFAULT;

	private boolean[][] drawnDotmap;
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
	protected void bTopFileClick(ActionEvent e){
		String filePath = getFilePath();
		if(filePath != null) {
			taTopPath.setText(filePath);
		}
	}

	@FXML
	protected void bLeftFileClick(ActionEvent e) {
		String filePath = getFilePath();
		if(filePath != null) {
			taLeftPath.setText(filePath);
		}
	}

	private String getFilePath() {
		File initialDirectory = lastChosenFile != null ? new File(lastChosenFile.getParent()) : null;
		fileChooser.setInitialDirectory(initialDirectory);
		lastChosenFile = fileChooser.showOpenDialog(null);

		if(lastChosenFile != null) { return lastChosenFile.getAbsolutePath();}
		else {return null;}
	}

//======================== Making dotplot map ================================

	private void updateDotmapScreen(boolean highlighting) {
		//画面更新の一括処理を行う。

		this.EraseCanvas(gcMap);
		this.DrawDotMap();

		//ハイライトが行われているならそれも更新
		if(highlighting == true) {
			this.HighlightSelectedDotSequence();
		}
	}

	@FXML
	protected void bMakeDotplotClick(ActionEvent e) {
		//[Make Dotplot]ボタンを押したときの処理。
		//TODO setValueが例外を投げるようになったら、ちゃんと例外処理する。
		//TODO Refseq配列の表示方法を変える。

		taLog.appendText("start making dotplot..." + CRLF);

		try {

			//ab1配列オブジェクトとRefSeq配列オブジェクトを生成
			//sample = new Ab1Sequence(new File(taTopPath.getText()));
			//refseq = new TextSequence(new File(taLeftPath.getText()));

			//TODO SequenceMasterを使ってみる
			seqTop = new SequenceMaster(new File(taTopPath.getText()));
			seqLeft = new SequenceMaster(new File(taLeftPath.getText()));

			//値が適切かどうかを判断し、代入する。
			if(this.setValueOfCutoffWindow() == false) {
				taLog.appendText("setValue failed"+CRLF);
				return;
			}

			//それぞれのseqについてmapを生成する
			switch(seqTop.getDataType()) {
			case typeFasta:
				seqTop.textSeq.makeMap();break;
			case typeAb1:
				seqTop.ab1Seq.makeMap(topCutoff);break;
			default:
				taLog.appendText("the Datatype of top-sequence is invalid. Datatype:"+String.valueOf(seqTop.getDataType()));
			}

			switch(seqLeft.getDataType()) {
			case typeFasta:
				seqLeft.textSeq.makeMap();break;
			case typeAb1:
				seqLeft.ab1Seq.makeMap(leftCutoff);break;
			default:
				taLog.appendText("the Datatype of left-sequence is invalid. Datatype:"+String.valueOf(seqLeft.getDataType()));
			}

			//Dotplot配列を生成
			dotplot = new MultipeakDotplot(sample.getMultipeakMap(), refseq.getMap(), window, threshold);
			dotplotRevCom = new MultipeakDotplot(sample.getRevcomMultipeakMap(), refseq.getMap(), window, threshold);

			//Sliding Windowで見やすくしたDotplot配列を生成
			//windowedDotmap = multiDot.getWindowedDotPlot();
			//windowedRevcomDotmap = revcomMultiDot.getWindowedDotPlot();



			//Refseq配列をカラムに入れる。この表示方法は変えたほうがいいのではないか？
			this.setReferenceSequenceString();
			taLog.appendText("end making dotplot"+CRLF);

			//シークエンス解析の波形データの表示
			//multiIntensity = sample.getMultiAllIntensity();
			this.DrawSampleWave(0);

			//波形の拡大縮小用のスライダーの設定
			sliderTopPosition.setMax(Math.floor((sample.getTraceLength() - sliderTopPosition.getWidth()) / topDrawInterval));
			sliderTopPosition.valueProperty().addListener( (a, b, c) -> this.sliderTopPositionSlide(c.intValue()) );
			sliderTopPosition.setVisible(true);

			sliderTopScale.setVisible(true);

			tfTopZoom.setText(String.valueOf(topDrawScale));
			tfTopZoom.setVisible(true);

			//ファイル入出力のタブからシークエンス処理のタブに表示を切り替え
			tabPane1.getSelectionModel().select(tabSequence);

			//選択された配列の長さを表す変数の初期化（ここで必要？）
			forwardSequenceLength = 0;
			reverseSequenceLength = 0;

			//画面の更新
			this.updateDotmapScreen(false);

		}catch(Exception exception) {
			taLog.appendText(exception.getMessage());
			taLog.appendText(exception.getStackTrace().toString());
		}
	}

	@FXML
	protected void bRemakeClick(ActionEvent e) {
		//TODO setValueが例外を投げるようになったら、ちゃんと例外処理する。

		if (dotplot == null) {
			taLog.appendText("Remake failed" + CRLF);
		}else {
			taLog.appendText("start remaking dotplot"+CRLF);
			if(this.setValueOfCutoffWindow()==false) {
				taLog.appendText("setValue failed" + CRLF);
				return;
			}
			sample.makeMap(topCutoff);
			dotplot = new MultipeakDotplot(sample.getMultipeakMap(), refseq.getMap(), window, threshold);
			//windowedDotmap = multiDot.getWindowedDotPlot();
			this.EraseCanvas(gcMap);
			this.DrawDotMap();
			taLog.appendText("end remaking dotplot"+CRLF);
			this.DrawSampleWave(topWaveStart);
			tabPane1.getSelectionModel().select(tabSequence);
		}
	}

	@FXML
	protected void bResetViewClick() {
		aff = IDENTITY_TRANSFORM.clone();
		this.EraseCanvas(gcMap);
		this.DrawDotMap();
	}

	private boolean setValueOfCutoffWindow() {
		//TODO これも例外処理に変えないといけない

		boolean invalid = false;

		//まず記入されている値が数値かどうかチェック
		try {
			window = Integer.parseInt(tfWindow.getText());
		} catch(NumberFormatException e){
			taLog.appendText("window value is not number" + CRLF);
			invalid = true;
		}

		try {
			threshold = Integer.parseInt(tfThreshold.getText());
		} catch(NumberFormatException e){
			taLog.appendText("threshold value is not number" + CRLF);
			invalid = true;
		}

		try {
			topCutoff = Integer.parseInt(tfTopCutoff.getText());
		} catch(NumberFormatException e){
			taLog.appendText("top-cutoff value is not number" + CRLF);
			invalid = true;
		}

		try {
			leftCutoff = Integer.parseInt(tfLeftCutoff.getText());
		} catch(NumberFormatException e){
			taLog.appendText("left-cutoff value is not number" + CRLF);
			invalid = true;
		}

		if(invalid) {
			//数値かどうかのチェックが通らなければ、もう関数を終わる。
			return false;
		}

		//数値が適切な範囲内かチェック
		if(topCutoff < 0 || 100 <= topCutoff) {
			taLog.appendText("top-cutoff value is out of range" + CRLF);
			invalid = true;
		}

		if(leftCutoff < 0 || 100 <= leftCutoff) {
			taLog.appendText("left-cutoff value is out of range" + CRLF);
			invalid = true;
		}

		if( refseq.getSequenceLength() < window || sample.getSequenceLength() < window) {
			taLog.appendText("window size is beyond sequence length" + CRLF);
			invalid = true;
		}

		if(window < 0) {
			taLog.appendText("window value is < 0");
			invalid = true;
		}

		if(threshold > window) {
			taLog.appendText("threshold value exceeds window value");
			invalid = true;
		}

		if(invalid == false) {
			tfWindow.setText(String.valueOf(window));
			tfTopCutoff.setText(String.valueOf(topCutoff));
			tfLeftCutoff.setText(String.valueOf(leftCutoff));
			tfThreshold.setText(String.valueOf(threshold));

			return true;
		}else {
			return false;
		}
	}

	private void EraseCanvas(GraphicsContext gc) {

		if(this.checkRevcom.isSelected() && gc.equals(gcMap)) {
			gc.setFill(Color.BLACK);
		}else {
			gc.setFill(Color.WHITE);
		}

		gc.setTransform(IDENTITY_TRANSFORM);
		gc.fillRect(0, 0, cvMapWidth, cvMapHeight);
	}

	private void DrawDotMap() {
//		scaled_dotsize = aff.getMxx()*dotsize;

		gcMap.setLineWidth(GRID_LINE_WIDTH);
		gcMap.setTransform(aff);

		//Highlight sample wave range
		this.calculateRangeOfDrawedBase(sample.getBasecalls(), topWaveStart ,topWaveEnd);

		gcMap.setFill(Color.YELLOW);
		gcMap.fillRect(topDrawedBaseStart, 0, (topDrawedBaseEnd - topDrawedBaseStart + 1)*dotsize, refseq.getSequenceLength()*dotsize);


		//Draw dot as filled rectangles.
		if(this.checkRevcom.isSelected() && this.checkMaximize.isSelected()) {
			gcMap.setFill(Color.WHITE);
			gcMap.setStroke(Color.WHITE);
			drawnDotmap = this.dotplotRevCom.getMaxWindowedDotPlot();
		}else if(this.checkRevcom.isSelected() && !this.checkMaximize.isSelected()) {
			gcMap.setFill(Color.WHITE);
			gcMap.setStroke(Color.WHITE);
			drawnDotmap = this.dotplotRevCom.getWindowedDotPlot();
		}else if(!this.checkRevcom.isSelected() && this.checkMaximize.isSelected()) {
			gcMap.setFill(Color.BLACK);
			gcMap.setStroke(Color.BLACK);
			drawnDotmap = this.dotplot.getMaxWindowedDotPlot();
		}else if(!this.checkRevcom.isSelected() && !this.checkMaximize.isSelected()) {
			gcMap.setFill(Color.BLACK);
			gcMap.setStroke(Color.BLACK);
			drawnDotmap = this.dotplot.getWindowedDotPlot();
		}

		for(int m = 0; m < drawnDotmap.length; m++) {
			for(int n = 0; n < drawnDotmap[0].length; n++) {
				if(drawnDotmap[m][n]) {
					//gc.fillRect(m*scaled_dotsize, n*scaled_dotsize, scaled_dotsize, scaled_dotsize);
					gcMap.fillRect(m*dotsize, n*dotsize, dotsize, dotsize);
				}
			}
		}

		//Draw lines for grids.
		for(int m = 1; m < drawnDotmap.length ; m++) {
			gcMap.strokeLine(m*dotsize, 0, m*dotsize, drawnDotmap[0].length * dotsize);
		}

		for(int n = 1; n < drawnDotmap[0].length; n++) {
			gcMap.strokeLine(0, n*dotsize, drawnDotmap.length * dotsize, n*dotsize);
		}
	}

	private void HighlightSelectedDotSequence() {
		if(forwardSequenceLength > 0) {
			gcMap.setFill(Color.RED);

			for(int N = 0; N < forwardSequenceLength; N++) {
				gcMap.fillRect( (forwardStartPoint[0]-1 + N)*dotsize, (forwardStartPoint[1]-1 + N)*dotsize, dotsize, dotsize);
			}
		}
		if(reverseSequenceLength > 0) {
			gcMap.setFill(Color.RED);

			for(int N = 0; N < reverseSequenceLength; N++) {
				gcMap.fillRect( (reverseStartPoint[0]-1 - N)*dotsize, (reverseStartPoint[1]-1 + N)*dotsize, dotsize, dotsize);
			}
		}
	}

//======================= Mouse-action to move the dotmap position ===============================

	@FXML
	protected void cvMapMouseMoved(MouseEvent e) {
		MouseX = e.getX();
		MouseY = e.getY();
	}

	@FXML
	protected void cvMapMousePressed(MouseEvent e) {
		MousePreX = e.getX();
		MousePreY = e.getY();

		taLog.setText("x: "+ MousePreX + "  y: "+ MousePreY);
	}

	@FXML
	protected void cvMapMouseDragged(MouseEvent e) {
		MouseX = e.getX();
		MouseY = e.getY();

		MouseDeltaX = (MouseX-MousePreX) / aff.getMxx();
		MouseDeltaY = (MouseY-MousePreY) / aff.getMxx();

		MousePreX = MouseX;
		MousePreY = MouseY;

		this.EraseCanvas(gcMap);
		aff.appendTranslation(MouseDeltaX,MouseDeltaY);

		gcMap.setTransform(aff);
		this.DrawDotMap();
		this.HighlightSelectedDotSequence();

		drag = true;
	}

	@FXML
	protected void cvMapScroll(ScrollEvent e) {
	//TODO 四隅の余白部分が均等になるように縮小されるよう修正しないといけない。

		this.EraseCanvas(gcMap);
//		gc.setTransform(aff);
		scale = e.getDeltaY() >=0 ? 1.05 : 1/1.05;
		aff.appendScale(scale, scale, (int)Math.ceil((MouseX - aff.getTx() ) / (dotsize * aff.getMxx()) ), (int)Math.ceil((MouseX - aff.getTy() ) / (dotsize * aff.getMyy()) ));
//		aff.append(scale, 0, (1-scale)*MouseX, 0, scale, (1-scale)*MouseY);
		gcMap.setTransform(aff);

		this.DrawDotMap();
		this.HighlightSelectedDotSequence();

	}

//======================== For extraction of selected sequence =======================

	@FXML
	protected void cvMapMouseClicked(MouseEvent e) {
		if(drag == false) {

			double ClickX = e.getX();
			double ClickY = e.getY();

			DotX = (int)Math.ceil((ClickX - aff.getTx() ) / (dotsize * aff.getMxx()) );
			DotY = (int)Math.ceil((ClickY - aff.getTy() ) / (dotsize * aff.getMxx()) );

			if (DotX > 0 && DotY > 0) {
				this.SearchSequence();
				this.EraseCanvas(gcMap);
				gcMap.setTransform(aff);
				this.DrawDotMap();
				this.HighlightSelectedDotSequence();

				taSelectedForwardSequence.setText(refseq.getSeqSubstring(forwardStartPoint[1], forwardSequenceLength));
				taSelectedReverseSequence.setText(refseq.getSeqSubstring(reverseStartPoint[1], reverseSequenceLength));

				tfForwardIntercept.setText(Integer.toString(DotY - DotX));
				tfReverseIntercept.setText(Integer.toString(DotY + DotX));
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

		if(X >= 0 && X < drawnDotmap.length && Y >= 0 && Y < drawnDotmap[0].length && drawnDotmap[X][Y]) {
			while(X >= 0 && X < drawnDotmap.length && Y >= 0 && Y < drawnDotmap[0].length && drawnDotmap[X][Y] ) {
				forwardSequenceLength++;
				X--;
				Y--;
			}

			forwardStartPoint[0] = X + 2;
			forwardStartPoint[1] = Y + 2;

			X = DotX;
			Y = DotY;

			while(X >= 0 && X < drawnDotmap.length && Y >= 0 && Y < drawnDotmap[0].length && drawnDotmap[X][Y]) {
				forwardSequenceLength++;
				X++;
				Y++;
			}

			X = DotX - 1;
			Y = DotY - 1;

			while(X >= 0 && X < drawnDotmap.length && Y >= 0 && Y < drawnDotmap[0].length && drawnDotmap[X][Y]) {
				reverseSequenceLength++;
				X++;
				Y--;
			}

			reverseStartPoint[0] = X;
			reverseStartPoint[1] = Y + 2;

			X = DotX - 2;
			Y = DotY;

			while(X >= 0 && X < drawnDotmap.length && Y >= 0 && Y < drawnDotmap[0].length && drawnDotmap[X][Y]) {
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
		//taReferenceSequence.setText("");
		for(int n=0; n < refseq.getSequenceLength(); n++) {
		//	taReferenceSequence.appendText(refseq.getSeqString().charAt(n) + CRLF);
		}
	}

//======================== Sample wave viewer ===========================

	private void DrawSampleWave(int start) {
		//TODO RevComに対応させる

		topWaveStart = start;
		topWaveEnd = topWaveStart + cvTopWidth * topDrawInterval;

		//double[][] drawIntensity = this.getDrawIntensity(topWaveStart);
		double localMax = sample.getLocalMaxIntensity(topWaveStart, topWaveEnd);
		boolean[][] multiMap = sample.getMultipeakMap();
		int[] basecall = sample.getBasecalls();
		int pointer = topDrawedBaseStart;
		double[][] drawIntensity = this.convertDrawIntensity(sample.getSubarrayMultiAllIntensity(topWaveStart, topWaveEnd), localMax);

		this.EraseCanvas(gcTop);

		gcTop.setLineWidth(SAMPLE_WAVELINE_WIDTH);

		for(int m = 0; m < drawIntensity[0].length - 1; m++) {
			for(int n = 0; n < 4; n++) {
				gcTop.setStroke(BASE_COLOR[n]);
				gcTop.strokeLine(m, drawIntensity[n][m], m+1, drawIntensity[n][m+1]);
			}

			if(basecall[pointer] == m + topWaveStart) {
				for(int n = 0; n < 4; n++) {
					if(multiMap[n][pointer]) {
						gcTop.setFill(BASE_COLOR[n]);
						gcTop.fillText(BASE[n], m - 5, 110 + 10 * n);
					}
				}

				pointer++;
			}
		}
	}

	private double[][] convertDrawIntensity(int[][] intensity, double localMax){
		double[][] converted = new double[intensity.length][intensity[1].length];

		for(int m = 0; m < intensity[0].length; m++) {
			for(int n = 0; n < 4; n++) {
				converted[n][m] = (1 - intensity[n][m] / localMax * topDrawScale) * 100;
			}
		}

		return converted;
	}

//	private double getMaxIntensity(double[][] multi) {
//		double localMax = Double.MIN_VALUE;
//
//		for(int m = 0; m < multi[0].length; m++) {
//			for(int n = 0; n < 4; n++) {
//				if(multi[n][m] > localMax) {
//					localMax = multi[n][m];
//				}
//			}
//		}
//
//		return localMax;
//	}

//	private double[][] getDrawIntensity(int start) throws ArrayIndexOutOfBoundsException {
//
//		if(topWaveEnd > multiIntensity[0].length) {
//			System.out.println("out of bounds");
//			throw new ArrayIndexOutOfBoundsException();
//		}
//
//		double[][] drawIntensity = new double[4][cvTopWidth];
//
//		for(int m = start; m < start + cvTopWidth ; m++) {
//			for(int n = 0; n < 4; n++) {
//				drawIntensity[n][m-start] = multiIntensity[n][m * topDrawInterval];
//			}
//		}
//
//		return drawIntensity;
//	}

	private void sliderTopPositionSlide(int start) {
		sliderTopValue = (int)Math.round(sliderTopPosition.getValue());
//		sliderTopValue = start;
		this.EraseCanvas(gcMap);
		this.DrawDotMap();
		this.HighlightSelectedDotSequence();
		this.DrawSampleWave(sliderTopValue);
	}

	private void sliderTopScaleSlide() {
		topDrawScale = sliderTopScale.getValue();
		tfTopZoom.setText(String.format("%1$.1f", topDrawScale));
		this.DrawSampleWave(sliderTopValue);
	}

	@FXML
	protected void tfTopZoomKeyPressed(KeyEvent e) {
		double scale;

		if(e.getCode() == KeyCode.ENTER) {

			try {
				scale = Double.parseDouble(tfTopZoom.getText());

				if(SAMPLE_SCALE_MIN <= scale && scale <= SAMPLE_SCALE_MAX) {
					topDrawScale = scale;
					sliderTopScale.setValue(scale);
					this.DrawSampleWave(sliderTopValue);
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
			topDrawedBaseStart = 0;
		}else {
			for(int m = 1; m < basecall.length; m++) {
				if(basecall[m-1] < start && start <= basecall[m]) {
					topDrawedBaseStart = m;
				}
			}
		}

		if( basecall[basecall.length - 1] <= end ) {
			topDrawedBaseEnd = basecall.length - 1;
		}else {
			for(int m = topDrawedBaseStart; m < basecall.length - 1; m++) {
				if(basecall[m] <= end && end < basecall[m+1]) {
					topDrawedBaseEnd = m;
				}
			}
		}
	}

//==================== Reverse complement processing ===================

	@FXML
	protected void checkRevcomClick() {
		this.EraseCanvas(gcMap);
		this.DrawDotMap();
	}

//===================== For debug or etc. ==========================

	@Override
	public void initialize(URL location, ResourceBundle rb) {
		gcMap = cvMap.getGraphicsContext2D();
		gcTop = cvTop.getGraphicsContext2D();

		cvMapHeight = cvMap.getHeight();
		cvMapWidth = cvMap.getWidth();

		cvTopHeight = (int)Math.floor(cvTop.getHeight());
		cvTopWidth = (int)Math.floor(cvTop.getWidth());

		tfTopZoom.setVisible(false);

		sliderTopPosition.setVisible(false);

		sliderTopScale.setValue(topDrawScale);
		sliderTopScale.setMin(SAMPLE_SCALE_MIN);
		sliderTopScale.setMax(SAMPLE_SCALE_MAX);
		sliderTopScale.valueProperty().addListener((a, b, c) -> this.sliderTopScaleSlide() );
		sliderTopScale.setVisible(false);

		EraseCanvas(gcMap);
		EraseCanvas(gcTop);

		taLog.appendText("Initialization finished."+CRLF);

		//taReferenceSequence.setEditable(false);

		gcTop.setFont(new Font("Arial", 12));

	}
}
