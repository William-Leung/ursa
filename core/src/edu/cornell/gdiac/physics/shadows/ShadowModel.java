package edu.cornell.gdiac.physics.shadows;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.math.*;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import edu.cornell.gdiac.physics.obstacle.SimpleObstacle;

/**
 * Dynamic (currently only rectangular) shadows for the current map
 */
public class ShadowModel extends PolygonObstacle {

    private static final TextureRegion SHADOW_TEXTURE;

    static {
        Pixmap redPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        redPixmap.setColor(new Color(0, 0, 0, 0.5f));
        redPixmap.fill();
        Texture redTexture = new Texture(redPixmap);
        SHADOW_TEXTURE = new TextureRegion(redTexture);
    }

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
    /** The physics shape of this object */
    private PolygonShape sensorShape;

    private Vector2 sensorCenter;

    private float sx;
    private float sy;
    private Vector2 drawScale = new Vector2();
    private Vector2 origin = new Vector2();

    private boolean playerCurrentInShadow;

    private static class ShadowCallback implements RayCastCallback {
        /**
         * The targeted body by the line-of-sight raycast
         */
        private final Body target;

        /**
         * The indication if the body was hit or not.
         */
        private boolean hitPlayer = false;


        /**
         * Constructs a new ShadowCallback object used for raycasting.
         * @param target
         */
        private ShadowCallback(Body target) {
            this.target = target;
        }

        @Override
        public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            Body body = fixture.getBody();

            if (body.getType() == BodyDef.BodyType.StaticBody) { // For simplicity's sake, we're considering all static bodies to be obstacles
                hitPlayer = false;
                return 0;
            } else if (body.equals(target)) { // The body is not static? Might be our target. Let's check to see if it is.
                hitPlayer = true;
                return -1;
            }
            return -1; // Otherwise, ignore the fixture and continue
        }
    }


        /**
         * The direction that the shadow is facing.
         * Invariant: This vector is always normalized.
         */
    private Vector2 direction = new Vector2(0, 1);


    public Vector2 getTopLeft() { return top_left; }

    public Vector2 getBottomRight() { return bottom_right; }

    public Vector2 getAnchor() { return shadow_anchor; }

    public void setTopLeft(Vector2 newCoord) { top_left.set(newCoord); }

    public void setBottomRight(Vector2 newCoord) { bottom_right.set(newCoord); }

    public void setDirection (Vector2 newDirec) { direction.set(newDirec); }

    public static float[] ShadowPolygon(String texture) {
        float[] tree = new float[] {
                0.0f, -4.0f, -4.0f, -1.0f, -6.0f, 0.6f, 0.0f, 20.0f, 6.0f, 0.6f, 4.0f, -1.0f
        };
        float[] cave = new float[] {
                0, -2f,
                -2f, 0,
                -3f, 2f,
                0, 4f,
                3f, 2f,
                3f, 0f,
        };
        float[] rock = new float[] {
                0, -1f,
                -2f, -0.25f,
                -2.5f, 3f,
                0, 5f,
                2.5f, 3f,
                2f, -0.25f,
        };
        switch (texture) {
            case "tree":
                return tree;
            case "cave":
                return cave;
            case "rock":
                return rock;
            default:
                return tree;
        }
    }

    public ShadowModel(Vector2 anchor, float sx, float sy, String texture, Vector2 textureOrigin, Vector2 drawScale) {
        super(ShadowPolygon(texture), anchor.x, anchor.y);
        this.initial_height = 0;
        this.initial_width = 0;
        this.shadow_anchor = anchor;

        this.sx = sx;
        this.sy = sy;
        setDrawScale(drawScale);
        setSensor(true);
        this.origin.set(textureOrigin);
        setTexture(SHADOW_TEXTURE);
        setName("shadow");
    }

    public void reset() {
        this.setHeight(initial_height);
        this.setWidth(initial_width);
    }

//    @Override
//    public void createFixtures() {
//        // Ground Sensor
//        // -------------
//        // We only allow the dude to jump when he's on the ground.
//        // Double jumping is not allowed.
//        //
//        // To determine whether or not the dude is on the ground,
//        // we create a thin sensor under his feet, which reports
//        // collisions with the world but has no collision response.
//        sensorCenter = new Vector2(0, 3);
//        FixtureDef sensorDef = new FixtureDef();
//        sensorShape = new PolygonShape();
//        sensorShape.setAsBox(1, 3, sensorCenter, 0.0f);
//        sensorDef.shape = sensorShape;
//
//        // Ground sensor to represent our feet
//        Fixture sensorFixture = body.createFixture( sensorDef );
//        sensorFixture.setUserData("shadow");
//        setSensor(true);
//        setBodyType(BodyDef.BodyType.StaticBody);
//    }

    /**
     * Rotates the direction of the shadow by the given degrees. These rotations are counterclockwise.
     * @param degrees The amount of degrees to rotate the shadow.
     */
    public void rotateDirection(float degrees) {
        direction.rotateDeg(degrees).nor();
        Transform transform = body.getTransform();
        body.setTransform(transform.getPosition(), transform.getRotation() + (float) Math.toRadians(degrees));
    }

    /**
     * Gets the direction of the shadow. Modifying this vector can have unintended behavior. If you need to modify it for external use,
     * use {@link Vector2#cpy()} or {@link Vector2#Vector2(Vector2)} to construct a copy of the direction.
     * @return The direction of the shadow.
     */
    public Vector2 getDirection() {
        return direction;
    }

    public void setTextureOrigin(Vector2 origin) {
        this.origin.set(origin);
    }

    public void setScale(float sx, float sy) {
        this.sx = sx;
        this.sy = sy;
    }

    @Override
    public void setDrawScale(Vector2 scale) {
        super.setDrawScale(scale);
        this.drawScale.set(scale);
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

    public boolean isPlayerInShadow(World world, SimpleObstacle player) {
        Vector2 pos = getAnchor();
        Vector2 playerPos = new Vector2(player.getPosition());
        double dst = playerPos.dst(pos);

        Vector2 dirToVector = new Vector2(player.getPosition()).sub(pos).nor();
        float angle = getDirection().angleDeg(dirToVector);
        boolean possiblyVisible = dst <= 5 && (angle <= 20 || angle >= 360 - 20);

        if (possiblyVisible) {
            ShadowCallback callback = new ShadowCallback(player.getBody());
            world.rayCast(callback, getAnchor(), player.getPosition());
            if (callback.hitPlayer) {
                playerCurrentInShadow = true;
                return true;
            }
        }
        playerCurrentInShadow = false;
        return false;

    }

    @Override
    public void preDraw(GameCanvas canvas) {
        super.draw(canvas);
    }

    @Override
    public void draw(GameCanvas canvas) {

    }

//    /**
//     * Draws the shadow onto the given GameCanvas with the specified max skew and y-scalar
//     * @param canvas The GameCanvas to draw to
//     * @param xSkew The maximum skew that this shadow can have.
//     * @param yScalar The maximum y-scaling this shadow can have.
//     */
//    public void draw(GameCanvas canvas, float xSkew, float yScalar) {
//        Affine2 affine = new Affine2()
//            .setToTranslation(shadow_anchor.x * drawScale.x, shadow_anchor.y * drawScale.y)
//            .scale(sx, sy * yScalar * direction.y)
//            .shear(xSkew * direction.x, 0);
//        canvas.draw(texture, new Color(0, 0, 0, 127), origin.x, origin.y, affine);
//    }

}
