package edu.cornell.gdiac.physics.tree;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import edu.cornell.gdiac.physics.shadows.ShadowModel;
import edu.cornell.gdiac.physics.shadows.ShadowedObject;
import java.awt.Polygon;

public class Tree extends PolygonObstacle implements ShadowedObject {

	private static final float X_SCALE = 0.1f;
	private static final float Y_SCALE = 0.1f;

	private final ShadowModel shadow;

	public Tree(float[] points, float x, float y) {
		super(points, x, y);
		setBodyType(BodyDef.BodyType.StaticBody);
		this.shadow = new ShadowModel(new Vector2(getX(), getY()));
		shadow.setScale(X_SCALE, Y_SCALE);
	}

	public void draw(GameCanvas canvas) {
		canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),
			X_SCALE,Y_SCALE);
	}

	@Override
	public void setTexture(TextureRegion value) {
		super.setTexture(value);
		shadow.setTexture(texture);
		shadow.setTextureOrigin(origin);
	}

	@Override
	public void setDrawScale(Vector2 drawScale) {
		super.setDrawScale(drawScale);
		shadow.setDrawScale(this.drawScale);

	}

	@Override
	public ShadowModel getShadow() {
		return shadow;
	}
}
