package edu.cornell.gdiac.physics.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.SceneModel;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import edu.cornell.gdiac.physics.obstacle.SimpleObstacle;
import edu.cornell.gdiac.physics.shadows.ShadowController;
import edu.cornell.gdiac.physics.shadows.ShadowModel;
import edu.cornell.gdiac.util.PooledList;

public class Enemy extends BoxObstacle {
	private static final float SIGHT_RANGE_INCREMENT = 0.75f;
	private static final float BLOB_SHADOW_SIZE = 0.85f;

	private static final int WALK_DAMPENING = 15;
	private static final int STUN_DAMPENING = 25;

	private static final int STUN_DURATION = 60 * 5;

	private final int num_vertices = 24;

	private float[] vertices = new float[num_vertices * 2];
	protected Vector2[] coneVectors = new Vector2[num_vertices * 2];

	/** A Pixmap used for drawing sightcones */
	private TextureRegion redTextureRegion;
	private TextureRegion greenTextureRegion;
	private TextureRegion grayTextureRegion;
	private TextureRegion purpleTextureRegion;
	private final Vector2 forceCache = new Vector2();
	private float maxSpeed;
	private float damping;
	private float maxX;
	private float minX;
	private Vector2 playerPos;
	private float previousXMovement;
	private float previousYMovement;

	private float textureScale;

	private boolean playerInShadow = false;
	protected boolean playerCurrentInSight;
	private boolean playerInDynamicShadow = false;
	private boolean stunned = false;
	private boolean adaptive = false;

	/** the vector to use to indicate the direction the enemy character
	 * should go/face x and y should be either -15 or 15 or 0*/
	private Vector2 movementDirection = new Vector2(15,0);

	/**
	 * The looking direction of the enemy in the x/y direction.
	 * Invariant: This direction is always normalized.
	 */
	private Vector2 lookDirection = new Vector2(1, 0);
	protected PolygonRegion sightConeRegion;

	/**
	 * The callback class for the enemy line-of-sight raycast towards the targeted body. This is used to detect whether or not there are any obstacles
	 * between the body position and the enemy position. If so, it will cancel the raycast callback and report that we could
	 * not hit the body (If we were able to hit the player then there wouldn't be any obstacles in between the body and the enemy)
	 */
	protected static class EnemyLoSCallback implements RayCastCallback {

		/**
		 * The targeted body by the line-of-sight raycast
		 */
		protected final Body target;

		/**
		 * The indication if the body was hit or not.
		 */
		protected boolean hitPlayer = false;

		/**
		 * The point at which the raycast terminates, if interrupted by something.
		 */
		protected Vector2 rayTerm;

		/**
		 * Constructs a new EnemyLoSCallback object used for raycasting.
		 * @param target
		 */
		protected EnemyLoSCallback(Body target) {
			this.target = target;
		}

		protected Vector2 getRayTerm() {
			return rayTerm;
		}

		protected void resetRayTerm() {
			rayTerm = null;
		}

		@Override
		public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
			Body body = fixture.getBody();

