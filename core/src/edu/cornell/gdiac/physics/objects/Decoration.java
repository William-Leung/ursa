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

public class Decoration {

    private float x;
    private float y;
    private TextureRegion texture;
    private Vector2 drawScale;
    private int index;

    public Decoration(TextureRegion texture, Vector2 scale, float x, float y, int index) {
        this.texture = texture;
        this.drawScale = scale;
        this.x = x;
        this.y = y;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void draw(GameCanvas canvas) {
        canvas.draw(texture, Color.WHITE, 0, 0, x * drawScale.x, y * drawScale.y, 0, 0.75f, 0.75f);
    }
}