package edu.cornell.gdiac.physics.tree;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import java.awt.Polygon;

public class Tree extends PolygonObstacle {

	public Tree(float[] points, float x, float y) {
		super(points, x, y);
		setBodyType(BodyDef.BodyType.StaticBody);
	}

	public void draw(GameCanvas canvas) {
		canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),0.1f,0.175f);
	}

}
