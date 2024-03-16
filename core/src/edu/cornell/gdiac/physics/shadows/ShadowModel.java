package edu.cornell.gdiac.physics.shadows;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.math.*;
import edu.cornell.gdiac.physics.GameCanvas;

/**
 * Dynamic (currently only rectangular) shadows for the current map
 */
public class ShadowModel {

    /** Coordinate of the top left corner of the shadow. */
    private Vector2 top_left;
    /** Coordinate of the bottom right corner of the shadow. */
    private Vector2 bottom_right;
    /** Anchor point of this shadow, i.e. point around which it rotates. */
    private final Vector2 shadow_anchor;
    /** The initial "height" of the shadow, used for updating */
    private final float initial_height;
    /** The initial "width" of the shadow, used for updating */
    private final float initial_width;
    /** The texture used to show this shadow */
    private TextureRegion texture;

    private float sx;
    private float sy;
    private Vector2 drawScale = new Vector2();
    private Vector2 origin = new Vector2();

    /**
     * The direction that the shadow is facing.
     * Invariant: This vector is always normalized.
     */
    private Vector2 direction = new Vector2(0, 1);


    public Vector2 getTopLeft() { return top_left; }

    public Vector2 getBottomRight() { return bottom_right; }

    public Vector2 getAnchor() { return shadow_anchor; }

    public void setTopLeft(Vector2 newCoord) { top_left = newCoord; }

    public void setBottomRight(Vector2 newCoord) { bottom_right = newCoord; }

    public ShadowModel(Vector2 top_left, Vector2 bottom_right, TextureRegion texture) {
        this.top_left = top_left;
        this.bottom_right = bottom_right;

        this.shadow_anchor = new Vector2(bottom_right.x,
                (top_left.y + bottom_right.y) / 2);

        this.texture = texture;

        this.initial_height = top_left.y - bottom_right.y;
        this.initial_width = bottom_right.x - top_left.x;
    }

    public ShadowModel(Vector2 anchor) {
        this.shadow_anchor = anchor;

        this.initial_height = 0;
        this.initial_width = 0;
    }

    /**
     * Rotates the direction of the shadow by the given degrees. These rotations are counterclockwise.
     * @param degrees The amount of degrees to rotate the shadow.
     */
    public void rotateDirection(float degrees) {
        direction.rotateDeg(degrees).nor();
    }

    /**
     * Gets the direction of the shadow. Modifying this vector can have unintended behavior. If you need to modify it for external use,
     * use {@link Vector2#cpy()} or {@link Vector2#Vector2(Vector2)} to construct a copy of the direction.
     * @return The direction of the shadow.
     */
    public Vector2 getDirection() {
        return direction;
    }

    public void setTexture(TextureRegion value) {
        this.texture = value;
    }

    public void setTextureOrigin(Vector2 origin) {
        this.origin.set(origin);
    }

    public void setScale(float sx, float sy) {
        this.sx = sx;
        this.sy = sy;
    }

    public void setDrawScale(Vector2 scale) {
        this.drawScale.set(scale);
    }

    public float getWidth() { return bottom_right.x - top_left.x; }

    public float getHeight() { return top_left.y - bottom_right.y; }

    public void setWidth(float width) {
        if (width < 0) {
            top_left.x = bottom_right.x + width;
        } else {
            bottom_right.x = top_left.x + width;
        }
    }

    public void setHeight(float height) {
        if (height < 0) {
            bottom_right.y = top_left.y + height;
        } else {
            top_left.x = bottom_right.x + height;
        }
    }

    public float getInitHeight() { return initial_height; }

    public float getInitWidth() { return initial_width; }

    /**
     * Draws the shadow onto the given GameCanvas with the specified max skew and y-scalar
     * @param canvas The GameCanvas to draw to
     * @param xSkew The maximum skew that this shadow can have.
     * @param yScalar The maximum y-scaling this shadow can have.
     */
    public void draw(GameCanvas canvas, float xSkew, float yScalar) {
        Affine2 affine = new Affine2()
            .setToTranslation(shadow_anchor.x * drawScale.x, shadow_anchor.y * drawScale.y)
            .scale(sx, sy * yScalar * direction.y)
            .shear(xSkew * direction.x, 0);

        canvas.draw(texture, new Color(0, 0, 0, 127), origin.x, origin.y, affine);
    }

}
