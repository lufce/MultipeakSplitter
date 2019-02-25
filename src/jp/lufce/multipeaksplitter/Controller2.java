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
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

public class Controller2 implements Initializable {
	//TODO ReveseComplementにチェックを入れたときの描画とかマウスクリック時の処理を完成させる。

	final private String CRLF = System.getProperty("line.separator");
	final private double WAVE_SCALE_MAX = 20;
	final private double WAVE_SCALE_MIN = 0.2;
	final private double WAVE_SCALE_DEFAULT = 1;

	private Alert alertDialog;

	//File I/O
	private File lastChosenFile = null;
	private FileChooser fileChooser = new FileChooser();

	private MultipeakDotplot[] dotplot = new MultipeakDotplot[4];
	/* Index   Top     Left
	 * 0:      -       -
	 * 1:      Revcom  -
	 * 2:      -       Revcom
	 * 3:      Revcom  Revcom
	 */

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

	@FXML protected Canvas cvDotplot;
	private GraphicsContext gcDotplot;
	private DotplotCanvasDrawer cvDotplotDrawer;

	@FXML protected Canvas cvLeft;
	@FXML protected Canvas cvTop;
	@FXML protected Slider sliderTopPosition;
	@FXML protected Slider sliderTopScale;
	@FXML protected Slider sliderLeftPosition;
	@FXML protected Slider sliderLeftScale;
	@FXML protected TextField tfTopZoom;
	@FXML protected TextField tfLeftZoom;
	private GraphicsContext gcTop;
	private GraphicsContext gcLeft;

	private int topDrawInterval = 1;
	private int leftDrawInterval = 1;
	private double topDrawScale = WAVE_SCALE_DEFAULT;
	private double leftDrawScale = WAVE_SCALE_DEFAULT;

	private double scale = 1.0;

	private boolean drag = false;

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
		cvDotplotDrawer.updateDotplot(cvTopDrawer, cvLeftDrawer);

