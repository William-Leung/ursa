package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class Tree extends PolygonObstacle {

	private static final int SHAKE_ANIMATION_TIME = 25;
	private static final int SHAKE_COOLDOWN = 180;
	private static final float SHAKE_STRENGTH = 0.1f;

	public static final float X_SCALE = 0.3f;
	public static final float Y_SCALE = 0.3f;

	private int shakeCooldown = 0;

	private boolean hasShaken;
	private int shakeAnimation = 0;

	private int shakeAnimationInvert = 1;

	public Tree(float[] points, float x, float y) {
		super(points, x, y);
		setBodyType(BodyDef.BodyType.StaticBody);
		hasShaken = false;
	}

	public boolean canShake() {
		return !hasShaken;
	}

	public void putOnShakeCooldown() {
		shakeCooldown = SHAKE_COOLDOWN;
		shakeAnimation = SHAKE_ANIMATION_TIME;
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

	@Override
	public void update(float dt) {
		shakeCooldown = Math.max(shakeCooldown - 1, 0);
		shakeAnimation = Math.max(shakeAnimation - 1, 0);
		shakeAnimationInvert *= -1;
	}

	public void draw(GameCanvas canvas) {
		Affine2 affine = new Affine2()
			.translate(getX() * drawScale.x, getY() * drawScale.y)
			.scale(X_SCALE, Y_SCALE)
			.shear(shakeAnimationInvert * SHAKE_STRENGTH * ((float) shakeAnimation / SHAKE_ANIMATION_TIME), 0);
		canvas.draw(texture, Color.WHITE,origin.x, origin.y, affine);
	}

}
