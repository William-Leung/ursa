package edu.cornell.gdiac.physics.shadows;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.SceneModel;
import edu.cornell.gdiac.util.PooledList;
import java.util.LinkedList;
import com.badlogic.gdx.math.*;

public class ShadowController {

    /** Current time of day */
    private int time;
    /** The texture used for all shadows in this controller. */
    private TextureRegion texture;
    /** Boolean that tracks whether it is currently nighttime **/
    private static boolean isNight;

    /**
     * The amount of x-skew to apply to all shadows
     */
    private float xSkew = 0.75f;

    /**
     * The amount of y-scaling to apply to all shadows. If this is negative then the shadow will appear
     * upside down.
     */
    private float yScalar = 0.8f;

    /** The total number of ticks in a day */
    private static final int TICKS_PER_DAY = 240;

    /** Creates and initializes a new instance of a shadow controller
     *
     * The shadow controller has a time of 0 ticks and texture texture */
    public ShadowController(TextureRegion texture) {
        this.time = 0;
        this.texture = texture;
        isNight = false;
    }

    /** Creates and initializes a new instance of a shadow controller
     *
     * The shadow controller has a time of 0 ticks */
    public ShadowController() {
        this.time = 0;
        this.texture = null;
        isNight = false;
    }

    public TextureRegion getTexture() { return texture; }

    public void setTexture(TextureRegion t) { texture = t; }

//    public void initAllShadows() {
//
//        // INITIALIZATION DATA FOR ALL THE SHADOWS --- DELETE LATER
//        ShadowModel[] init_data = {
//                new ShadowModel(new Vector2(100f, 100f), new Vector2(300f,300f), texture)
//        };
//
//        for (ShadowModel sh : init_data) addShadow(sh);
//    }

    Vector2 origDir = new Vector2(0, 1);
    public void updateShadow(ShadowModel sh) {
//        if (time < TICKS_PER_DAY / 2) {
//            sh.setWidth(sh.getInitWidth() - (time * (1/(TICKS_PER_DAY / 2))));
//        } else {
//            sh.setWidth(0 + (time * (1/(TICKS_PER_DAY / 2))));
//        }
        //System.out.println("Ticks: " + time);
        if (!isNight) {
            sh.rotateDirection((float) (360 / TICKS_PER_DAY) );
        } else {
            sh.setDirection(origDir);
        }
    }

    public void update(SceneModel sceneModel) {
        if (time == 180) {
            isNight = true;
        } else if (time == 360) {
            time = 0;
            isNight = false;
        }
        time++;
        for (ShadowModel sh : sceneModel.getShadows()) {
            updateShadow(sh);
        }
    }

    public void drawAllShadows(GameCanvas canvas, SceneModel sceneModel) {
        for (ShadowModel sh: sceneModel.getShadows()) {
            sh.draw(canvas, xSkew, yScalar);
        }
    }

    /** An isNight getter to access the time of night from within ShadowController **/
    public static boolean isNight() { return isNight; }

    //public boolean checkOverlap(Vector2 ursa_tl, Vector2 ursa_br, ShadowModel sh) {}
}
