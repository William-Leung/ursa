package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import java.awt.Polygon;

public class Cave extends PolygonObstacle {
    public Cave(float[] points, float x, float y,float yOffset, float textureScale) {
        super(points, x, y, yOffset, textureScale);
        setBodyType(BodyDef.BodyType.StaticBody);
    }
}
