package edu.cornell.gdiac.physics.tree;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class Tree extends PolygonObstacle {

	private static final int SHAKE_COOLDOWN = 60;

	public static final float X_SCALE = 0.1f;
	public static final float Y_SCALE = 0.1f;

	private int shakeCooldown = 0;

	public Tree(float[] points, float x, float y) {
		super(points, x, y);
		setBodyType(BodyDef.BodyType.StaticBody);
	}

	public boolean canShake() {
		return shakeCooldown <= 0;
	}

	public void putOnShakeCooldown() {
		shakeCooldown = SHAKE_COOLDOWN;
	}

	@Override
	public void setTexture(TextureRegion value) {
		super.setTexture(value);
		/*
		 * For some reason the origin for tree textures aren't set properly, so we just have to do it manually here.
		 * We also need to set the origin to the base/trunk of the tree.
		 */
		origin.set(texture.getRegionWidth()/2.0f, 50);
	}

	@Override
	public void update(float dt) {
		shakeCooldown = Math.max(shakeCooldown - 1, 0);
	}

	public void draw(GameCanvas canvas) {
		canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),
			X_SCALE,Y_SCALE);
	}

}
