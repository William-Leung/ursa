package edu.cornell.gdiac.physics.shadows;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    /** Tinting for the shadow */
    private final Color shadowTint;

    /**
     * The direction that the shadow is facing.
     * Invariant: This vector is always normalized.
     */
    private Vector2 direction = new Vector2(0, 1);

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

    public ShadowModel(float[] points, float x, float y) {
        super(points, x, y, 0,1);
        this.shadowTint = new Color(1,1,1,0.3f);

        setSensor(true);
        setName("shadow");
    }

    /**
     * Setter for direction variable
     * @param newDirec The new direction.
     */
    public void setDirection (Vector2 newDirec) {
        direction.set(newDirec);
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

    /**
     * Adjusts the tinting of the shadows such that the alphas of shadows + tint sum to 0.3
     * @param backgroundColor color of background tinting
     */
    public void updateTinting(Color backgroundColor) {
        shadowTint.a = 0.35f - backgroundColor.a;
    }

    /**
     * preDraw simply draws the shadow to the canvas just as in a normal draw() method.
     * However, we want to do this before all the game objects are drawn.
     * @param canvas Drawing context
     */
    @Override
    public void preDraw(GameCanvas canvas) {
        canvas.draw(region,shadowTint,0,0,getX()*drawScale.x,getY()*drawScale.y, vectorToRadians(direction), 1, 1);

//        Affine2 affine = new Affine2()
//                .rotate(vectorToRadians(direction))
//                .translate(getX() * drawScale.x, getY()* drawScale.y)
//                .scale(1, 1)
//                ;
//        canvas.draw(region, shadowTint, 0, 0, affine);
    }

    /**
     * All shadow drawing is done in preDraw() so this method doesn't do anything.
     * @param canvas Drawing context
     */
    @Override
    public void draw(GameCanvas canvas) {
        /**if (region != null) {
            Affine2 affine = new Affine2()
                    .rotate(vectorToRadians(direction))
                    .translate(getX() * drawScale.x, getY()* drawScale.y)
                    .scale(1, 1)
                    ;
           // canvas.draw(texture, Color.WHITE, texture.getRegionWidth() / 2f, yOffset, affine);
        }*/
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