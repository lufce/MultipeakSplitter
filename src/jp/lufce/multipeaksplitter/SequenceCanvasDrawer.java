package jp.lufce.multipeaksplitter;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SequenceCanvasDrawer extends CanvasDrawer{
	private SequenceMaster seq;
	private int drawableRange;
	private boolean isLeftCanvas = false;

	private int sequenceStartIndex = -1;
	private int sequenceEndIndex = -1;
	private int basecallStart;
//	private int basecallEnd;

	private int drawInterval = 1;
	private double drawScale = 1.0;

	final private Color[] BASE_COLOR = {Color.RED, Color.GREEN, Color.ORANGE, Color.BLACK};
	final private String[] BASE = {"A","C","G","T"};
	final private double WAVELINE_WIDTH = 0.5;
	final private int WAVE_DRAW_HEIGHT = 100;
	final private int TEXT_INTERVAL = 10;

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

	public int getTextInterval() {
		return this.TEXT_INTERVAL;
	}

	public int getDrawnStartIndex() {
		return this.sequenceStartIndex;
	}

	public int getDrawnEndIndex() {
		return this.sequenceEndIndex;
	}

	public int getSequenceLength() {
		return seq.getSequenceLength();
	}

	public int getDrawnSequenceLength() {
		return sequenceEndIndex - sequenceStartIndex + 1;
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
			drawSeqText(this.sequenceStartIndex);
			break;
		case SequenceMaster.typeAb1:
			drawSeqWaveAndText(this.basecallStart);
			break;
		}
	}

	private void drawSeqWaveAndText(int start) {

		int basecallStart = start;
		int basecallEnd = basecallStart + drawableRange * drawInterval;

		double localMax = seq.ab1Seq.getLocalMaxIntensity(basecallStart, basecallEnd);
		boolean[][] map = seq.ab1Seq.getMap();
		int[] basecall = seq.ab1Seq.getBasecalls();
		int pointer = this.searchSequenceStartIndex(basecall, start, sequenceStartIndex);
		double[][] drawIntensity = this.convertDrawIntensity(seq.ab1Seq.getSubarrayMultiAllIntensity(basecallStart, basecallEnd), localMax);

		gc.setLineWidth(WAVELINE_WIDTH);

		for(int m = 0; m < drawIntensity[0].length - 1; m++) {
			for(int n = 0; n < 4; n++) {
				gc.setStroke(BASE_COLOR[n]);

				if(isLeftCanvas) {
					gc.strokeLine(drawIntensity[n][m], m, drawIntensity[n][m+1], m+1);
				}else {
					gc.strokeLine(m, drawIntensity[n][m], m+1, drawIntensity[n][m+1]);
				}
			}

			if(basecallStart < basecall[basecall.length-1] && basecall[pointer] == m + basecallStart) {
				for(int n = 0; n < 4; n++) {
					if(map[n][pointer]) {
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
		sequenceEndIndex = pointer - 1;
	}

	private int searchSequenceStartIndex(int[] basecall, int start, int previousIndex) {
		//TODO 毎回0から探すのは無駄。

		if(previousIndex == -1) {
			//sequenceStartIndexが初期状態なら0から探し始める。
			for(int n = 0; n < basecall.length; n++) {
				if(basecall[n] > start) {
					sequenceStartIndex = n;
					break;
				}
			}
		}else {
			//sequenceStartIndexがすでに更新されているのなら、そこから探し始める。

			if(basecall[previousIndex] > start) {
				//前回のindexでのbasecallの値が、すでにstartを越えているなら、0に向かって探索。

				for(int n = previousIndex; n >= 0; n--) {
					if(basecall[n] < start) {
						sequenceStartIndex = n + 1;
						break;
					}
				}
			}else {
				//前回のindexからbasecallの最後に向かって探索

				for(int n = previousIndex; n < basecall.length; n++) {
					if(basecall[n] > start) {
						sequenceStartIndex = n;
						break;
					}
				}
			}
		}

		return sequenceStartIndex;
	}

	private void drawSeqText(int start) {
		boolean[][] map = seq.fastaSeq.getMap();
		sequenceStartIndex = start;
		sequenceEndIndex = start + drawableRange / TEXT_INTERVAL;

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
