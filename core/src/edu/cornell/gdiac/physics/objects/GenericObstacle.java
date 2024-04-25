package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

/**
 * A generic (non-moveable and non-interactable) physics-influenced obstacle.
 */
public class GenericObstacle extends BoxObstacle {
    private float sx = 0.5f;
    private float sy = 0.5f;
    private float heightModifier = 1.0f;

    /**
     * A generic (non-moveable and non-interactable) physics-influenced obstacle.
     * Extends BoxObstacle.
     */
    public GenericObstacle(float x, float y, float width, float height) {
        super(x,y,width, height);
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(0.0f);
        setFriction(0.0f);
        setRestitution(0.0f);
    }

    /**
     * A generic (non-moveable and non-interactable) physics-influenced obstacle.
     * Extends BoxObstacle.
     * @param sx: The x-axis scaling factor.
     * @param sy: The y-axis scaling factor.
     */
    public GenericObstacle(float x, float y, float width, float height, float sx, float sy) {
        super(x,y,width, height);
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(0.0f);
        setFriction(0.0f);
        setRestitution(0.0f);
        this.sx = sx;
        this.sy = sy;
    }

    /**
     * A generic (non-moveable and non-interactable) physics-influenced obstacle.
     * Extends BoxObstacle.
     * @param sx: The x-axis scaling factor.
     * @param sy: The y-axis scaling factor.
     * @param heightModifier: Modifies the y-axis displacement of the texture from its physics body
     *                      by the region height of the texture divided by heightModifier.
     *                      Cannot be 0.
     */
    public GenericObstacle(float x, float y, float width, float height, float sx, float sy, float heightModifier) {
        super(x,y,width, height);
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(0.0f);
        setFriction(0.0f);
        setRestitution(0.0f);
        this.sx = sx;
        this.sy = sy;
        this.heightModifier = heightModifier;
    }

    /**
     * A generic (non-moveable and non-interactable) physics-influenced obstacle.
     * Extends BoxObstacle.
     * @param heightModifier: Modifies the y-axis displacement of the texture from its physics body
     *                      by the region height of the texture divided by heightModifier.
     *                      Cannot be 0.
     */
    public GenericObstacle(float x, float y, float width, float height, float heightModifier) {
        super(x,y,width, height);
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(0.0f);
        setFriction(0.0f);
        setRestitution(0.0f);
        this.heightModifier = heightModifier;
    }

    public void draw(GameCanvas canvas) {
        canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,
                getY()*drawScale.y + (float) texture.getRegionHeight()/heightModifier,getAngle(),sx,sy);
    }
}