			if (body.getType() == BodyDef.BodyType.StaticBody) { // For simplicity's sake, we're considering all static bodies to be obstacles
				hitPlayer = false;
				rayTerm = point;
				return 0;
			} else if (body.equals(target)) { // The body is not static? Might be our target. Let's check to see if it is.
				hitPlayer = true;
				rayTerm = point;
				return -1;
			}
			return -1; // Otherwise, ignore the fixture and continue
		}
	}

	protected static class ObstacleCallback implements RayCastCallback {
		protected final Vector2 rayOrigin;

		/**
		 * The point at which the raycast terminates, if interrupted by something.
		 */
		protected Vector2 rayTerm;

		protected float rayDist;

		/** was the raycast blocked by an obstacle? */
		protected boolean blocked;

		public ObstacleCallback(Vector2 origin) {
			rayOrigin = origin;
			rayTerm = null;
			blocked = false;
		}

		public Vector2 getRayTermination() {
			return rayTerm;
		}

		public boolean wasBlocked() {
			return blocked;
		}


		@Override
		public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
			Body body = fixture.getBody();

			if (body.getType() == BodyDef.BodyType.StaticBody) { // For simplicity's sake, we're considering all static bodies to be obstacles
				if (rayTerm == null || point.dst2(rayOrigin) < rayDist) { // Get the closest point to the ray origin
					rayTerm = point.cpy();
					rayDist = rayTerm.dst2(rayOrigin);
				}
				blocked = true;
				return 1;
			}
			return -1;
		}
	}

	/**
	 * The default range that the enemy can hear noise/footsteps around them
	 */
	private static final float ENEMY_DETECTION_RANGE_NOISE = 4f;

	private static final float ENEMY_DETECTION_RANGE_SIGHT = 10f;

	private static final float ENEMY_DETECTION_RANGE_SHADOW = 24f;

	private static final float ENEMY_DETECTION_ANGLE_SIGHT = 25;

	/**
	 * The boolean for whether or not this enemy is alerted by the player
	 */
	private boolean alerted = false;

	/**
	 * Indicates whether or not this enemy is stunned.
	 */
	private int stunDuration = 0;

	private float detectionRange = ENEMY_DETECTION_RANGE_SIGHT;

	private float speed = 8f;

	public Enemy(float xStart,float yStart,float maxX, float minX,JsonValue data, float width, float height, float textureScale) {
		// The shrink factors fit the image to a tigher hitbox
		super(xStart,
				yStart, width, height);
		setDensity(data.getFloat("density", 0));
		setFriction(data.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
		setFixedRotation(true);
		maxSpeed = data.getFloat("maxspeed", 0);
		//data.getFloat("damping", 0);
		this.maxX = maxX;
		this.minX = minX;

		/** Creating the red and green texture regions */
		Pixmap redPixmap = new Pixmap(1, 1, Format.RGBA8888);
		redPixmap.setColor(new Color(1, 0, 0, 0.5f));
		redPixmap.fill();
		Texture redTexture = new Texture(redPixmap);
		redTextureRegion = new TextureRegion(redTexture);

		Pixmap greenPixmap = new Pixmap(1, 1, Format.RGBA8888);
		greenPixmap.setColor(new Color(0, 1, 0, 0.3f));
		greenPixmap.fill();
		Texture greenTexture = new Texture(greenPixmap);
		greenTextureRegion = new TextureRegion(greenTexture);

		Pixmap purplePixmap = new Pixmap(1, 1, Format.RGBA8888);
		purplePixmap.setColor(Color.PURPLE);
		purplePixmap.fill();
		Texture purpleTexture = new Texture(purplePixmap);
		purpleTextureRegion = new TextureRegion(purpleTexture);

		Pixmap grayPixmap = new Pixmap(1, 1, Format.RGBA8888);
		grayPixmap.setColor(new Color(181/255, 181/255, 181/255, 0.25f));
		grayPixmap.fill();
		Texture grayTexture = new Texture(grayPixmap);
		grayTextureRegion = new TextureRegion(grayTexture);


		redPixmap.dispose();
		greenPixmap.dispose();
		/** RED TEXTURE AND GREENTEXTURE ARE NOT DISPOSED*/
		setName("enemy");

		this.textureScale = textureScale;
	}

	public void stun() {
		stunDuration = STUN_DURATION;
	}

	/**
	 * Returns whether or not the enemy is alerted by the player
	 * @return true if the enemy is alerted, false otherwise.
	 */
	public boolean isAlerted() {
		return alerted;
	}

	public boolean isStunned() {
		return stunDuration > 0;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public float getSpeed() {
		return speed;
	}

	public void setAdaptive(boolean v) { adaptive = v; }

	/**
	 * gets the player position to move towards
	 */
	public void getPlayerPos(Vector2 pos){
		playerPos = pos;
	}

	public void applyForce() {
		if (!isActive()) {
			return;
		}

		if (isStunned()) {
			setVX(getVX() * 0.75f);
			setVY(getVY() * 0.75f);
			return;
		}

		if(previousXMovement != movementDirection.x){
			setVX(previousXMovement/2);
		}
		if(previousYMovement != movementDirection.y){
			setVY(previousXMovement/2);
		}
		previousXMovement = movementDirection.x;
		previousYMovement = movementDirection.y;
		if (!playerCurrentInSight) {

			if (this.getPosition().x >= maxX) {
				movementDirection.x = -15;
				setLookDirection(-1, 0);
			}
			if (this.getPosition().x <= minX) {
				movementDirection.x = 15;
				setLookDirection(1, 0);
			}
			if (movementDirection.x == 0f) {
				forceCache.set(-damping * getVX(), 0);
				body.applyForce(forceCache, getPosition(), true);
			}
			if (movementDirection.y == 0f) {
				forceCache.set(0, -damping * getVY());
				body.applyForce(forceCache, getPosition(), true);
			}

		} else {
			if(playerPos.x > getPosition().x){
				movementDirection.x = 15;
			} else if (playerPos.x < getPosition().x) {
				movementDirection.x = -15;
			}
			else {
				movementDirection.x = 0;
			}
			if(playerPos.y > getPosition().y){
				movementDirection.y = 15;
			} else if (playerPos.y < getPosition().y) {
				movementDirection.y = -15;
			}
			else {
				movementDirection.y = 0;
			}
		}
		if (Math.abs(getVX()) >= maxSpeed * 2) {
			//setVX(Math.signum(getVX()) * maxSpeed);
		} else {
			forceCache.set(isStunned() ? 0 : movementDirection.x, 0);
			body.applyForce(forceCache, getPosition(), true);
		}
		if (Math.abs(getVY()) >= maxSpeed * 2) {
			//setVY(Math.signum(getVY()) * maxSpeed ); // Set y-velocity, not x-velocity
		} else {
			forceCache.set(0, isStunned() ? 0 : movementDirection.y); // Set y-movement
			body.applyForce(forceCache, getPosition(), true);
		}

	}

	@Override
	public void update(float dt) {
		stunDuration = Math.max(stunDuration - 1, 0);

		if (isInShadow()) {
			detectionRange = Math.min(detectionRange + SIGHT_RANGE_INCREMENT, ENEMY_DETECTION_RANGE_SHADOW);
		} else {
			detectionRange = Math.max(detectionRange - SIGHT_RANGE_INCREMENT, ENEMY_DETECTION_RANGE_SIGHT);
		}
	}

	/**
	 * Sets the look direction of the enemy given the x and y direction vectors.
	 * The new look direction will be normalized.
	 * @param dx The x-direction vector
	 * @param dy The y-direction vector.
	 */
	public void setLookDirection(float dx, float dy) {
		lookDirection.set(dx, dy).nor();
	}

	/**
	 * Rotates the look direction of the enemy by a given number of degrees.
	 * @param degrees The amount of degrees to rotate the enemy by, positive or negative.
	 */
	public void rotateLookDirection(float degrees) {
		lookDirection.rotateDeg(degrees);
	}

	public float getLookAngle() {
		return lookDirection.angleDeg();
	}

	/**
	 * Sets whether or not the enemy is alerted by the player
	 * @param alerted True if the enemy should be alerted, false otherwise.
	 */

	public void setAlerted(boolean alerted) {
		this.alerted = alerted;
	}

	public void setInShadow(boolean shadowed) {
		this.playerInDynamicShadow = shadowed;
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

		Vector2 dirToVector = new Vector2(player.getPosition()).sub(pos).nor();
		float angle = lookDirection.angleDeg(dirToVector);
		boolean possiblyVisible;
		possiblyVisible = dst <= detectionRange && (angle <= ENEMY_DETECTION_ANGLE_SIGHT || angle >= 360 - ENEMY_DETECTION_ANGLE_SIGHT)
			                  || dst <= ENEMY_DETECTION_RANGE_NOISE;

		if (possiblyVisible) {
			EnemyLoSCallback callback = new EnemyLoSCallback(player.getBody());
			world.rayCast(callback, getPosition(), player.getPosition());

			if (callback.hitPlayer) {
				playerCurrentInSight = true;
				return true;
			}

		}
		playerCurrentInSight = false;
		return false;
	}

	@Override
	public void preDraw(GameCanvas canvas) {
		Texture blobShadow = SceneModel.BLOB_SHADOW_TEXTURE;
		int xcenter = blobShadow.getWidth() / 2;
		int ycenter = blobShadow.getHeight() / 2;
		canvas.draw(blobShadow, Color.WHITE,xcenter,ycenter,
				getX()*drawScale.x,(getY() - 1.25f) * drawScale.y,getAngle(),BLOB_SHADOW_SIZE / drawScale.x,
				(BLOB_SHADOW_SIZE / 2f) / drawScale.y);
	}

	public void draw(GameCanvas canvas) {
		canvas.draw(sightConeRegion, Color.WHITE, origin.x,origin.y,getX()*drawScale.x,
				getY()*drawScale.y,getAngle(),1.0f,1.0f);
		canvas.draw(texture, Color.WHITE,origin.x,0,getX()*drawScale.x,(getY() - getHeight() / 2) * drawScale.y,getAngle(),
				(lookDirection.x > 0 ? 1 : -1) * textureScale,textureScale);
	}

	/**
	 * drawSightCones(canvas, num_vertices) uses PolygonRegion to
	 */
	public void createSightCone(World world) {
		// Create the vertices which will form the cone
		//float[] vertices = new float[num_vertices * 2];
		vertices[0] = 0f;
		vertices[1] = 0f;
		float curr_angle = ENEMY_DETECTION_ANGLE_SIGHT + lookDirection.angleDeg();
		float angle_scale_factor =  (ENEMY_DETECTION_ANGLE_SIGHT)/ (
				(float) (num_vertices - 2) / 2);

		for(int i = 2; i < vertices.length - 1; i += 2) {
			Vector2 sightConePoint = new Vector2();
			sightConePoint.x = detectionRange * (float) Math.cos(Math.toRadians(curr_angle));
			sightConePoint.y = detectionRange * (float) Math.sin(Math.toRadians(curr_angle));

			ObstacleCallback callback = new ObstacleCallback(getPosition());
			world.rayCast(callback, getPosition(), sightConePoint.cpy().add(getPosition()));
			coneVectors[i] = callback.rayTerm;
			if (coneVectors[i] != null) {
				coneVectors[i] = coneVectors[i].cpy();
			}

			if (callback.rayTerm != null) {
				sightConePoint = callback.rayTerm.cpy().sub(getPosition());
			}
			vertices[i] = sightConePoint.x * drawScale.x;
			vertices[i+1] = sightConePoint.y * drawScale.y;
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

		// Create a polygonRegion with color dependent on alerted
		PolygonRegion polygonRegion;
		if (stunned) {
			polygonRegion = new PolygonRegion(grayTextureRegion,vertices, triangles);
		} else if(alerted) {
			polygonRegion = new PolygonRegion(redTextureRegion,vertices, triangles);
		} else if (adaptive) {
			polygonRegion = new PolygonRegion(purpleTextureRegion,vertices, triangles);
		} else {
			polygonRegion = new PolygonRegion(greenTextureRegion,vertices, triangles);
		}
		sightConeRegion = polygonRegion;
	}

	public boolean isInShadow() {
		playerInShadow = ShadowController.isNight() || playerInDynamicShadow;
		return playerInShadow;
	}

	private int getDampening() {
		return stunDuration > 0 ? STUN_DAMPENING : WALK_DAMPENING;
	}

	public float getMaxStun() { return STUN_DURATION; }

	public void setStunned(boolean value) { stunned = value; }

	@Override
	public void drawDebug(GameCanvas canvas) {
		super.drawDebug(canvas);
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(0.5f, 0.5f);
		for (Vector2 rayTerm : coneVectors) {
			if (rayTerm != null) {
				canvas.drawPhysics(shape, Color.BLACK,rayTerm.x,rayTerm.y,getAngle(),drawScale.x,drawScale.y);
			}
		}

	}
}