package edu.cornell.gdiac.physics.enemy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.SimpleObstacle;
import edu.cornell.gdiac.physics.platform.DudeModel;

public class Enemy extends BoxObstacle {

	/**
	 * The callback class for the enemy line-of-sight raycast towards the targeted body. This is used to detect whether or not there are any obstacles
	 * between the body position and the enemy position. If so, it will cancel the raycast callback and report that we could
	 * not hit the body (If we were able to hit the player then there wouldn't be any obstacles in between the body and the enemy)
	 */
	private static class EnemyLoSCallback implements RayCastCallback {

		/**
		 * The targeted body by the line-of-sight raycast
		 */
		private final Body target;

		/**
		 * The indication if the body was hit or not.
		 */
		private boolean hitPlayer = false;

		/**
		 * Constructs a new EnemyLoSCallback object used for raycasting.
		 * @param target
		 */
		private EnemyLoSCallback(Body target) {
			this.target = target;
		}

		@Override
		public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
			Body body = fixture.getBody();

			if (body.getType() == BodyDef.BodyType.StaticBody) { // For simplicity's sake, we're considering all static bodies to be obstacles
				hitPlayer = false;
				return 0;
			} else if (body.equals(target)) { // The body is not static? Might be our target. Let's check to see if it is.
				hitPlayer = true;
				return -1;
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

	public Enemy(JsonValue data, float width, float height) {
		// The shrink factors fit the image to a tigher hitbox
		super(	data.get("pos").getFloat(0),
				data.get("pos").getFloat(1),
				width*data.get("shrink").getFloat( 0 ),
				height*data.get("shrink").getFloat( 1 ));
		setDensity(data.getFloat("density", 0));
		setFriction(data.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
		setFixedRotation(true);


		setName("ursa");
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
	public boolean isPlayerInLineOfSight(World world, SimpleObstacle player) {

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
		Vector2 direction = new Vector2(1, 0); // Dummy direction vector. Represents the enemy looking East
		Vector2 dirToVector = new Vector2(player.getPosition()).sub(pos).nor();
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


	public void draw(GameCanvas canvas) {
		Color color = alerted ? Color.RED : Color.GREEN;
		canvas.draw(texture, color,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),1.0f,1.0f);

	}

}