		//ハイライトが行われているならそれも更新
		if(highlighting == true) {
			cvDotplotDrawer.highlightSelectedDotSequence();
		}
	}

	@FXML
	protected void bMakeDotplotClick(ActionEvent e) {
		//[Make Dotplot]ボタンを押したときの処理。
		//TODO setValueが例外を投げるようになったら、ちゃんと例外処理する。
		//TODO Refseq配列の表示方法を変える。

		taLog.appendText("start making dotplot..." + CRLF);

		try {

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

			cvTopDrawer.updateSeqCanvas(0);
			cvLeftDrawer.updateSeqCanvas(0);

			//Dotplot用描画クラスを生成
			cvDotplotDrawer = new DotplotCanvasDrawer(this.gcDotplot);
			cvDotplotDrawer.setDrawnDotplot(this.judgeDrawnDotplot());

			taLog.appendText("end making dotplot"+CRLF);

			//スライダーの設定
			this.initializeSliders();

			//ファイル入出力のタブからシークエンス処理のタブに表示を切り替え
			tabPane1.getSelectionModel().select(tabSequence);

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

			cvDotplotDrawer.updateDotplot(cvTopDrawer, cvLeftDrawer);

			taLog.appendText("end remaking dotplot"+CRLF);
			//this.drawSeqCanvas(topWaveStart,gcTop);
			tabPane1.getSelectionModel().select(tabSequence);
		}
	}

	private void makeMaps() {
		//それぞれのseqについてmapを生成する
		switch(seqTop.getDataType()) {
		case SequenceMaster.typeFasta:
			seqTop.fastaSeq.makeMap();
			break;
		case SequenceMaster.typeAb1:
			seqTop.ab1Seq.makeMap(topCutoff);
			break;
		default:
			taLog.appendText("The datatype of top-sequence is invalid. Datatype:"+String.valueOf(seqTop.getDataType()));
		}

		switch(seqLeft.getDataType()) {
		case SequenceMaster.typeFasta:
			seqLeft.fastaSeq.makeMap();
			break;
		case SequenceMaster.typeAb1:
			seqLeft.ab1Seq.makeMap(leftCutoff);
			break;
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
		cvDotplotDrawer.resetAffine();
		cvDotplotDrawer.updateDotplot(cvTopDrawer, cvTopDrawer);
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

//======================= Mouse-action to move the dotmap position ===============================

	@FXML
	protected void cvMapMouseMoved(MouseEvent e) {
		//ドラッグ操作には必要ないが、スクロール操作には必要。
		if(cvDotplotDrawer != null) {
			cvDotplotDrawer.setMouse(e.getX(), e.getY());
		}
	}

	@FXML
	protected void cvMapMousePressed(MouseEvent e) {
		if(cvDotplotDrawer != null) {
			cvDotplotDrawer.setMousePre(e.getX(), e.getY());
		}
	}

	@FXML
	protected void cvMapMouseDragged(MouseEvent e) {
		if(cvDotplotDrawer != null) {
			cvDotplotDrawer.mouseDragged(e.getX(), e.getY());
			cvDotplotDrawer.updateDotplot(cvTopDrawer, cvLeftDrawer);
			cvDotplotDrawer.highlightSelectedDotSequence();

			drag = true;
		}
	}

	@FXML
	protected void cvMapScroll(ScrollEvent e) {
	//TODO 四隅の余白部分が均等になるように縮小されるよう修正しないといけない。
		if(cvDotplotDrawer != null) {
			cvDotplotDrawer.mouseScroll(e.getDeltaY());
			cvDotplotDrawer.updateDotplot(cvTopDrawer, cvLeftDrawer);
			cvDotplotDrawer.highlightSelectedDotSequence();
		}
	}

//======================== For extraction of selected sequence =======================

	@FXML
	protected void cvMapMouseClicked(MouseEvent e) {
		String fwSeq = "", rvSeq = "";
		int fwStart = 0, fwLength =0, rvStart = 0, rvLength = 0;

		if(cvDotplotDrawer != null) {
			if(drag == false) {

				if ( cvDotplotDrawer.clickDot(e.getX(), e.getY()) ) {

					fwStart  = cvDotplotDrawer.getForwardStart();
					fwLength = cvDotplotDrawer.getForwardSequenceLength();
					rvStart  = cvDotplotDrawer.getReverseStart();
					rvLength = cvDotplotDrawer.getReverseSequenceLength();

					cvDotplotDrawer.updateDotplot(cvTopDrawer, cvLeftDrawer);
					cvDotplotDrawer.highlightSelectedDotSequence();

					if(seqTop.getDataType() == SequenceMaster.typeFasta) {
						fwSeq = seqTop.fastaSeq.getSeqSubstring(fwStart, fwLength);
						rvSeq = seqTop.fastaSeq.getSeqSubstring(rvStart, rvLength);
					}else if(seqLeft.getDataType() == SequenceMaster.typeFasta) {
						fwSeq = seqLeft.fastaSeq.getSeqSubstring(fwStart, fwLength);
						rvSeq = seqLeft.fastaSeq.getSeqSubstring(rvStart, rvLength);
					}

					taSelectedForwardSequence.setText(fwSeq.toUpperCase());
					taSelectedReverseSequence.setText(rvSeq.toUpperCase());

					tfForwardIntercept.setText(Integer.toString(cvDotplotDrawer.getForwardInterception()));
					tfReverseIntercept.setText(Integer.toString(cvDotplotDrawer.getReverseInterception()));
				}
			}else {
				drag = false;
			}
		}
	}

//======================= Slider ==================================
	private void initializeSliders() {

		//波形の位置変更のスライダーの設定
		initializePositionSlider(sliderTopPosition, seqTop, topDrawInterval, cvTopDrawer);
		initializePositionSlider(sliderLeftPosition, seqLeft, leftDrawInterval, cvLeftDrawer);

		//波形倍率変更スライドの設定
		sliderTopScale.setVisible(true);
		sliderLeftScale.setVisible(true);

		//波形倍率テキストボックスの設定
		tfTopZoom.setText(String.valueOf(topDrawScale));
		tfTopZoom.setVisible(true);

		tfLeftZoom.setText(String.valueOf(leftDrawScale));
		tfLeftZoom.setVisible(true);

	}

	private void initializePositionSlider(Slider sld, SequenceMaster seq, int interval, SequenceCanvasDrawer scd) {

		sld.setMax(getSliderMaxValue(seq, sld, interval, scd));
		sld.valueProperty().addListener( (a, b, c) -> this.slidePosition(sld, scd) );
		sld.setVisible(true);
	}

	private double getSliderMaxValue(SequenceMaster seq, Slider sld, int interval, SequenceCanvasDrawer scd) {
		double max = 0;

		switch( seq.getDataType() ) {
		case SequenceMaster.typeAb1:
			max = Math.floor( (seq.ab1Seq.getBasecalls()[seq.ab1Seq.getSequenceLength()-1] - sld.getWidth()) / interval);
			break;
		case SequenceMaster.typeFasta:
			max = Math.floor( (seq.getSequenceLength() - sld.getWidth() * scd.getTextInterval() ) / interval);
			break;
		}

		return max;
	}

	private void slidePosition(Slider sld, SequenceCanvasDrawer scd) {
		scd.updateSeqCanvas( (int)Math.round(sld.getValue()) );

//		this.clearCanvas(gcDotplot);
//		this.drawDotplot();
		cvDotplotDrawer.updateDotplot(cvTopDrawer, cvLeftDrawer);
		cvDotplotDrawer.highlightSelectedDotSequence();

	}

	private void slideScale(Slider sld, TextField tf, SequenceCanvasDrawer cvd) {
		double scale = sld.getValue();
		tf.setText(String.format("%1$.1f", scale));
		cvd.setDrawScale(scale);
		cvd.updateSeqCanvas();
	}

	@FXML
	protected void tfTopZoomKeyPressed(KeyEvent e) {
		if(e.getCode() == KeyCode.ENTER) {
			this.tfZoom(tfTopZoom, sliderTopScale, cvTopDrawer);
		}
	}

	@FXML
	protected void tfLeftZoomKeyPressed(KeyEvent e) {
		if(e.getCode() == KeyCode.ENTER) {
			this.tfZoom(tfLeftZoom, sliderLeftScale, cvLeftDrawer);
		}
	}

	private void tfZoom(TextField tf, Slider sld, SequenceCanvasDrawer scd) {
		try {
			scale = Double.parseDouble(tf.getText());

			if(WAVE_SCALE_MIN <= scale && scale <= WAVE_SCALE_MAX) {
				sld.setValue(scale);
				scd.setDrawScale(scale);
				scd.updateSeqCanvas();
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

//==================== Reverse complement processing ===================

	@FXML
	protected void checkRevcomClick() {
//		this.clearCanvas(gcDotplot);
//		this.drawDotplot();
		cvDotplotDrawer.updateDotplot(cvTopDrawer, cvLeftDrawer);
	}

//===================== For debug or etc. ==========================

	@Override
	public void initialize(URL location, ResourceBundle rb) {
		gcDotplot  = cvDotplot.getGraphicsContext2D();
		gcTop  = cvTop.getGraphicsContext2D();
		gcLeft = cvLeft.getGraphicsContext2D();

		tfTopZoom.setVisible(false);
		tfLeftZoom.setVisible(false);

		sliderTopPosition.setVisible(false);
		sliderLeftPosition.setVisible(false);

		sliderTopScale.setValue(topDrawScale);
		sliderTopScale.setMin(WAVE_SCALE_MIN);
		sliderTopScale.setMax(WAVE_SCALE_MAX);
		sliderTopScale.valueProperty().addListener((a, b, c) -> this.slideScale(sliderTopScale, tfTopZoom, cvTopDrawer));
		sliderTopScale.setVisible(false);

		sliderLeftScale.setValue(leftDrawScale);
		sliderLeftScale.setMin(WAVE_SCALE_MIN);
		sliderLeftScale.setMax(WAVE_SCALE_MAX);
		sliderLeftScale.valueProperty().addListener((a, b, c) -> this.slideScale(sliderLeftScale, tfLeftZoom, cvLeftDrawer));
		sliderLeftScale.setVisible(false);

		taLog.appendText("Initialization finished."+CRLF);

		gcTop.setFont(new Font("Arial", 12));

	}
}
