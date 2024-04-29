package edu.cornell.gdiac.physics.shadows;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.math.*;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.PooledList;

/**
 * This class represents dynamic shadows for all static game objects.
 * This includes (not expansively) houses, rocks, trunks, and trees.
 */
public class ShadowModel extends PolygonObstacle {
    private final Color shadowColor;
    /** Converts between 2560 x 1440 resolution assets were drawn for and 1920 x 1080 resolution */
    private final float textureScale;

    /**
     * The direction that the shadow is facing.
     * Invariant: This vector is always normalized.
     */
    private Vector2 direction = new Vector2(0, 1);


    public void setDirection (Vector2 newDirec) { direction.set(newDirec); }

    public static float[] ShadowPolygon(String texture) {
        float[] tree = new float[] {
                0.0f, -4.0f, -4.0f, -1.0f, -6.0f, 0.6f, 0.0f, 20.0f, 6.0f, 0.6f, 4.0f, -1.0f
        };
        float[] cave = new float[] {
                0, -2f,
                -2f, 0,
                -3f, 2f,
                0, 4f,
                3f, 2f,
                3f, 0f,
        };
        float[] rock = new float[] {
                0, -1f,
                -2f, -0.25f,
                -2.5f, 3f,
                0, 5f,
                2.5f, 3f,
                2f, -0.25f,
        };
        switch (texture) {
            case "tree":
                return tree;
            case "cave":
                return cave;
            case "rock":
                return rock;
            default:
                return tree;
        }
    }

    public ShadowModel(float x, float y, String shadowType, float textureScale) {
        super(ShadowPolygon(shadowType), x, y, 0,textureScale);
        this.shadowColor = new Color(1,1,1,0.3f);
        this.textureScale = textureScale;

        setSensor(true);
        setName("shadow");
    }

    /**
     * Rotates the direction of the shadow by the given degrees. These rotations are counterclockwise.
     * @param degrees The amount of degrees to rotate the shadow.
     */
    public void rotateDirection(float degrees) {
        direction.rotateDeg(degrees).nor();
        Transform transform = body.getTransform();
        body.setTransform(transform.getPosition(), transform.getRotation() + (float) Math.toRadians(degrees));
    }

    @Override
    public void setDrawScale(Vector2 scale) {
        super.setDrawScale(scale);
        this.drawScale.set(scale);
    }

    @Override
    public void preDraw(GameCanvas canvas) {
        canvas.draw(region,shadowColor,0,0,getX()*drawScale.x,getY()*drawScale.y, vectorToRadians(direction), textureScale, textureScale);
    }

    @Override
    public void draw(GameCanvas canvas) {

    }

    /**
     * Converts a vector to radians
     * @param vector Vector2 to be converted
     * @return corresponding radians value
     */
    public static float vectorToRadians(Vector2 vector) {
        return (float) Math.atan2(vector.y, vector.x);
    }
}