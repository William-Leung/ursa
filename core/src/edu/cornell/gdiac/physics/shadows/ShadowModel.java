package edu.cornell.gdiac.physics.shadows;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.math.*;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

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
    private Vector2 direction = new Vector2(1, 0);
    /** X offset of the shadow. */
    private float xOffset;
    /** Y offset of the shadow. */
    private float yOffset;
    private boolean doesShadowMove;

    public ShadowModel(float[] points, float x, float y, float xOffset, float yOffset, boolean doesShadowMove) {
        super(points, x, y, 0,1);
        this.shadowTint = new Color(1,1,1,0.35f);
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.doesShadowMove = doesShadowMove;

        setSensor(true);
        setName("shadow");
    }

    /**
     * Setter for direction variable
     * @param newDirec The new direction.
     */
    public void setDirection (Vector2 newDirec) {
        if(!doesShadowMove) {
            return;
        }
        direction.set(newDirec);
    }

    /**
     * Rotates the direction of the shadow by the given degrees. These rotations are counterclockwise.
     * @param degrees The amount of degrees to rotate the shadow.
     */
    public void rotateDirection(float degrees) {
        if(!doesShadowMove) {
            return;
        }
        direction.rotateDeg(degrees).nor();
        Transform transform = body.getTransform();
        body.setTransform(transform.getPosition(), transform.getRotation() + (float) Math.toRadians(degrees));
    }

    /**
     * Adjusts the tinting of the shadows such that the alphas of shadows + tint sum to 0.35
     * Create smoother transitions when shadows disappear at night.
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
        canvas.draw(region,shadowTint,0,0,getX()*drawScale.x + xOffset,getY()*drawScale.y + yOffset, vectorToRadians(direction), 1, 1);
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