package edu.cornell.gdiac.physics.gameobjects.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.platform.DudeModel;

public class Enemy extends BoxObstacle {

	private static class EnemyLoSCallback implements RayCastCallback {

		private final Body target;

		private boolean hitPlayer = false;

		private EnemyLoSCallback(Body target) {
			this.target = target;
		}

		@Override
		public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
			Body body = fixture.getBody();
			if (body.getType() == BodyDef.BodyType.StaticBody) { // For simplicity's sake, we're considering all static bodies to be obstacles.
				return 0;
			} else if (body.equals(target)) { // The body is not static? Might be our target. Let's check to see if it is.
				hitPlayer = true;
				return 0;
			}
			return -1; // Otherwise, ignore the fixture and continue
		}
	}
	/**
	 * The default range that the enemy can hear noise/footsteps around them
	 */
	private static final float ENEMY_DETECTION_RANGE_NOISE = 3;

	private static final float ENEMY_DETECTION_RANGE_SIGHT = 1000;

	private static final float ENEMY_DETECTION_RANGE_SHADOW = 20;

	private static final float ENEMY_DETECTION_ANGLE_SIGHT = 30;

	/**
	 * The boolean for whether or not this enemy is alerted by the player
	 */
	private boolean alerted = false;

	public Enemy(float x, float y) {
		super(x, y, 1, 1);
		setName("enemy");
	}

	/**
	 * Returns whether or not the enemy is alerted by the player
	 * @return true if the enemy is alerted, false otherwise.
	 */
	public boolean isAlerted() {
		return alerted;
	}

	/**
	 * Sets whether or not the enemy is alerted by the player
	 * @param alerted True if the enemy should be alerted, false otherwise.
	 */
	public void setAlerted(boolean alerted) {
		this.alerted = alerted;
	}

	/**
	 * Checks if the given player object is in line of sight. This checks based on their position, centered
	 * around their body of mass.
	 * @param world The world to check the player's line of sight, required to perform raycasting with.
	 * @param player The given player object
	 * @return true if this player is visible to the enemy, false otherwise.
	 */
	public boolean isPlayerInLineOfSight(World world, DudeModel player) {

		/*
		 * First let's check to see if the player is near the enemy at all.
		 * The enemy will be able to hear their footsteps if so, then become alerted.
		 * We'll first check by distance, then we'll check to see if the enemy has line-of-sight
		 *
		 * Next we're checking to see if the player is in a conal LoS
		 * The enemy will be able to hear their footsteps if so, then become alerted.
		 */

		Vector2 pos = new Vector2(getPosition());
		Vector2 playerPos = new Vector2(player.getPosition());
		double dst = playerPos.dst(pos);
		Vector2 direction = new Vector2(0, -1); // Dummy direction vector. Represents the enemy looking south
		Vector2 dirToVector = new Vector2(player.getPosition()).sub(pos).nor();
//		Gdx.app.log("los", "poses: " + new Vector2(player.getPosition()) + " - " + pos);
//		Gdx.app.log("los", "dirto: " + dirToVector);
//		Gdx.app.log("los", "angle: " + direction.angleDeg(dirToVector));
		float angle = direction.angleDeg(dirToVector);
		boolean possiblyVisible = dst <= ENEMY_DETECTION_RANGE_NOISE
			                          || dst <= ENEMY_DETECTION_RANGE_SIGHT
				                             && (angle <= ENEMY_DETECTION_ANGLE_SIGHT || angle >= 360 - ENEMY_DETECTION_ANGLE_SIGHT);

		if (possiblyVisible) {
			EnemyLoSCallback callback = new EnemyLoSCallback(player.getBody());
			world.rayCast(callback, getPosition(), player.getPosition());
			if (callback.hitPlayer) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void draw(GameCanvas canvas) {
		/*
		 * Dummy drawing code for now. Uses the drawDebug code to draw the outline of the enemy.
		 * Green = the enemy is not alerted
		 * Red = the enemy is alerted
		 */
		canvas.beginDebug();
		Color color = alerted ? Color.RED : Color.GREEN;
		canvas.drawPhysics(shape,color,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
		canvas.endDebug();
	}

}
