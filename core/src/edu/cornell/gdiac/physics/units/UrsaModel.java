package edu.cornell.gdiac.physics.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.SceneModel;
import edu.cornell.gdiac.physics.obstacle.CapsuleObstacle;

public class UrsaModel extends CapsuleObstacle {

    private static final float BLOB_SHADOW_SIZE = 1.15f;

    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;
    /** The factor to multiply by the input */
    private final float force;
    /** The amount to slow the character down */
    private final float damping;
    /** The maximum character speed */
    private final float maxspeed;
    /** Identifier to allow us to track the sensor in ContactListener */
    private final String sensorName;
    /** The current horizontal movement of the character */
    private float xMovement;
    /** The current vertical movement of the character */
    private float yMovement;
    /** Which direction is the character facing */
    private boolean isFacingRight;
    /** The physics shape of this object */
    private PolygonShape sensorShape;
    private boolean inShadow;

    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();

    private float textureScale;


    /**
     * Returns left/right movement of this character.
     *
     * This is the result of input times dude force.
     *
     * @return left/right movement of this character.
     */
    public float getXMovement() {
        return xMovement;
    }
    public float getyMovement(){
        return yMovement;
    }

    /**
     * Sets left/right movement of this character.
     *
     * This is the result of input times dude force.
     *
     * @param xValue left/right movement of this character.
     */
    public void setMovement(float xValue,float yValue) {
        xMovement = xValue;
        yMovement = yValue;
        // Change facing if appropriate
        if (xMovement < 0) {
            isFacingRight = false;
        } else if (xMovement > 0) {
            isFacingRight = true;
        }
    }

    public boolean isInShadow() {
        return inShadow;
    }

    public void setInShadow(boolean v) {
        inShadow = v;
    }

    /**
     * Returns how much force to apply to get the dude moving
     *
     * Multiply this by the input to get the movement value.
     *
     * @return how much force to apply to get the dude moving
     */
    public float getForce() {
        return force;
    }

    /**
     * Returns ow hard the brakes are applied to get a dude to stop moving
     *
     * @return ow hard the brakes are applied to get a dude to stop moving
     */
    public float getDamping() {
        return damping;
    }

    /**
     * Returns the upper limit on dude left-right movement.
     *
     * This does NOT apply to vertical movement.
     *
     * @return the upper limit on dude left-right movement.
     */
    public float getMaxSpeed() {
        return maxspeed;
    }

    /**
     * Returns the name of the ground sensor
     *
     * This is used by ContactListener
     *
     * @return the name of the ground sensor
     */
    public String getSensorName() {
        return sensorName;
    }

    /**
     * Creates a new dude avatar with the given physics data
     *
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * @param data  	The physics constants for this dude
     * @param width		The object width in physics units
     * @param height	The object width in physics units
     */
    public UrsaModel(float xPos,float yPos,JsonValue data, float width, float height, float textureScale) {
        // The shrink factors fit the image to a tigher hitbox

        super(xPos, yPos,
                width,
                height);
        setDensity(data.getFloat("density", 0));
        setFriction(data.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);


        maxspeed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        sensorName = "shadow_sensor";
        this.data = data;

        // Gameplay attributes
        isFacingRight = true;
        setName("ursa");
        this.textureScale = textureScale;
    }

    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     *
     * This method overrides the base method to keep your ship from spinning.
     *
     * @param world Box2D world to store body
     *
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
        // create the box from our superclass
        if (!super.activatePhysics(world)) {
            return false;
        }

        // Shadow sensor
        Vector2 sensorCenter = new Vector2(0, 0);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = data.getFloat("density",0);
        sensorDef.isSensor = true;
        sensorShape = new PolygonShape();

        float size = BLOB_SHADOW_SIZE + 0.5f;
        sensorShape.setAsBox(size, size / 2f, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Ground sensor to represent our feet
        Fixture sensorFixture = body.createFixture( sensorDef );
        sensorFixture.setUserData(getSensorName());

        return true;
    }

    /**
     * Applies the force to the body of this dude
     *
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if (!isActive()) {
            return;
        }

        // Don't want to be moving. Damp out player motion

        if (getXMovement() == 0f) {
            forceCache.set(-getDamping() * getVX(), 0);
            body.applyForce(forceCache, getPosition(), true);
        }
        if (getyMovement() == 0f) {
            forceCache.set(0, -getDamping() * getVY());
            body.applyForce(forceCache, getPosition(), true);
        }

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getVX()) * getMaxSpeed());
        } else {
            forceCache.set(getXMovement(), 0);
            body.applyForce(forceCache, getPosition(), true);
        }
        if (Math.abs(getVY()) >= getMaxSpeed()) {
            setVY(Math.signum(getVY()) * getMaxSpeed()); // Set y-velocity, not x-velocity
        } else {
            forceCache.set(0, getyMovement()); // Set y-movement
            body.applyForce(forceCache, getPosition(), true);
        }
    }

    /**
     * Draws shadow underneath Ursa
     * @param canvas Drawing context
     */
    @Override
    public void preDraw(GameCanvas canvas) {
        Texture blobShadow = SceneModel.BLOB_SHADOW_TEXTURE;
        int xcenter = blobShadow.getWidth() / 2;
        int ycenter = blobShadow.getHeight() / 2;
        canvas.draw(blobShadow, Color.WHITE,xcenter,ycenter,
           getX() * drawScale.x,getY() * drawScale.y,getAngle(),BLOB_SHADOW_SIZE / drawScale.x,
          (BLOB_SHADOW_SIZE / 2f) / drawScale.y);
    }

    /**
     * Draws Ursa to the screen.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = isFacingRight ? 1.0f : -1.0f;
        canvas.draw(texture, Color.WHITE,origin.x,0,getX()*drawScale.x,(getY() - data.get("yOffset").asFloat())*drawScale.y,getAngle(),effect * textureScale,textureScale);
    }

    /**
     * Draws the outline of the physics body.
     * This method can be helpful for understanding issues with collisions.
     *
     * @param canvas Drawing context
     */
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        canvas.drawPhysics(sensorShape,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }

}
