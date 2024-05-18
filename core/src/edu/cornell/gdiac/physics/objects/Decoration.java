package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.Obstacle;

/**
 * Decoration represents objects that are drawn onto the screen yet have no physics objects.
 * This includes the large textures on the ground and smaller decorations.
 * See decorationTextures in SceneModel for a comprehensive listing.
 * Origin is in the bottom left.
 */
public class Decoration {
    /** The decoration's x position */
    private final float x;
    /** The decoration's y position */
    private final float y;
    /** The decoration's texture */
    private final TextureRegion texture;
    /** The decoration's scale between screen coordinates and drawing coordinates */
    private final Vector2 drawScale;
    /** The decoration's scale between texture coordinates and drawing coordinates */
    private final float textureScale;
    /** The index of this decoration (used to determine drawing order)
     * Higher indexed decorations are drawn first and behind smaller indexed decorations
     */
    private final int index;
    private TextureRegion dialogueTexture;

    public Decoration(TextureRegion texture, Vector2 scale, float x, float y, int index, float textureScale) {
        this.texture = texture;
        this.drawScale = scale;
        this.x = x;
        this.y = y;
        this.index = index;
        this.textureScale = textureScale;
    }

    public void setDialogueTexture(TextureRegion t) {
        dialogueTexture = t;
    }

    public int getIndex() {
        return index;
    }

    public void draw(GameCanvas canvas) {
        canvas.draw(texture, Color.WHITE, 0, 0, x * drawScale.x, y * drawScale.y, 0, textureScale, textureScale);
    }

    public void postDraw(GameCanvas canvas) {
        if(dialogueTexture != null) {
            canvas.draw(dialogueTexture, Color.WHITE, 0, 0, x * drawScale.x, y * drawScale.y, 0, 1.5f *  textureScale, 1.5f * textureScale);
        }
    }
}