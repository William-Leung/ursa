package edu.cornell.gdiac.physics.shadows;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.GameCanvas;
import java.util.LinkedList;
import com.badlogic.gdx.math.*;

public class ShadowController {

    /** Current time of day */
    private int time;
    /** All of the dynamic shadows on the map */
    private LinkedList<ShadowModel> shadows;
    /** The texture used for all shadows in this controller. */
    private TextureRegion texture;

    /** The total number of ticks in a day */
    private static final int TICKS_PER_DAY = 240;

    /** Creates and initializes a new instance of a shadow controller
     *
     * The shadow controller has a time of 0 ticks and texture texture */
    public ShadowController(TextureRegion texture) {
        this.time = 0;
        shadows = new LinkedList<ShadowModel>();
        this.texture = texture;
    }

    /** Creates and initializes a new instance of a shadow controller
     *
     * The shadow controller has a time of 0 ticks */
    public ShadowController() {
        this.time = 0;
        shadows = new LinkedList<ShadowModel>();
        this.texture = null;
    }

    public TextureRegion getTexture() { return texture; }

    public void setTexture(TextureRegion t) { texture = t; }

    public void addShadow(Vector2 top_left, Vector2 bottom_right, TextureRegion texture) {
        shadows.add(new ShadowModel(top_left, bottom_right, texture));
    }

    public void addShadow(ShadowModel sh) { shadows.add(sh); }

    public void initAllShadows() {

        // INITIALIZATION DATA FOR ALL THE SHADOWS --- DELETE LATER
        ShadowModel[] init_data = {
                new ShadowModel(new Vector2(100f, 100f), new Vector2(300f,300f), texture)
        };

        for (ShadowModel sh : init_data) addShadow(sh);
    }

    public void updateShadow(ShadowModel sh) {
        if (time < TICKS_PER_DAY / 2) {
            sh.setWidth(sh.getInitWidth() - (time * (1/(TICKS_PER_DAY / 2))));
        } else {
            sh.setWidth(0 + (time * (1/(TICKS_PER_DAY / 2))));
        }
        //System.out.println("Ticks: " + time);
    }

    public void update() {
        if (time == TICKS_PER_DAY) {
            time = 0;
        } else {
            time++;
        }

        for (ShadowModel sh : shadows) { updateShadow(sh); }
    }

    public void drawAllShadows(GameCanvas canvas) {
        for (ShadowModel sh: shadows) {
            sh.draw(canvas);
        }
    }

    //public boolean checkOverlap(Vector2 ursa_tl, Vector2 ursa_br, ShadowModel sh) {}
}
