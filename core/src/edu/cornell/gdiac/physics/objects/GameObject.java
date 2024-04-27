package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class GameObject extends PolygonObstacle {
    private float xScale;
    private float yScale;

    /**
     * Creates a (not necessarily convex) polygon
     * <p>
     * The points given are relative to the polygon's origin.  They are measured in physics units.  They
     * tile the image according to the drawScale (which must be set for drawing to work properly).
     *
     * @param points The polygon vertices
     * @param x      Initial x position of the polygon center
     * @param y      Initial y position of the polygon center
     */
    public GameObject(float[] points, float x, float y, float xScale, float yScale) {
        super(points, x, y);
        this.xScale = 1 / xScale;
        this.yScale = 1 / yScale;

        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(0.0f);
        setFriction(0.0f);
        setRestitution(0.0f);
    }

    public void draw(GameCanvas canvas) {
        if (region != null) {
            // We draw the texture from the bottom middle
            canvas.draw(texture,Color.WHITE, texture.getRegionWidth() / 2f, 0,getX()*drawScale.x,getY()*drawScale.y,getAngle(),xScale,yScale);
        }
    }
}
