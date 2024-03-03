package edu.cornell.gdiac.physics.gameobjects.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;

public class Enemy extends BoxObstacle {

	/**
	 * The default range that the enemy can hear noise/footsteps around them
	 */
	private static final float ENEMY_DETECTION_RANGE_NOISE = 5;

	private static final float ENEMY_DETECTION_RANGE_SIGHT = 10;

	private static final float ENEMY_DETECTION_RANGE_SHADOW = 20;

	private static final float ENEMY_DETECTION_ANGLE_SIGHT = 75;

	public Enemy(float x, float y, float width, float height) {
		super(x, y, width, height);
	}

	/**
	 * Checks if the given player object is in line of sight. This checks based on their position, centered
	 * around their body of mass.
	 * @param world The world to check the player's line of sight, required to perform raycasting with.
	 * @param player The given player object
	 * @return true if this player is visible to the enemy, false otherwise.
	 */
	public boolean isPlayerInLineOfSight(World world, Player player) {

		/*
		 * First let's check to see if the player is near the enemy at all.
		 * The enemy will be able to hear their footsteps if so, then become alerted.
		 * We'll first check by distance, then we'll check to see if the enemy has line-of-sight
		 */

		/*
		 * Next we're checking to see if the player is in a conal LoS
		 * The enemy will be able to hear their footsteps if so, then become alerted.
		 */

		double dst = player.getPosition().dst(getPosition());
		boolean possiblyVisible = dst <= ENEMY_DETECTION_RANGE_NOISE
			                          || dst <= ENEMY_DETECTION_RANGE_SIGHT
				                             && getPosition().angleDeg(player.getPosition()) <= ENEMY_DETECTION_ANGLE_SIGHT;

		if (possiblyVisible) {
			// TODO: Perform raycast here to check for obstacle obscurity
			return true;
		}

		return false;
	}

}
