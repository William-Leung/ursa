package edu.cornell.gdiac.physics.shadows;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.util.PooledList;
import com.badlogic.gdx.math.*;

/**
 * ShadowController stores references to all the shadows in the world that are used for collisions.
 * (This does not include player and enemy shadows which are done in preDraw())
 * It also stores the time of day which interacts with the day/night UI in Scene Model
 */
public class ShadowController {
    /** Time in terms of number of update loops
     *  At 60 updates/second, time % 60 is the number of seconds elapsed
     */
    private static int time;
    /** Length of the day (dayLength % 60 is the number of seconds) */
    private static int dayLength = 1800;
    /** Length of the night */
    private static final int nightLength = 1800;
    /** Length of a single day/night cycle */
    private static final int fullDayLength = dayLength + nightLength;
    /** The texture of all shadows */
    private final TextureRegion shadowTexture;
    /** Is it currently night time?  **/
    private static boolean isNight;
    /**
     * Time as a float where 0 represents sunrise, 0.5 is sunset.
     * Always stays between 0 and 1
     */
    private float timeRatio;
    /** The starting direction of the shadows */
    private final Vector2 starting_direction = new Vector2(1, 0);
    /** List of references to all shadows. */
    private PooledList<ShadowModel> shadows = new PooledList<>();
    private boolean doShadowsMove;
    private float beginningTimeRatio = 0;
    private float endTimeRatio = 0;
    private FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), false);


    /**
     * Empty Constructor
     */
    public ShadowController() {
        this(null, false);
    }

    /**
     * Creates a new ShadowController, starting at time = 0
     */
    public ShadowController(TextureRegion region, boolean doShadowsMove) {
        time = 0;
        isNight = false;
        shadowTexture = region;
        this.doShadowsMove = doShadowsMove;
        if(!doShadowsMove) {
            time = 300;
        }
    }

    /**
     * @return the time ratio (always between 0 and 1)
     */
    public float getTimeRatio(){
        return timeRatio;
    }

    /**
     * Adds shadow to the list of shadows in ShadowController
     * @param shadow A shadow
     */
    public void addShadow(ShadowModel shadow) {
        shadow.setTexture(shadowTexture);
        shadow.setDirection(starting_direction);
        shadows.add(shadow);
    }

    /**
     * Updates the time and handles day -> night + night -> day transitions
     * If shadows are dynamic, rotates them.
     */
    public void update(Color backgroundColor) {
        // Transition from night to day
        if (time > fullDayLength) {
            for(ShadowModel shadow: shadows) {
                shadow.setDirection(starting_direction);
            }
            time = 0;
            isNight = false;
        // Transition from day to night
        } else if (time > dayLength) {
            isNight = true;
        }
        // Update the timeRatio to match the time
        timeRatio = (float) time / fullDayLength;

        // Update tinting ONLY if shadows are nonmoving
        if(!doShadowsMove) {
            for(ShadowModel shadow: shadows) {
                shadow.updateTinting(backgroundColor);
            }
            return;
        }
        // If shadows, move update time, rotate shadows, and update tinting
        time++;
        for (ShadowModel shadow : shadows) {
            if (isNight) {
                continue;
            }
            shadow.updateTinting(backgroundColor);
            shadow.rotateDirection(360f / dayLength);
        }
    }

    /**
     * Resets the ShadowController
     */
    public void reset() {
        time = 0;
        isNight = false;
        shadows.clear();
    }

    /**
     * Draws all shadows to the canvas only when it is not night
     * @param canvas Drawing context
     */
    public void drawShadows(GameCanvas canvas) {
        if(isNight) {
            return;
        }
        //fb.begin();
        for(ShadowModel shadow: shadows) {
            shadow.preDraw(canvas);
        }
        //fb.end();
    }

    /** Returns if the time of day is night **/
    public static boolean isNight() {
        return isNight;
    }

    /**
     * Animates the shadows to spin, starting at beginningTimeRatio, ending at endTimeRatio
     * @param framesIntoAnimation How far are we into the animation?
     * @param animationLength Total length of animation
     */
    public void animateFastForward(float framesIntoAnimation, float animationLength) {
        timeRatio = beginningTimeRatio + (endTimeRatio - beginningTimeRatio) * framesIntoAnimation / animationLength;
        time = (int) (timeRatio * fullDayLength);
        if (time > dayLength) {
            isNight = true;
        }
        for (ShadowModel shadow : shadows) {
            if (isNight) {
                continue;
            }
            shadow.rotateDirection(720 / animationLength * (endTimeRatio - beginningTimeRatio));
        }
    }

    public void forwardTimeRatio(float amount) {
        if(doShadowsMove) {
            amount = 1 - timeRatio;
        }
        beginningTimeRatio = timeRatio;
        endTimeRatio = timeRatio + amount;
    }
}
