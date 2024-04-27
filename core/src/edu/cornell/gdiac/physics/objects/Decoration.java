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
    private TextureRegion redTextureRegion;

    public Decoration(TextureRegion texture, Vector2 scale, float x, float y) {
        this.texture = texture;
        this.drawScale = scale;
        this.x = x;
        this.y = y;

        int CIRCLE_RADIUS = 500;

        Pixmap pixmap = new Pixmap(2 * CIRCLE_RADIUS + 1, 2 * CIRCLE_RADIUS + 1,
                Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED); // Set the color to red
        pixmap.fillCircle(CIRCLE_RADIUS, CIRCLE_RADIUS,
                CIRCLE_RADIUS); // Fill a circle in the pixmap
        redTextureRegion = new TextureRegion(
                new Texture(pixmap)); // Create a texture region from the pixmap
        pixmap.dispose();

    }

    public void draw(GameCanvas canvas) {
        canvas.draw(texture, Color.WHITE, 0, 0, x * drawScale.x, y * drawScale.y, 0, 0.75f, 0.75f);
        //canvas.draw(redTextureRegion,Color.WHITE, x * drawScale.y,y * drawScale.y, 100,100);
    }
}