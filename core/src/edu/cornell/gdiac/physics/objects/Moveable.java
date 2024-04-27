package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

/**
 * A generic (non-moveable and non-interactable) physics-influenced obstacle.
 */
public class Moveable extends BoxObstacle {
    private float sx = 0.5f;
    private float sy = 0.5f;

    /**
     * A moveable physics-influenced obstacle. Extends BoxObstacle.
     */
    public Moveable(float x, float y, float width, float height) {
        super(x, y, width, height);
        setBodyType(BodyDef.BodyType.DynamicBody);
        setDensity(3.0f);
        setFriction(1.0f);
        setLinearDamping(1.0f);
        setFixedRotation(true);
    }

    public void draw(GameCanvas canvas) {
        canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x,
                getY() * drawScale.y,
                getAngle(), sx, sy);
    }

}
