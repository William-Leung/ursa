package edu.cornell.gdiac.physics.gameobjects.entities;

import com.badlogic.gdx.graphics.Color;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;

public class Player extends BoxObstacle {

	public Player(float x, float y, float width, float height) {
		super(x, y, width, height);
	}

	@Override
	public void draw(GameCanvas canvas) {
		/*
		 * Dummy drawing code for now. Uses the drawDebug code to draw the outline of the player.
		 */
		canvas.drawPhysics(shape,Color.BLUE,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
	}

}
