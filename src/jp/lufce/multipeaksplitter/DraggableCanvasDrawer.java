package jp.lufce.multipeaksplitter;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Affine;

public class DraggableCanvasDrawer extends CanvasDrawer {
	protected Affine aff;

	private double scale = 1.0;

	private double mouseX;
	private double mouseY;
	private double mousePreX;
	private double mousePreY;
	private double mouseDeltaX;
	private double mouseDeltaY;

	DraggableCanvasDrawer(GraphicsContext gc){
		super(gc);
	}

	public void setAffine(Affine af) {
		aff = af;
	}

	public Affine getAffine() {
		return aff;
	}

	public void resetAffine() {
		aff = IDENTITY_TRANSFORM.clone();
	}

	public void setMousePre(double x, double y) {
		mousePreX = x;
		mousePreY = y;
	}

	public void setMouse(double x, double y) {
		mouseX = x;
		mouseY = y;
	}

	public void mouseDragged(double x, double y) {
		setMouse(x, y);
		calcDelta();
		setMousePre(x, y);
	}

	public void mouseScroll(double deltaY) {
		scale = deltaY >=0 ? 1.05 : 1/1.05;
		aff.appendScale(scale, scale, (int)Math.ceil((mouseX - aff.getTx() ) / aff.getMxx() ), (int)Math.ceil((mouseX - aff.getTy() ) / aff.getMyy() ));
	}

	private void calcDelta() {
		mouseDeltaX = (mouseX-mousePreX) / aff.getMxx();
		mouseDeltaY = (mouseY-mousePreY) / aff.getMxx();

		aff.appendTranslation(mouseDeltaX, mouseDeltaY);
	}
}
