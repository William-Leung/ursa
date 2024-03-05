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

    public void draw(GameCanvas canvas) {
        canvas.draw(texture, Color.WHITE,getAnchor().x,getAnchor().y,getWidth(),getHeight());
    }

}
