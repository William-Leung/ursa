package edu.cornell.gdiac.physics.shadows;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.SceneModel;
import edu.cornell.gdiac.util.PooledList;
import java.util.LinkedList;
import com.badlogic.gdx.math.*;

/**
 * ShadowController stores references to all the shadows in the world that are used for collisions.
 * (This does not include player and enemy shadows which are done in preDraw())
 * It also stores the time of day which interacts with the day/night UI in Scene Model
 */
public class ShadowController {

    /** Current time of day */
    private static int time;
    /** The texture used for all shadows in this controller. */
    private TextureRegion texture;
    /** Boolean that tracks whether it is currently nighttime **/
    private static boolean isNight;

    /**
     * The amount of x-skew to apply to all shadows
     */
    private float xSkew = 0.75f;
    /**
     * ratio of ticks remaining in day
     */
    private float timeRatio = time/1800;
    /**
     * The amount of y-scaling to apply to all shadows. If this is negative then the shadow will appear
     * upside down.
     */
    private float yScalar = 0.8f;

    /** The total number of ticks in a day */
    private static final int TICKS_PER_DAY = 240;

    private final Vector2 origDir = new Vector2(0, 1);

    /** Creates and initializes a new instance of a shadow controller
     *
     * The shadow controller has a time of 0 ticks and texture texture */
    public ShadowController(TextureRegion texture) {
        time = 0;
        this.texture = texture;
        isNight = false;
    }



    /** Creates and initializes a new instance of a shadow controller
     *
     * The shadow controller has a time of 0 ticks */
    public ShadowController() {
        time = 0;
        this.texture = null;
        isNight = false;
    }
    public float getTimeRatio(){
        return timeRatio;
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

    public void updateShadow(ShadowModel sh) {
//        if (time < TICKS_PER_DAY / 2) {
//            sh.setWidth(sh.getInitWidth() - (time * (1/(TICKS_PER_DAY / 2))));
//        } else {
//            sh.setWidth(0 + (time * (1/(TICKS_PER_DAY / 2))));
//        }
        //System.out.println("Ticks: " + time);
        if (!isNight) {
            sh.rotateDirection((float) (360 / TICKS_PER_DAY)/5 );
        } else {
            sh.setDirection(origDir);
        }


//        System.out.println(sh.getDirection());
    }

    public void update(SceneModel sceneModel) {
        if (time == 1800) {
            isNight = true;
        } else if (time == 3600) {
            time = 0;
            isNight = false;
        }
        time++;
        timeRatio = time/1800f;
        for (ShadowModel sh : sceneModel.getShadows()) {
            updateShadow(sh);
        }
    }

    public void reset(SceneModel sm) {
        this.time = 0;
        this.isNight = false;
        update(sm);
    }

    /**
     * Draws shadows for all objects in the SceneModel.
     * If it is night, draws nothing.
     *
     * This method actually doesn't do anything right now. PreDraw draws the shadows
     * @param canvas GameCanvas
     * @param sceneModel SceneModel
     */
    public void drawAllShadows(GameCanvas canvas, SceneModel sceneModel) {
        if(isNight()) {
            return;
        }
        for (ShadowModel sh: sceneModel.getShadows()) {
            sh.draw(canvas);
        }
    }

    /** An isNight getter to access the time of night from within ShadowController **/
    public static boolean isNight() { return isNight; }

    //public boolean checkOverlap(Vector2 ursa_tl, Vector2 ursa_br, ShadowModel sh) {}

    /** Sets the time of day */
    public void setTime(int time) { this.time = time % 3600; }

    /** Gets the time of day */
    public int getTime() { return this.time; }
}
