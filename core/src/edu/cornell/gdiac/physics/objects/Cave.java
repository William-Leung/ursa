package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import java.awt.Polygon;

public class Cave extends PolygonObstacle {

    public Cave(float[] points, float x, float y) {
        super(points, x, y);
        setBodyType(BodyDef.BodyType.StaticBody);
    }

    public void draw(GameCanvas canvas) {
        float x_SCALE = 0.75f;
        float y_SCALE = 0.75f;
        float yOffset = 0f;

        Affine2 affine = new Affine2()
                .translate(getX() * drawScale.x, getY()* drawScale.y - yOffset)
                .scale(x_SCALE, y_SCALE)
                ;
        canvas.draw(texture, Color.WHITE ,256, 200, affine);    }
}
