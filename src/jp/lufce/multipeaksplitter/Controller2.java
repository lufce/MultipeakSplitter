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
	final private double WAVE_SCALE_MAX = 20;
	final private double WAVE_SCALE_MIN = 0.2;
	final private double WAVE_SCALE_DEFAULT = 1;
//	final private double WAVELINE_WIDTH = 0.5;
//	final private Color[] BASE_COLOR = {Color.RED, Color.GREEN, Color.ORANGE, Color.BLACK};
//	final private String[] BASE = {"A","C","G","T"};
	// The index 0 to 3 associates with A, C, G, and T, respectively.

	private Alert alertDialog;

	//File I/O
	private File lastChosenFile = null;
	private FileChooser fileChooser = new FileChooser();
	//private File sampleFile;
	//private File referenceFile;

	private MultipeakDotplot[] dotplot = new MultipeakDotplot[4];
	/* Index   Top     Left
	 * 0:      -       -
	 * 1:      Revcom  -
	 * 2:      -       Revcom
	 * 3:      Revcom  Revcom
	 */

	private MultipeakDotplot dotplotRevCom;
	private Ab1Sequence sample;		//あとで消す
	private FastaSequence refseq;	//あとで消す

	private SequenceMaster seqTop;
	private SequenceMaster seqLeft;

	private SequenceCanvasDrawer cvTopDrawer;
	private SequenceCanvasDrawer cvLeftDrawer;
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
	@FXML protected CheckBox checkTopRevcom;
	@FXML protected CheckBox checkLeftRevcom;
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
	private int sliderLeftValue;
	private GraphicsContext gcTop;
	private int cvTopHeight;
	private int cvTopWidth;
	private int topDrawedBaseStart = 0;
	private int topDrawedBaseEnd = 0;
	private int topWaveStart;
	private int topWaveEnd;

	private GraphicsContext gcLeft;
	private int cvLeftHeight;
	private int cvLeftWidth;
	private int leftDrawedBaseStart = 0;
	private int leftDrawedBaseEnd = 0;
	private int leftWaveStart;
	private int leftWaveEnd;

	//private int[][] multiIntensity;
	private int topDrawInterval = 1;
	private double topDrawScale = WAVE_SCALE_DEFAULT;
	private double leftDrawScale = WAVE_SCALE_DEFAULT;

	private boolean[][] drawnDotplot;
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

		this.clearCanvas(gcMap);
		this.drawDotplot();

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
			seqTop  = new SequenceMaster(new File(taTopPath.getText()));
			seqLeft = new SequenceMaster(new File(taLeftPath.getText()));

			//値が適切かどうかを判断し、代入する。
			if(this.setValueOfCutoffWindow() == false) {
				taLog.appendText("setValue failed"+CRLF);
				return;
			}

			//SequenceMasterの初期化を行って、逆相補鎖同士などを含めたDotplotを作る。
			makeMaps();
			makeDotplots();

			//Sequence用描画クラスを生成して、最初部分から描画する
			cvTopDrawer = new SequenceCanvasDrawer(gcTop, seqTop, false);
			cvLeftDrawer = new SequenceCanvasDrawer(gcLeft, seqLeft, true);

			cvTopDrawer.drawSeqCanvas(0);
			cvLeftDrawer.drawSeqCanvas(0);

			taLog.appendText("end making dotplot"+CRLF);

			/*
			//波形の拡大縮小用のスライダーの設定
			sliderTopPosition.setMax(Math.floor((seqTop.ab1Seq.getTraceLength() - sliderTopPosition.getWidth()) / topDrawInterval));
			sliderTopPosition.valueProperty().addListener( (a, b, c) -> this.sliderTopPositionSlide(c.intValue()) );
			sliderTopPosition.setVisible(true);

			sliderTopScale.setVisible(true);

			tfTopZoom.setText(String.valueOf(topDrawScale));
			tfTopZoom.setVisible(true);

			sliderLeftPosition.setMax(Math.floor((seqTop.ab1Seq.getTraceLength() - sliderLeftPosition.getWidth()) / topDrawInterval));
			sliderLeftPosition.valueProperty().addListener( (a, b, c) -> this.sliderLeftPositionSlide(c.intValue()) );
			sliderLeftPosition.setVisible(true);

			sliderLeftScale.setVisible(true);

			tfLeftZoom.setText(String.valueOf(topDrawScale));
			tfLeftZoom.setVisible(true);

			*/

			//ファイル入出力のタブからシークエンス処理のタブに表示を切り替え
			tabPane1.getSelectionModel().select(tabSequence);

			//選択された配列の長さを表す変数の初期化（ここで必要？）
			forwardSequenceLength = 0;
			reverseSequenceLength = 0;

			taLog.appendText("make dotplot finished"+CRLF);

			//画面の更新
			this.updateDotmapScreen(false);

		}catch(Exception exception) {
//			taLog.appendText(exception.getMessage());
//			taLog.appendText(exception.getStackTrace().toString());
			java.lang.StackTraceElement[] stack = exception.getStackTrace();
			System.out.println(exception.getMessage());
			for (int i=0; i<stack.length; i++) {
				System.out.println(stack[i]);
			}

		}
	}

	@FXML
	protected void bRemakeClick(ActionEvent e) {
		//TODO setValueが例外を投げるようになったら、ちゃんと例外処理する。
		//Remakeボタンはファイルの再読込はしない。

		if (dotplot == null) {
			taLog.appendText("Remake failed" + CRLF);
		}else {
			taLog.appendText("start remaking dotplot"+CRLF);
			if(this.setValueOfCutoffWindow()==false) {
				taLog.appendText("setValue failed" + CRLF);
				return;
			}
			makeMaps();
			makeDotplots();
			//windowedDotmap = multiDot.getWindowedDotPlot();
			this.clearCanvas(gcMap);
			this.drawDotplot();
			taLog.appendText("end remaking dotplot"+CRLF);
			//this.drawSeqCanvas(topWaveStart,gcTop);
			tabPane1.getSelectionModel().select(tabSequence);
		}
	}

	private void makeMaps() {
		//それぞれのseqについてmapを生成する
		switch(seqTop.getDataType()) {
		case SequenceMaster.typeFasta:
			seqTop.fastaSeq.makeMap();break;
		case SequenceMaster.typeAb1:
			seqTop.ab1Seq.makeMap(topCutoff);break;
		default:
			taLog.appendText("The datatype of top-sequence is invalid. Datatype:"+String.valueOf(seqTop.getDataType()));
		}

		switch(seqLeft.getDataType()) {
		case SequenceMaster.typeFasta:
			seqLeft.fastaSeq.makeMap();break;
		case SequenceMaster.typeAb1:
			seqLeft.ab1Seq.makeMap(leftCutoff);break;
		default:
			taLog.appendText("The datatype of left-sequence is invalid. Datatype:"+String.valueOf(seqLeft.getDataType()));
		}

		return;
	}

	private void makeDotplots() {
		//Dotplot配列を生成
		//TODO ここでそれぞれの配列の逆相補鎖を含めた４種類のdotplotを作る？
		dotplot[0] = new MultipeakDotplot(seqTop.getMap(),       seqLeft.getMap(),       window, threshold);
		dotplot[1] = new MultipeakDotplot(seqTop.getRevcomMap(), seqLeft.getMap(),       window, threshold);
		dotplot[2] = new MultipeakDotplot(seqTop.getMap(),       seqLeft.getRevcomMap(), window, threshold);
		dotplot[3] = new MultipeakDotplot(seqTop.getRevcomMap(), seqLeft.getRevcomMap(), window, threshold);
	}

	@FXML
	protected void bResetViewClick() {
		aff = IDENTITY_TRANSFORM.clone();
		this.clearCanvas(gcMap);
		this.drawDotplot();
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

		if( seqLeft.getSequenceLength() < window || seqTop.getSequenceLength() < window) {
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

	@Deprecated
	private void clearCanvas(GraphicsContext gc) {
		//消す予定
		gc.setFill(Color.WHITE);

//		Revcomのときに色を変えるかどうか。
//		if(this.checkRevcom.isSelected() && gc.equals(gcMap)) {
//			gc.setFill(Color.BLACK);
//		}else {
//			gc.setFill(Color.WHITE);
//		}

		gc.setTransform(IDENTITY_TRANSFORM);
		gc.fillRect(0, 0, cvMapWidth, cvMapHeight);
	}

	private void drawDotplot() {

		gcMap.setLineWidth(GRID_LINE_WIDTH);
		gcMap.setTransform(aff);

		//seqTopのCanvasで表示されている部分に当たるMap部分を黄色でハイライトする。
		//TODO seqLeftに対してもこの操作を行わないといけない。
		//TODO seqがAb1形式じゃない場合どうする？
		this.calculateRangeOfDrawedBase(seqTop.ab1Seq.getBasecalls(), topWaveStart ,topWaveEnd);
		gcMap.setFill(Color.YELLOW);
		gcMap.fillRect(topDrawedBaseStart, 0, (topDrawedBaseEnd - topDrawedBaseStart + 1)*dotsize, seqLeft.getSequenceLength()*dotsize);


		//ドットを描画していく
		gcMap.setFill(Color.BLACK);
		gcMap.setStroke(Color.BLACK);
		drawnDotplot = this.judgeDrawnDotplot();



//		とりあえずRevcomのときに黒背景にするのはやめる。
//		//Draw dot as filled rectangles.
//		if(this.checkRevcom.isSelected() && this.checkMaximize.isSelected()) {
//			gcMap.setFill(Color.WHITE);
//			gcMap.setStroke(Color.WHITE);
//			drawnDotmap = this.dotplotRevCom.getMaxWindowedDotPlot();
//		}else if(this.checkRevcom.isSelected() && !this.checkMaximize.isSelected()) {
//			gcMap.setFill(Color.WHITE);
//			gcMap.setStroke(Color.WHITE);
//			drawnDotmap = this.dotplotRevCom.getWindowedDotPlot();
//		}else if(!this.checkRevcom.isSelected() && this.checkMaximize.isSelected()) {
//			gcMap.setFill(Color.BLACK);
//			gcMap.setStroke(Color.BLACK);
//			drawnDotmap = this.dotplot.getMaxWindowedDotPlot();
//		}else if(!this.checkRevcom.isSelected() && !this.checkMaximize.isSelected()) {
//			gcMap.setFill(Color.BLACK);
//			gcMap.setStroke(Color.BLACK);
//			drawnDotmap = this.dotplot.getWindowedDotPlot();
//		}

		for(int m = 0; m < drawnDotplot.length; m++) {
			for(int n = 0; n < drawnDotplot[0].length; n++) {
				if(drawnDotplot[m][n]) {
					//gc.fillRect(m*scaled_dotsize, n*scaled_dotsize, scaled_dotsize, scaled_dotsize);
					gcMap.fillRect(m*dotsize, n*dotsize, dotsize, dotsize);
				}
			}
		}

		//Draw lines for grids.
		for(int m = 1; m < drawnDotplot.length ; m++) {
			gcMap.strokeLine(m*dotsize, 0, m*dotsize, drawnDotplot[0].length * dotsize);
		}

		for(int n = 1; n < drawnDotplot[0].length; n++) {
			gcMap.strokeLine(0, n*dotsize, drawnDotplot.length * dotsize, n*dotsize);
		}
	}

	private boolean[][] judgeDrawnDotplot(){
		MultipeakDotplot selectedDotplot = null;

		if      ( !this.checkTopRevcom.isSelected() && !this.checkTopRevcom.isSelected()) {
			selectedDotplot = dotplot[0];
		}else if(  this.checkTopRevcom.isSelected() && !this.checkTopRevcom.isSelected()) {
			selectedDotplot = dotplot[1];
		}else if( !this.checkTopRevcom.isSelected() &&  this.checkTopRevcom.isSelected()) {
			selectedDotplot = dotplot[2];
		}else if(  this.checkTopRevcom.isSelected() &&  this.checkTopRevcom.isSelected()) {
			selectedDotplot = dotplot[3];
		}

		if(this.checkMaximize.isSelected()) {
			return selectedDotplot.getMaxWindowedDotPlot();
		}else {
			return selectedDotplot.getWindowedDotPlot();
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

		this.clearCanvas(gcMap);
		aff.appendTranslation(MouseDeltaX,MouseDeltaY);

		gcMap.setTransform(aff);
		this.drawDotplot();
		this.HighlightSelectedDotSequence();

		drag = true;
	}

	@FXML
	protected void cvMapScroll(ScrollEvent e) {
	//TODO 四隅の余白部分が均等になるように縮小されるよう修正しないといけない。

		this.clearCanvas(gcMap);
//		gc.setTransform(aff);
		scale = e.getDeltaY() >=0 ? 1.05 : 1/1.05;
		aff.appendScale(scale, scale, (int)Math.ceil((MouseX - aff.getTx() ) / (dotsize * aff.getMxx()) ), (int)Math.ceil((MouseX - aff.getTy() ) / (dotsize * aff.getMyy()) ));
//		aff.append(scale, 0, (1-scale)*MouseX, 0, scale, (1-scale)*MouseY);
		gcMap.setTransform(aff);

		this.drawDotplot();
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
				this.clearCanvas(gcMap);
				gcMap.setTransform(aff);
				this.drawDotplot();
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

		if(X >= 0 && X < drawnDotplot.length && Y >= 0 && Y < drawnDotplot[0].length && drawnDotplot[X][Y]) {
			while(X >= 0 && X < drawnDotplot.length && Y >= 0 && Y < drawnDotplot[0].length && drawnDotplot[X][Y] ) {
				forwardSequenceLength++;
				X--;
				Y--;
			}

			forwardStartPoint[0] = X + 2;
			forwardStartPoint[1] = Y + 2;

			X = DotX;
			Y = DotY;

			while(X >= 0 && X < drawnDotplot.length && Y >= 0 && Y < drawnDotplot[0].length && drawnDotplot[X][Y]) {
				forwardSequenceLength++;
				X++;
				Y++;
			}

			X = DotX - 1;
			Y = DotY - 1;

			while(X >= 0 && X < drawnDotplot.length && Y >= 0 && Y < drawnDotplot[0].length && drawnDotplot[X][Y]) {
				reverseSequenceLength++;
				X++;
				Y--;
			}

			reverseStartPoint[0] = X;
			reverseStartPoint[1] = Y + 2;

			X = DotX - 2;
			Y = DotY;

			while(X >= 0 && X < drawnDotplot.length && Y >= 0 && Y < drawnDotplot[0].length && drawnDotplot[X][Y]) {
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



	private void sliderTopPositionSlide(int start) {
		sliderTopValue = (int)Math.round(sliderTopPosition.getValue());
//		sliderTopValue = start;
		this.clearCanvas(gcMap);
		this.drawDotplot();
		this.HighlightSelectedDotSequence();
		cvTopDrawer.drawSeqCanvas(sliderTopValue);
	}

	private void sliderLeftPositionSlide(int start) {
		sliderLeftValue = (int)Math.round(sliderLeftPosition.getValue());
//		sliderLeftValue = start;
		this.clearCanvas(gcMap);
		this.drawDotplot();
		this.HighlightSelectedDotSequence();
		cvLeftDrawer.drawSeqCanvas(sliderLeftValue);
	}

	private void sliderTopScaleSlide() {
		topDrawScale = sliderTopScale.getValue();
		tfTopZoom.setText(String.format("%1$.1f", topDrawScale));
		this.drawSeqCanvas(sliderTopValue, gcTop);
	}


	//TODO TopとLeftの操作はまとめたほうが良いと思う。
	@FXML
	protected void tfTopZoomKeyPressed(KeyEvent e) {
		double scale;

		if(e.getCode() == KeyCode.ENTER) {

			try {
				scale = Double.parseDouble(tfTopZoom.getText());

				if(WAVE_SCALE_MIN <= scale && scale <= WAVE_SCALE_MAX) {
					topDrawScale = scale;
					sliderTopScale.setValue(scale);
					this.drawSeqCanvas(sliderTopValue, gcTop);
				}else {
					alertDialog = new Alert(AlertType.INFORMATION);
					alertDialog.setTitle("Zoom value is out of range.");
					alertDialog.setHeaderText(null);
					alertDialog.setContentText("Zoom value should be between " + WAVE_SCALE_MIN + " to " + WAVE_SCALE_MAX + ".");
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

	@FXML
	protected void tfLeftZoomKeyPressed(KeyEvent e) {
		double scale;

		if(e.getCode() == KeyCode.ENTER) {

			try {
				scale = Double.parseDouble(tfLeftZoom.getText());

				if(WAVE_SCALE_MIN <= scale && scale <= WAVE_SCALE_MAX) {
					leftDrawScale = scale;
					sliderLeftScale.setValue(scale);
					this.drawSeqCanvas(sliderTopValue, gcLeft);
				}else {
					alertDialog = new Alert(AlertType.INFORMATION);
					alertDialog.setTitle("Zoom value is out of range.");
					alertDialog.setHeaderText(null);
					alertDialog.setContentText("Zoom value should be between " + WAVE_SCALE_MIN + " to " + WAVE_SCALE_MAX + ".");
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

		if(seqTop.getDataType() == SequenceMaster.typeAb1) {
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
		this.clearCanvas(gcMap);
		this.drawDotplot();
	}

//===================== For debug or etc. ==========================

	@Override
	public void initialize(URL location, ResourceBundle rb) {
		gcMap  = cvMap.getGraphicsContext2D();
		gcTop  = cvTop.getGraphicsContext2D();
		gcLeft = cvLeft.getGraphicsContext2D();

		cvMapHeight = cvMap.getHeight();
		cvMapWidth = cvMap.getWidth();

		cvTopHeight = (int)Math.floor(cvTop.getHeight());
		cvTopWidth = (int)Math.floor(cvTop.getWidth());
		cvLeftHeight = (int)Math.floor(cvLeft.getHeight());
		cvLeftWidth = (int)Math.floor(cvLeft.getWidth());

		tfTopZoom.setVisible(false);
		tfLeftZoom.setVisible(false);

		sliderTopPosition.setVisible(false);
		sliderLeftPosition.setVisible(false);

		sliderTopScale.setValue(topDrawScale);
		sliderTopScale.setMin(WAVE_SCALE_MIN);
		sliderTopScale.setMax(WAVE_SCALE_MAX);
		sliderTopScale.valueProperty().addListener((a, b, c) -> this.sliderTopScaleSlide());
		sliderTopScale.setVisible(false);

		sliderLeftScale.setValue(leftDrawScale);
		sliderLeftScale.setMin(WAVE_SCALE_MIN);
		sliderLeftScale.setMax(WAVE_SCALE_MAX);
		sliderLeftScale.valueProperty().addListener((a, b, c) -> this.sliderTopScaleSlide());
		sliderLeftScale.setVisible(false);

		clearCanvas(gcMap);
		clearCanvas(gcTop);
		clearCanvas(gcLeft);

		taLog.appendText("Initialization finished."+CRLF);

		//taReferenceSequence.setEditable(false);

		gcTop.setFont(new Font("Arial", 12));

	}
}
