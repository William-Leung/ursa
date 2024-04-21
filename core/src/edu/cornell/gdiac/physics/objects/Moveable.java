package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class Moveable extends PolygonObstacle {
    public Moveable(float[] points, float x, float y) {
        super(points, x, y);
        setBodyType(BodyDef.BodyType.DynamicBody);
    }
}
