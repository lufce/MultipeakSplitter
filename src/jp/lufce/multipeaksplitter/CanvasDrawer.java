package jp.lufce.multipeaksplitter;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

public class CanvasDrawer {

	protected static final Affine IDENTITY_TRANSFORM = new Affine(1f,0f,0f,0f,1f,0);
	protected GraphicsContext gc;

	CanvasDrawer(GraphicsContext gc_arg){
		gc = gc_arg;
	}

	protected void clearCanvas() {

		gc.setFill(Color.WHITE);
		gc.setTransform(IDENTITY_TRANSFORM);
		gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
	}
}
