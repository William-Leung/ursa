package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class House extends PolygonObstacle {
    public House(float[] points, float x, float y) {
        super(points, x, y);
        setBodyType(BodyDef.BodyType.StaticBody);
    }
}
