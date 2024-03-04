package edu.cornell.gdiac.physics.tree;

import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;

public class Tree extends BoxObstacle {

	public Tree(float x, float y, float width, float height) {
		super(x, y, width, height);
		setBodyType(BodyDef.BodyType.StaticBody);
	}

}
