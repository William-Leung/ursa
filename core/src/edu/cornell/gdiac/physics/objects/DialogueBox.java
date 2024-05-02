package edu.cornell.gdiac.physics.objects;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.physics.GameCanvas;

/**
 * This class represents dialogue boxes and also the ursa sleeping in cave indicator.
 * I'm low key abusing this subclass extension.
 */
public class DialogueBox extends Decoration{

    public DialogueBox(TextureRegion texture, Vector2 scale, float x, float y, int index, float textureScale) {
        super(texture, scale, x, y, index, textureScale);
    }


    public void draw(GameCanvas canvas) {
    }

    public void postDraw(GameCanvas canvas) {
        super.draw(canvas);
    }
}
