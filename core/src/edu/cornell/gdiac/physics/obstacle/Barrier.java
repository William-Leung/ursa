package edu.cornell.gdiac.physics.obstacle;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

public class Barrier extends PolygonObstacle{

    /**
     * Creates a (not necessarily convex) polygon at the origin.
     * <p>
     * The points given are relative to the polygon's origin.  They are measured in physics units.  They
     * tile the image according to the drawScale (which must be set for drawing to work properly).
     *
     * @param points The polygon vertices
     */
    public Barrier(float[] points, float x, float y) {
        super(points, x, y);
        setBodyType(BodyType.StaticBody);
        setDensity(0);
        setFriction(0);
        setRestitution(0);
    }
}
