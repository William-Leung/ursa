package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

/**
 * This class represents a generic moveable obstacle.
 */
public class Moveable extends PolygonObstacle {

    /**
     * A moveable physics-influenced obstacle. Extends BoxObstacle.
     */
    public Moveable(float[] points, float width, float height, float yOffset, float textureScale) {
        super(points, width, height, yOffset, textureScale);
        setBodyType(BodyDef.BodyType.DynamicBody);
        setDensity(3.0f);
        setFriction(1.0f);
        setLinearDamping(1.0f);
        setFixedRotation(true);
    }
}
