package edu.cornell.gdiac.physics.shadows;

import com.badlogic.gdx.graphics.Texture;
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
    /** Time in terms of number of update loops
     *  At 60 updates/second, time % 60 is the number of seconds elapsed
     */
    private int time;
    /** Length of the day (dayLength % 60 is the number of seconds) */
    private final int dayLength;
    /** Length of the night */
    private final int nightLength;
    /** Length of a single day/night cycle */
    private final int fullDayLength;
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

    /**
     * Empty constructor
     */
    public ShadowController() {
        this(null);
    }

    /** Creates and initializes a new instance of a shadow controller
     *
     * The shadow controller has a time of 0 ticks */
    public ShadowController(TextureRegion region) {
        time = 0;
        isNight = false;
        shadowTexture = region;

        dayLength = 1800;
        nightLength = 1800;
        fullDayLength = dayLength + nightLength;
    }

    /**
     *
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
     * Rotates all the shadows every update loop
     */
    public void rotateShadows() {
        for (ShadowModel shadow : shadows) {
            if (isNight) {
                continue;
            }
            shadow.rotateDirection(360f / dayLength);
        }
    }

    public void update() {
        if (time == dayLength) {
            isNight = true;
        } else if (time == fullDayLength) {
            for(ShadowModel shadow: shadows) {
                shadow.setDirection(starting_direction);
            }
            time = 0;
            isNight = false;
        }
        time++;
        timeRatio = (float) time / fullDayLength;
        rotateShadows();
    }

    /**
     * Resets the ShadowController
     */
    public void reset() {
        this.time = 0;
        this.isNight = false;
        shadows.clear();
    }

    /**
     * Draws all shadows to the canvas
     * @param canvas Drawing context
     */
    public void drawShadows(GameCanvas canvas) {
        if(isNight) {
            return;
        }
        for(ShadowModel shadow: shadows) {
            shadow.preDraw(canvas);
        }
    }

    /** An isNight getter to access the time of night from within ShadowController **/
    public static boolean isNight() { return isNight; }

    /** Sets the time of day */
    public void setTime(int time) { this.time = time % 3600; }

    /** Gets the time of day */
    public int getTime() { return this.time; }
}
