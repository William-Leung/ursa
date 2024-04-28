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

/**
 * This class represents the trees of the game and can either have snow or no snow upon creation.
 * Trees with no snow cannot be shaken and trees with snow can only be shaken once.
 * Origin is at the bottom middle of the texture.
 */
public class Tree extends PolygonObstacle {
	/** Has the tree shaken yet? Once shaken, trees will not be able to shake again. */
	private boolean hasShaken;

	public Tree(float[] points, float x, float y, float offset, float textureScale) {
		super(points, x, y, offset, textureScale);
		setBodyType(BodyDef.BodyType.StaticBody);
		hasShaken = false;
	}

	public boolean canShake() {
		return !hasShaken;
	}

	public void putOnShakeCooldown() {
		hasShaken = true;
	}
	//FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), false);
}
