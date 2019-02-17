package jp.lufce.multipeaksplitter;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

public class DotplotCanvasDrawer extends CanvasDrawer {

	private Affine aff;
	private boolean[][] drawnDotplot;

	private final double GRID_LINE_WIDTH = 0.025;

	private int dotsize = 1;

	DotplotCanvasDrawer(GraphicsContext gc){
		super(gc);
		aff = super.IDENTITY_TRANSFORM.clone();
	}

	public void setDrawnDotplot(boolean[][] dot) {
		drawnDotplot = dot;
	}

	public void setAffine(Affine aff_arg) {
		aff = aff_arg;
	}

	public Affine getAffine() {
		return aff.clone();
	}

	public void resetAffine() {
		aff = super.IDENTITY_TRANSFORM.clone();
	}

	public void updateDotplot(SequenceCanvasDrawer scdTop, SequenceCanvasDrawer scdLeft) {
		super.clearCanvas();
		this.drawDotplot(scdTop, scdLeft);
	}

	private void drawDotplot(SequenceCanvasDrawer scdTop, SequenceCanvasDrawer scdLeft) {
		super.gc.setTransform(aff);

		this.highlightTopAndLeftRange(scdTop, scdLeft);
		this.drawDot();
		this.drawGridLines();
	}

	private void highlightTopAndLeftRange(SequenceCanvasDrawer scdTop, SequenceCanvasDrawer scdLeft) {

		int topStart = scdTop.getDrawnStartIndex();
		int topEnd = scdTop.getDrawnEndIndex();
		int topLength = scdTop.getSequenceLength();

		int leftStart = scdLeft.getDrawnStartIndex();
		int leftEnd = scdLeft.getDrawnEndIndex();
		int leftLength = scdLeft.getSequenceLength();

		super.gc.setFill(Color.YELLOW);
		super.gc.fillRect(topStart, 0, (topEnd - topStart + 1) * dotsize, leftLength * dotsize);
		super.gc.fillRect(0,leftStart, topLength * dotsize, (leftEnd - leftStart + 1) * dotsize);
		super.gc.setFill(Color.ORANGE);
		super.gc.fillRect(topStart,leftStart, (topEnd - topStart + 1) * dotsize, (leftEnd - leftStart + 1) * dotsize);
	}

	private void drawDot() {
		super.gc.setFill(Color.BLACK);
		//drawnDotplot = this.judgeDrawnDotplot();

		for(int m = 0; m < drawnDotplot.length; m++) {
			for(int n = 0; n < drawnDotplot[0].length; n++) {
				if(drawnDotplot[m][n]) {
					//gc.fillRect(m*scaled_dotsize, n*scaled_dotsize, scaled_dotsize, scaled_dotsize);
					super.gc.fillRect(m*dotsize, n*dotsize, dotsize, dotsize);
				}
			}
		}
	}

	private void drawGridLines() {

		super.gc.setStroke(Color.BLACK);
		super.gc.setLineWidth(GRID_LINE_WIDTH);

		for(int m = 1; m < drawnDotplot.length ; m++) {
			super.gc.strokeLine(m*dotsize, 0, m*dotsize, drawnDotplot[0].length * dotsize);
		}

		for(int n = 1; n < drawnDotplot[0].length; n++) {
			super.gc.strokeLine(0, n*dotsize, drawnDotplot.length * dotsize, n*dotsize);
		}
	}
}
