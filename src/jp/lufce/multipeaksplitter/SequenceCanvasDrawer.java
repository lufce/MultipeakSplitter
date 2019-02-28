package jp.lufce.multipeaksplitter;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SequenceCanvasDrawer extends CanvasDrawer{
	private SequenceMaster seq;
	private SequenceMaster refseq = null;
	private int drawableRange;
	private boolean isLeftCanvas = false;
	private boolean forwardHideText = false;
	private boolean reverseHideText = false;

	private int sequenceDrawnStartIndex = -1;
	private int sequenceDrawnEndIndex = -1;
	private int basecallStart;
//	private int basecallEnd;

	private int[] clickedForwardSequenceStart = null;
	private int clickedForwardSequenceLength = 0;
	private int[] clickedReverseSequenceStart = null;
	private int clickedReverseSequenceLength = 0;

	private int drawInterval = 1;
	private double drawScale = 1.0;

	final private Color[] BASE_COLOR = {Color.RED, Color.GREEN, Color.ORANGE, Color.BLACK};
	final private String[] BASE = {"A","C","G","T"};
	final private double WAVELINE_WIDTH = 0.5;
	final private int WAVE_DRAW_HEIGHT = 100;
	final private int TEXT_INTERVAL = 10;
	final private int VIEW_OFFSET = 0;

	SequenceCanvasDrawer(GraphicsContext gc_arg, SequenceMaster seq_arg, boolean left){
		super(gc_arg);
		seq = seq_arg;
		isLeftCanvas = left;

		if(isLeftCanvas) {
			drawableRange = (int)gc.getCanvas().getHeight();
		}else {
			drawableRange = (int)gc.getCanvas().getWidth();
		}
	}

	public void setDrawScale(double scale) {
		drawScale = scale;
	}

	public void setForwardHideText(boolean boo) {
		this.forwardHideText = boo;
	}

	public void setReverseHideText(boolean boo) {
		this.reverseHideText = boo;
	}

	public void setRefseq(SequenceMaster seq) {
		this.refseq = seq;
	}

	public void setClickedForwardSequenceStart(int[] arg) {
		this.clickedForwardSequenceStart = arg;
	}

	public void setClickedForwardSequenceLength(int arg) {
		this.clickedForwardSequenceLength = arg;
	}

	public void setClickedReverseSequenceStart(int[] arg) {
		this.clickedReverseSequenceStart = arg;
	}

	public void setClickedReverseSequenceLength(int arg) {
		this.clickedReverseSequenceLength = arg;
	}

	public int getTextInterval() {
		return this.TEXT_INTERVAL;
	}

	public int getDrawnStartIndex() {
		return this.sequenceDrawnStartIndex;
	}

	public int getDrawnEndIndex() {
		return this.sequenceDrawnEndIndex;
	}

	public int getSequenceLength() {
		return seq.getSequenceLength();
	}

	public int getDrawnSequenceLength() {
		return sequenceDrawnEndIndex - sequenceDrawnStartIndex + 1;
	}

	public void updateSeqCanvas(int start) {

		super.clearCanvas();

		switch(seq.getDataType()) {
		case SequenceMaster.typeFasta:
			drawSeqText(start);
			break;
		case SequenceMaster.typeAb1:
			drawSeqWaveAndText(start);
			this.basecallStart = start;
			break;
		}
	}

	public void updateSeqCanvas() {

		super.clearCanvas();

		switch(seq.getDataType()) {
		case SequenceMaster.typeFasta:
			drawSeqText(this.sequenceDrawnStartIndex);
			break;
		case SequenceMaster.typeAb1:
			drawSeqWaveAndText(this.basecallStart);
			break;
		}
	}

	public int getDrawableNextSeqStart() {
		switch(seq.getDataType()) {
		case SequenceMaster.typeFasta:
			if(this.sequenceDrawnEndIndex + 1 <= seq.getSequenceLength()) {
				return this.sequenceDrawnStartIndex + 1;
			}
			break;
		case SequenceMaster.typeAb1:
			if(seq.ab1Seq.getWorkingBasecall()[this.sequenceDrawnStartIndex + 1] + drawableRange - VIEW_OFFSET <= seq.ab1Seq.getTraceEnd()) {
				return seq.ab1Seq.getWorkingBasecall()[this.sequenceDrawnStartIndex + 1] - VIEW_OFFSET;
			}
			break;
		}
		return -1;
	}

	public int getDrawablePreviousSeqStart() {
		switch(seq.getDataType()) {
		case SequenceMaster.typeFasta:
			if(this.sequenceDrawnStartIndex - 1 >= 0) {
				return this.sequenceDrawnStartIndex - 1;
			}
			break;
		case SequenceMaster.typeAb1:
			if(this.sequenceDrawnStartIndex - 1 - VIEW_OFFSET >= 0) {
				return seq.ab1Seq.getWorkingBasecall()[this.sequenceDrawnStartIndex - 1] - VIEW_OFFSET;
			}
			break;
		}
		return -1;
	}

	private void drawSeqWaveAndText(int start) {

		int waveStart = start;
		int waveEnd = waveStart + drawableRange * drawInterval;

		double localMax = seq.ab1Seq.getLocalMaxIntensity(waveStart, waveEnd);
		boolean[][] map = seq.ab1Seq.getWorkingMap();
		int[] basecall = seq.ab1Seq.getWorkingBasecall();
		int pointer = this.searchSequenceStartIndex(basecall, start, sequenceDrawnStartIndex);
		double[][] drawIntensity = this.convertDrawIntensity(seq.ab1Seq.getSubarrayMultiAllIntensity(waveStart, waveEnd), localMax);

		boolean[][] refseqMap = null;

		gc.setLineWidth(WAVELINE_WIDTH);

		if(forwardHideText == true || reverseHideText == true) {
			refseqMap = refseq.fastaSeq.getWorkingMap();
		}

		for(int m = 0; m < drawIntensity[0].length - 1; m++) {
			for(int n = 0; n < 4; n++) {
				gc.setStroke(BASE_COLOR[n]);

				if(isLeftCanvas) {
					gc.strokeLine(drawIntensity[n][m], m, drawIntensity[n][m+1], m+1);
				}else {
					gc.strokeLine(m, drawIntensity[n][m], m+1, drawIntensity[n][m+1]);
				}
			}

			//if(waveStart < basecall[basecall.length-1] && basecall[pointer] == m + waveStart) {

			if(pointer < basecall.length && basecall[pointer] == m + waveStart) {
				for(int n = 0; n < 4; n++) {
					if(map[n][pointer]) {

						//pointerやmapのインデックスは0スタート。ForwardSequenceStartなどは1スタート

						if(forwardHideText  &&
						clickedForwardSequenceStart != null && clickedForwardSequenceLength != 0 &&
						clickedForwardSequenceStart[0] - 1 <= pointer && pointer < clickedForwardSequenceStart[0] + clickedForwardSequenceLength - 1  &&
						refseqMap[n][pointer + clickedForwardSequenceStart[1] - clickedForwardSequenceStart[0]]) {
							continue;
						}

						if(reverseHideText  &&
						clickedReverseSequenceStart != null && clickedReverseSequenceLength != 0 &&
						clickedReverseSequenceStart[0] - clickedReverseSequenceLength <= pointer && pointer < clickedReverseSequenceStart[0] &&
						refseqMap[n][clickedReverseSequenceStart[1] + clickedReverseSequenceStart[0] - 2 - pointer]) {
							continue;
						}

						gc.setFill(BASE_COLOR[n]);

						if(isLeftCanvas) {
							gc.fillText(BASE[n], WAVE_DRAW_HEIGHT + TEXT_INTERVAL * (n+1)-5, m - 5);
						}else {
							gc.fillText(BASE[n], m - 5, WAVE_DRAW_HEIGHT + TEXT_INTERVAL * (n+1));
						}
					}
				}

				pointer++;
			}
		}

		//描画範囲の最後のインデックスを取得
		sequenceDrawnEndIndex = pointer - 1;
	}

	private int searchSequenceStartIndex(int[] basecall, int start, int previousIndex) {
		//TODO 毎回0から探すのは無駄。

		if(previousIndex == -1) {
			//sequenceStartIndexが初期状態なら0から探し始める。
			for(int n = 0; n < basecall.length; n++) {
				if(basecall[n] > start) {
					sequenceDrawnStartIndex = n;
					break;
				}
			}
		}else {
			//sequenceStartIndexがすでに更新されているのなら、そこから探し始める。

			if(basecall[previousIndex] > start) {
				//前回のindexでのbasecallの値が、すでにstartを越えているなら、0に向かって探索。

				for(int n = previousIndex; n >= 0; n--) {
					if(basecall[n] < start) {
						sequenceDrawnStartIndex = n + 1;
						break;
					}else if(basecall[n] == start) {
						sequenceDrawnStartIndex = n;
					}
				}
			}else {
				//前回のindexからbasecallの最後に向かって探索

				for(int n = previousIndex; n < basecall.length; n++) {
					if(basecall[n] >= start) {
						sequenceDrawnStartIndex = n;
						break;
					}
				}
			}
		}

		return sequenceDrawnStartIndex;
	}

	private void drawSeqText(int start) {
		boolean[][] map = seq.fastaSeq.getMap();
		sequenceDrawnStartIndex = start;
		sequenceDrawnEndIndex = start + drawableRange / TEXT_INTERVAL;

		for(int m = 0; m < drawableRange / TEXT_INTERVAL; m++) {
			for(int n = 0; n < 4; n++) {
				if(map[n][start + m]) {
					gc.setFill(BASE_COLOR[n]);

					if(isLeftCanvas) {
						gc.fillText(BASE[n], WAVE_DRAW_HEIGHT + TEXT_INTERVAL * n, (m+1) * TEXT_INTERVAL);
					}else {
						gc.fillText(BASE[n], (m+1) * TEXT_INTERVAL, WAVE_DRAW_HEIGHT + TEXT_INTERVAL * n);
					}
				}
			}
		}
	}

	private double[][] convertDrawIntensity(int[][] intensity, double localMax){
		double[][] converted = new double[intensity.length][intensity[1].length];

		for(int m = 0; m < intensity[0].length; m++) {
			for(int n = 0; n < 4; n++) {
				converted[n][m] = (1 - intensity[n][m] / localMax * drawScale) * WAVE_DRAW_HEIGHT;
			}
		}

		return converted;
	}
}
