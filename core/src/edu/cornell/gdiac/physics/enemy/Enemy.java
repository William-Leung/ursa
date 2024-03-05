package edu.cornell.gdiac.physics.enemy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.Shape;
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
	private float direc;
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

	private static final float ENEMY_DETECTION_RANGE_SIGHT = 5;

	private static final float ENEMY_DETECTION_RANGE_SHADOW = 20;

	private static final float ENEMY_DETECTION_ANGLE_SIGHT = 30;

	/**
	 * The boolean for whether or not this enemy is alerted by the player
	 */
	private boolean alerted = false;

	public Enemy(JsonValue data, float width, float height,float dire) {
		// The shrink factors fit the image to a tigher hitbox
		super(	data.get("pos").getFloat(0),
				data.get("pos").getFloat(1),
				width*data.get("shrink").getFloat( 0 ),
				height*data.get("shrink").getFloat( 1 ));
		setDensity(data.getFloat("density", 0));
		setFriction(data.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
		setFixedRotation(true);
		direc = dire;

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
		//System.out.println("Position" + playerPos);

		Vector2 direction = new Vector2(direc * 1, 0); // Dummy direction vector. Represents the enemy looking East
		Vector2 dirToVector = new Vector2(player.getPosition()).sub(pos).nor();
		float angle = direction.angleDeg(dirToVector);
		//System.out.println("Distance: " + dst);
		boolean possiblyVisible = (dst <= ENEMY_DETECTION_RANGE_NOISE
				|| dst <= ENEMY_DETECTION_RANGE_SIGHT)
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
		canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),direc* .1f,.1f);

		drawSightCone(canvas, 8, new Vector2(direc * 1,0));
	}

	/**
	 * drawSightCones(canvas, num_vertices) uses PolygonRegion to
	 * @param canvas This is the canvas you can draw with.
	 * @param num_vertices Number of vertices in PolygonRegion. Higher values increase smoothness.
	 * @param direction direction in which cone faces
	 * Invariant: num_vertices must be even and >= 4.
	 */
	public void drawSightCone(GameCanvas canvas, int num_vertices, Vector2 direction) {
		// Create the texture for the sight cone
		Pixmap pixmap = new Pixmap(1,1, Format.RGBA8888);
		if(alerted) {
			pixmap.setColor(new Color(1,0,0,0.5f));
		} else {
			pixmap.setColor(new Color(0,1,0,0.5f));
		}
		pixmap.fill();
		Texture cone_texture = new Texture(pixmap);
		TextureRegion cone_texture_region = new TextureRegion(cone_texture);

		// Create the vertices to form the cone
		float[] vertices = new float[num_vertices * 2];
		vertices[0] = 0f;
		vertices[1] = 0f;
		float curr_angle = ENEMY_DETECTION_ANGLE_SIGHT + direction.angleDeg();
		float angle_scale_factor =  (ENEMY_DETECTION_ANGLE_SIGHT)/ ((num_vertices - 2 ) / 2);
		for(int i = 2; i < vertices.length - 1; i += 2) {
			/** FIX: this 30 is super hard-coded. find out the world-local scaling*/
			vertices[i] = 30 * ENEMY_DETECTION_RANGE_SIGHT * (float) Math.cos(Math.toRadians(curr_angle));
			vertices[i+1] = 30 * ENEMY_DETECTION_RANGE_SIGHT * (float) Math.sin(Math.toRadians(curr_angle));
			curr_angle -= angle_scale_factor;
		}

		// Specify triangles to draw our texture region.
		// For example, triangles = {0,1,2} draws a triangle between vertices 0, 1, and 2
		short[] triangles = new short[3 * (num_vertices - 2)];
		short triangle_counter = 1;
		for(int i = 0; i < triangles.length - 2; i += 3) {
			triangles[i] = 0;
			triangles[i+1] = triangle_counter;
			triangle_counter++;
			triangles[i+2] = triangle_counter;
		}

		PolygonRegion polygonRegion = new PolygonRegion(cone_texture_region,vertices, triangles);
		canvas.draw(polygonRegion, Color.WHITE, origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),1.0f,1.0f);
	}

}