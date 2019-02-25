package jp.lufce.multipeaksplitter;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class DotplotCanvasDrawer extends DraggableCanvasDrawer {

	private boolean[][] drawnDotplot;

	private final double GRID_LINE_WIDTH = 0.025;

	private int   dotsize = 1;
	private int   forwardSequenceLength = 0;
	private int[] forwardStartPoint = new int[2];
	private int   forwardInterception = 0;
	private int   reverseSequenceLength = 0;
	private int[] reverseStartPoint = new int[2];
	private int   reverseInterception = 0;

	DotplotCanvasDrawer(GraphicsContext gc){
		super(gc);
		aff = IDENTITY_TRANSFORM.clone();
	}

	public void setDrawnDotplot(boolean[][] dot) {
		drawnDotplot = dot;
	}

	public int getForwardSequenceLength() {
		return this.forwardSequenceLength;
	}

	public int getForwardInterception() {
		return this.forwardInterception;
	}

	public int getReverseSequenceLength() {
		return this.reverseSequenceLength;
	}

	public int getForwardStart() {
		return this.forwardStartPoint[1];
	}

	public int getReverseStart() {
		return this.reverseStartPoint[1];
	}

	public int getReverseInterception() {
		return this.reverseInterception;
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

		int topStart   = scdTop.getDrawnStartIndex();
		int topEnd     = scdTop.getDrawnEndIndex();
		int topLength  = scdTop.getSequenceLength();

		int leftStart  = scdLeft.getDrawnStartIndex();
		int leftEnd    = scdLeft.getDrawnEndIndex();
		int leftLength = scdLeft.getSequenceLength();

		super.gc.setFill(Color.YELLOW);
		super.gc.fillRect(topStart, 0, (topEnd - topStart + 1) * dotsize, leftLength * dotsize);
		super.gc.fillRect(0,leftStart, topLength * dotsize, (leftEnd - leftStart + 1) * dotsize);

		super.gc.setFill(Color.ORANGE);
		super.gc.fillRect(topStart,leftStart, (topEnd - topStart + 1) * dotsize, (leftEnd - leftStart + 1) * dotsize);
	}

	private void drawDot() {
		super.gc.setFill(Color.BLACK);

		for(int m = 0; m < drawnDotplot.length; m++) {
			for(int n = 0; n < drawnDotplot[0].length; n++) {
				if(drawnDotplot[m][n]) {
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

	public boolean clickDot(double clickX, double clickY) {
		int dotX, dotY;

		dotX = (int)Math.ceil((clickX - getAffine().getTx() ) / (dotsize * getAffine().getMxx()) );
		dotY = (int)Math.ceil((clickY - getAffine().getTy() ) / (dotsize * getAffine().getMxx()) );

		this.forwardInterception = dotY - dotX;
		this.reverseInterception = dotY + dotX - 2;

		if(dotX > 0 && dotY > 0) {
			this.searchClickedSequence(dotX, dotY);
			return true;
		}else {
			return false;
		}

	}

	private void searchClickedSequence(int dotX, int dotY) {

		int X = dotX - 1;
		int Y = dotY - 1;

		forwardSequenceLength = 0;
		reverseSequenceLength = 0;

		if(X >= 0 && X < drawnDotplot.length && Y >= 0 && Y < drawnDotplot[0].length && drawnDotplot[X][Y]) {
			while(X >= 0 && X < drawnDotplot.length && Y >= 0 && Y < drawnDotplot[0].length && drawnDotplot[X][Y] ) {
				forwardSequenceLength++;
				X--;
				Y--;
			}

			forwardStartPoint[0] = X + 2;
			forwardStartPoint[1] = Y + 2;

			X = dotX;
			Y = dotY;

			while(X >= 0 && X < drawnDotplot.length && Y >= 0 && Y < drawnDotplot[0].length && drawnDotplot[X][Y]) {
				forwardSequenceLength++;
				X++;
				Y++;
			}

			X = dotX - 1;
			Y = dotY - 1;

			while(X >= 0 && X < drawnDotplot.length && Y >= 0 && Y < drawnDotplot[0].length && drawnDotplot[X][Y]) {
				reverseSequenceLength++;
				X++;
				Y--;
			}

			reverseStartPoint[0] = X;
			reverseStartPoint[1] = Y + 2;

			X = dotX - 2;
			Y = dotY;

			while(X >= 0 && X < drawnDotplot.length && Y >= 0 && Y < drawnDotplot[0].length && drawnDotplot[X][Y]) {
				reverseSequenceLength++;
				X--;
				Y++;
			}

		}
	}

	public void highlightSelectedDotSequence() {
		if(forwardSequenceLength > 0) {
			gc.setFill(Color.RED);

			for(int N = 0; N < forwardSequenceLength; N++) {
				gc.fillRect( (forwardStartPoint[0]-1 + N)*dotsize, (forwardStartPoint[1]-1 + N)*dotsize, dotsize, dotsize);
			}
		}
		if(reverseSequenceLength > 0) {
			gc.setFill(Color.RED);

			for(int N = 0; N < reverseSequenceLength; N++) {
				gc.fillRect( (reverseStartPoint[0]-1 - N)*dotsize, (reverseStartPoint[1]-1 + N)*dotsize, dotsize, dotsize);
			}
		}
	}
}
