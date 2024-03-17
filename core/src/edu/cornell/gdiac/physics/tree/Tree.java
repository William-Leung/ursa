package edu.cornell.gdiac.physics.tree;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class Tree extends PolygonObstacle {

	public static final float X_SCALE = 0.1f;
	public static final float Y_SCALE = 0.1f;


	public Tree(float[] points, float x, float y) {
		super(points, x, y);
		setBodyType(BodyDef.BodyType.StaticBody);
	}

	public void draw(GameCanvas canvas) {
		canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),
			X_SCALE,Y_SCALE);
	}


}
