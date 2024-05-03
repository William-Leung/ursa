package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

/**
 * This class represents game objects with a physics body but no special attributes.
 * Thus, houses, rocks, and trunks are all encompassed in this class.
 * Origin is at the bottom middle of the texture
 */
public class GameObject extends PolygonObstacle {
    public GameObject(float[] points, float x, float y, float yOffset, float textureScale) {
        super(points, x, y, yOffset, textureScale);

        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(0.0f);
        setFriction(0.0f);
        setRestitution(0.0f);
    }
}
