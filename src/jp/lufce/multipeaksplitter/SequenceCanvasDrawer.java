package jp.lufce.multipeaksplitter;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SequenceCanvasDrawer {
	private GraphicsContext gc;
	private String id;
	private boolean needTransform = false;

	private int basecallStart;
	private int basecallEnd;
	private int drawableRange;


	final private Color[] BASE_COLOR = {Color.RED, Color.GREEN, Color.ORANGE, Color.BLACK};
	final private String[] BASE = {"A","C","G","T"};
	final private double WAVELINE_WIDTH = 0.5;

	SequenceCanvasDrawer(GraphicsContext gc_arg, boolean transform){
		gc = gc_arg;
		id = gc.getCanvas().getId();
		needTransform = transform;

		if (needTransform) {
			drawableRange = (int)gc.getCanvas().getHeight();
		}else {
			drawableRange = (int)gc.getCanvas().getWidth();
		}
	}

	private void drawSeqCanvas(int start, int[] basecall) {
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

		basecallStart = start;
		basecallEnd = basecallStart + cvTopWidth * drawableRange;

		//double[][] drawIntensity = this.getDrawIntensity(topWaveStart);
		double localMax = sample.getLocalMaxIntensity(basecallStart, basecallEnd);
		boolean[][] multiMap = sample.getMap();
		int[] basecall = sample.getBasecalls();
		int pointer = topDrawedBaseStart;
		double[][] drawIntensity = this.convertDrawIntensity(sample.getSubarrayMultiAllIntensity(basecallStart, basecallEnd), localMax);

		this.clearCanvas(gcTop);

		gcTop.setLineWidth(WAVELINE_WIDTH);

		for(int m = 0; m < drawIntensity[0].length - 1; m++) {
			for(int n = 0; n < 4; n++) {
				gcTop.setStroke(BASE_COLOR[n]);
				gcTop.strokeLine(m, drawIntensity[n][m], m+1, drawIntensity[n][m+1]);
			}

			if(basecall[pointer] == m + basecallStart) {
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

	private void calculateRangeOfDrawedBase(int[] basecall, int start, int end) {

		if(seqTop.getDataType() == typeAb1) {
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
}
