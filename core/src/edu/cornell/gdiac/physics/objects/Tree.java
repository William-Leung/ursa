package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class Tree extends PolygonObstacle {
	public static final float X_SCALE = 0.3f;
	public static final float Y_SCALE = 0.3f;
	/** has the tree shaken yet: used to limit shaking to one time */
	private boolean hasShaken;

	public Tree(float[] points, float x, float y) {
		super(points, x, y);
		setBodyType(BodyDef.BodyType.StaticBody);
		hasShaken = false;
	}

	public boolean canShake() {
		return !hasShaken;
	}


	public void putOnShakeCooldown() {
		hasShaken = true;
	}

	@Override
	public void setTexture(TextureRegion value) {
		super.setTexture(value);
		/*
		 * For some reason the origin for tree textures aren't set properly, so we just have to do it manually here.
		 * We also need to set the origin to the base/trunk of the tree.
		 */
		origin.set(texture.getRegionWidth() / 2.0f, 0);
	}

	public void draw(GameCanvas canvas) {
		Affine2 affine = new Affine2()
			.translate(getX() * drawScale.x, getY() * drawScale.y)
			.scale(X_SCALE, Y_SCALE)
		;
		canvas.draw(texture, Color.WHITE ,origin.x, origin.y, affine);
		//FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), false);
	}

}
