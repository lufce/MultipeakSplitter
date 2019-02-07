package jp.lufce.multipeaksplitter;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SequenceCanvasDrawer extends CanvasDrawer{
	private SequenceMaster seq;
	private int drawableRange;
	private boolean isLeftCanvas = false;

	private int basecallStart;
	private int basecallEnd;

	private int drawInterval = 1;
	private int drawScale = 1;

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

	public void setDrawScale(int scale) {
		drawScale = scale;
	}

	public void drawSeqCanvas(int start) {
		//TODO RevComに対応させる
		//TODO seqCanvasを描画する入り口。ab1_seq, fasta_seq両方描画できるようにする。

		/*
		 * ab1_seqを受け取ったときと、fasta_seqを受け取ったときの挙動の違いは？
		 * →ab1、波形と文字の両方を描画
		 * →fasta 文字だけ描画
		 * topとleftの違いは？
		 * →topはそのまま描画。leftは左右反転させて反時計回りに90度回転
		 * 1. GraphicsContextsを引数として受け取るように変更。
		 * 2.
		 */

		super.clearCanvas();

		switch(seq.getDataType()) {
		case SequenceMaster.typeFasta:
			drawSeqText(start);
			break;
		case SequenceMaster.typeAb1:
			drawSeqWaveAndText(start);
			break;
		}
	}

	private void drawSeqWaveAndText(int start) {

		basecallStart = start;
		basecallEnd = basecallStart + drawableRange * drawInterval;

		double localMax = seq.ab1Seq.getLocalMaxIntensity(basecallStart, basecallEnd);
		boolean[][] map = seq.ab1Seq.getMap();
		int[] basecall = seq.ab1Seq.getBasecalls();
		int pointer = basecallStart;
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

			if(basecall[pointer] == m + basecallStart) {
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
	}

	private void drawSeqText(int start) {
		boolean[][] map = seq.fastaSeq.getMap();

		for(int m = start; m < drawableRange / TEXT_INTERVAL; m++) {
			for(int n = 0; n < 4; n++) {
				if(map[n][m]) {
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
