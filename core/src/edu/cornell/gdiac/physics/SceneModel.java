package edu.cornell.gdiac.physics;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.enemy.Enemy;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import edu.cornell.gdiac.physics.obstacle.WheelObstacle;
import edu.cornell.gdiac.physics.player.UrsaModel;
import edu.cornell.gdiac.physics.shadows.ShadowController;
import edu.cornell.gdiac.physics.shadows.ShadowModel;
import edu.cornell.gdiac.physics.tree.Tree;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.PooledList;

public class SceneModel extends WorldController implements ContactListener {

    /*
     * Initialize the global blob shadow used for Ursa and enemies.
     * Since it's a shared texture, we can just use it statically across everything to make it easier.
     */

    private static final int BLOB_SHADOW_RESOLUTION = 1024; // High resolution for lesser edge cuts (in theory that is)

    public static final Texture BLOB_SHADOW_TEXTURE;

    static {
        Pixmap pixmap = new Pixmap(BLOB_SHADOW_RESOLUTION * 2, BLOB_SHADOW_RESOLUTION * 2, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        int xcenter = pixmap.getWidth() / 2;
        int ycenter = pixmap.getHeight() / 2;
        pixmap.fillCircle(xcenter, ycenter, BLOB_SHADOW_RESOLUTION);
        BLOB_SHADOW_TEXTURE = new Texture(pixmap);
        pixmap.dispose();
    }

    /** Texture asset for character avatar */
    private TextureRegion avatarTexture;
    private TextureRegion ursaTexture;
    private TextureRegion enemyTexture;
    private TextureRegion enemyTexture2;
    private FilmStrip salmonFilmStrip;
    /** Texture asset for the spinning barrier */
    private TextureRegion barrierTexture;
    /** Texture asset for the bullet */
    private TextureRegion bulletTexture;
    /** Texture asset for the bridge plank */
    private TextureRegion bridgeTexture;
    /** Texture asset for the shadows */
    private TextureRegion shadowTexture;
    private TextureRegion tundraTree;
    private TextureRegion tundraTreeWithSnow;
    private TextureRegion backGround;

    /** The jump sound.  We only want to play once. */
    private Sound jumpSound;
    private long jumpId = -1;
    /** The weapon fire sound.  We only want to play once. */
    private Sound fireSound;
    private long fireId = -1;
    /** The weapon pop sound.  We only want to play once. */
    private Sound plopSound;
    private long plopId = -1;
    /** The default sound volume */
    private float volume;

    // Physics objects for the game
    /** Physics constants for initialization */
    private JsonValue constants;
    /** Reference to the character avatar */
    private UrsaModel avatar;
    /** List of references to enemies */
    private Enemy[] enemies = new Enemy[20];

    /**
     * List of references to all shadows.
     */
    private static PooledList<ShadowModel> shadows = new PooledList<>();

    /**
     * List of all references to all trees
     */
    private PooledList<Tree> trees = new PooledList<>();

    private float timer = 0;
    private TextureRegion playerWalkTextureScript;
    private TextureRegion playerIdleTextureScript;
    private TextureRegion salmonUprightWalkScript;
    private FilmStrip playerWalkFilm;
    private FilmStrip salmonUprightWalkFilm;
    private FilmStrip playerIdleFilm;

    private int playerWalkAnimIndex = 0;
    private int playerIdleAnimIndex =0;
    private int salmonWalkAnimIndex = 0;
    /** Reference to the goalDoor (for collision detection) */
    private BoxObstacle goalDoor;
    /** Controller for all dynamic shadows */
    private ShadowController shadowController;

    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    /**
     * Creates and initialize a new instance of the platformer game
     *
     * The game has default gravity and other settings
     */
    public SceneModel() {
        super(DEFAULT_WIDTH,DEFAULT_HEIGHT,DEFAULT_GRAVITY);
        setDebug(false);
        setComplete(false);
        setFailure(false);
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();

    }
    /**
     * Gather the assets for this controller.
     *
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     *
     * @param directory	Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        avatarTexture  = new TextureRegion(directory.getEntry("platform:dude", Texture.class));
        ursaTexture = new TextureRegion(directory.getEntry("platform:ursa", Texture.class));
        enemyTexture = new TextureRegion(directory.getEntry("platform:enemy", Texture.class));
        enemyTexture2 = new TextureRegion(directory.getEntry("platform:enemy2", Texture.class));
        barrierTexture = new TextureRegion(directory.getEntry("platform:barrier",Texture.class));
        bulletTexture = new TextureRegion(directory.getEntry("platform:bullet",Texture.class));
        bridgeTexture = new TextureRegion(directory.getEntry("platform:rope",Texture.class));
        shadowTexture = new TextureRegion(directory.getEntry("platform:shadow",Texture.class));
        backGround = new TextureRegion(directory.getEntry("platform:snowback",Texture.class));
        tundraTree = new TextureRegion(directory.getEntry("object:tundra_tree",Texture.class));
        tundraTreeWithSnow = new TextureRegion(directory.getEntry("object:tundra_tree_with_snow", Texture.class));

        playerWalkTextureScript = new TextureRegion(directory.getEntry("player:ursaWalk",Texture.class));
        playerWalkFilm = new FilmStrip(playerWalkTextureScript.getTexture(),2,8);
        playerWalkFilm.setFrame(0);

        playerIdleTextureScript = new TextureRegion(directory.getEntry("player:ursaIdle",Texture.class));
        playerIdleFilm = new FilmStrip(playerIdleTextureScript.getTexture(),4,8);
        playerIdleFilm.setFrame(0);

        salmonUprightWalkScript = new TextureRegion(directory.getEntry("enemies:salmonUprightWalk",Texture.class));
        salmonUprightWalkFilm = new FilmStrip(salmonUprightWalkScript.getTexture(),3,8);
        salmonUprightWalkFilm.setFrame(0);










        jumpSound = directory.getEntry( "platform:jump", Sound.class );
        fireSound = directory.getEntry( "platform:pew", Sound.class );
        plopSound = directory.getEntry( "platform:plop", Sound.class );

        constants = directory.getEntry( "platform:constants", JsonValue.class );
        super.gatherAssets(directory);
    }

    /**
     * Resets the status of the game so that we can play again.
     *
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        Vector2 gravity = new Vector2(0,0 );

        for(Obstacle obj : objects) {
            obj.deactivatePhysics(world);
        }
        objects.clear();
        addQueue.clear();
        world.dispose();

        world = new World(gravity,false);
        world.setContactListener(this);
        setComplete(false);
        setFailure(false);
        populateLevel();
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        // Add level goal
        float dwidth  = goalTile.getRegionWidth()/scale.x;
        float dheight = goalTile.getRegionHeight()/scale.y;

//        JsonValue goal = constants.get("goal");
//        JsonValue goalpos = goal.get("pos");
//        goalDoor = new BoxObstacle(goalpos.getFloat(0),goalpos.getFloat(1),dwidth,dheight);
//        goalDoor.setBodyType(BodyDef.BodyType.StaticBody);
//        goalDoor.setDensity(goal.getFloat("density", 0));
//        goalDoor.setFriction(goal.getFloat("friction", 0));
//        goalDoor.setRestitution(goal.getFloat("restitution", 0));
//        goalDoor.setSensor(true);
//        goalDoor.setDrawScale(scale);
//        goalDoor.setTexture(goalTile);
//        goalDoor.setName("goal");
//        addObject(goalDoor);

        // create shadow (idk if this does anything even)
        shadowController = new ShadowController();

        String wname = "wall";
        JsonValue walljv = constants.get("walls");
        JsonValue defaults = constants.get("defaults");
        for (int ii = 0; ii < walljv.size; ii++) {
            PolygonObstacle obj;
            obj = new PolygonObstacle(walljv.get(ii).asFloatArray(), 0, 0);
            obj.setBodyType(BodyDef.BodyType.StaticBody);
            obj.setDensity(defaults.getFloat( "density", 0.0f ));
            obj.setFriction(defaults.getFloat( "friction", 0.0f ));
            obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
            obj.setDrawScale(scale);
            obj.setTexture(earthTile);
            obj.setName(wname+ii);
            addObject(obj);
        }

        String pname = "platform";
        JsonValue platjv = constants.get("platforms");
        for (int ii = 0; ii < platjv.size; ii++) {
            PolygonObstacle obj;
            obj = new PolygonObstacle(platjv.get(ii).asFloatArray(), 0, 0);
            obj.setBodyType(BodyDef.BodyType.StaticBody);
            obj.setDensity(defaults.getFloat( "density", 0.0f ));
            obj.setFriction(defaults.getFloat( "friction", 0.0f ));
            obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
            obj.setDrawScale(scale);
            obj.setTexture(ursaTexture);
            obj.setName(pname+ii);
            addObject(obj);
        }

        // This world is heavier
        //world.setGravity( new Vector2(0,defaults.getFloat("gravity",0)) );

        // Create ursa
        dwidth  = ursaTexture.getRegionWidth()/750f;
        dheight = ursaTexture.getRegionHeight()/750f;
        avatar = new UrsaModel(constants.get("ursa"), dwidth, dheight);
        avatar.setDrawScale(scale);

        avatar.setTexture(playerWalkFilm);
        addObject(avatar);

        //create enemy
        dwidth  = enemyTexture.getRegionWidth()/scale.x;
        dheight = enemyTexture.getRegionHeight()/scale.y;
        enemies[0] = new Enemy(constants.get("enemy"), dwidth, dheight);
        enemies[0].setLookDirection(1, 0);
        enemies[0].setDrawScale(scale);
        enemies[0].setTexture(salmonUprightWalkFilm);
        addObject(enemies[0]);

        dwidth  = enemyTexture2.getRegionWidth()/30;
        dheight = enemyTexture2.getRegionHeight()/30;
        enemies[1] = new Enemy(constants.get("enemy2"), dwidth, dheight);
        enemies[1].setLookDirection(1, 0);
        enemies[1].setDrawScale(scale);
        enemies[1].setTexture(salmonUprightWalkFilm);
        addObject(enemies[1]);

        String tname = "tree";
        JsonValue treejv = constants.get("trees");
        for(int ii = 0; ii < treejv.size; ii++) {
            Tree obj = new Tree(treejv.get(ii).asFloatArray(),3,3);
            obj.setBodyType(BodyDef.BodyType.StaticBody);
            obj.setDensity(defaults.getFloat( "density", 0.0f ));
            obj.setFriction(defaults.getFloat( "friction", 0.0f ));
            obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
            obj.setDrawScale(scale);
            obj.setTexture(tundraTreeWithSnow);
            obj.setName(tname+ii);
            addObject(obj);
            trees.add(obj);
            shadows.add(new ShadowModel(new Vector2(obj.getX(), obj.getY()), Tree.X_SCALE, Tree.Y_SCALE,
                obj.getTexture(), obj.getDrawOrigin(), obj.getDrawScale()));
        }

        volume = constants.getFloat("volume", 1.0f);
    }

    /**
     * Returns whether to process the update loop
     *
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode.  If not, the update proceeds
     * normally.
     *
     * @param dt	Number of seconds since last animation frame
     *
     * @return whether to process the update loop
     */
    public boolean preUpdate(float dt) {
        if (!super.preUpdate(dt)) {
            return false;
        }

        if (!isFailure() && avatar.getY() < -1) {
            setFailure(true);
            return false;
        }

        return true;
    }
    private void animateEnemies(){
        if(salmonWalkAnimIndex == 0 || salmonWalkAnimIndex == 21){
            salmonWalkAnimIndex = 0;
            salmonUprightWalkFilm.setFrame(0);
            enemies[1].setTexture(salmonUprightWalkFilm);
            enemies[0].setTexture(salmonUprightWalkFilm);
            salmonWalkAnimIndex +=1;
//            System.out.println(salmonWalkAnimIndex);
        }
        else {
            salmonUprightWalkFilm.setFrame(salmonWalkAnimIndex);
            salmonWalkAnimIndex +=1;
            enemies[1].setTexture(salmonUprightWalkFilm);
            enemies[0].setTexture(salmonUprightWalkFilm);
        }
    }
    /*
    Animates the player model based on the conidtions of the player
     */
    private void animatePlayerModel(){
        if(avatar.getXMovement() != 0 || avatar.getyMovement() != 0){
            playerIdleAnimIndex = 0;
            if(playerWalkAnimIndex == 0 || playerWalkAnimIndex == 16){
                playerWalkFilm.setFrame(0);
                playerWalkAnimIndex = 0;
                avatar.setTexture(playerWalkFilm);
                playerWalkAnimIndex += 1;
            }
            else {
                playerWalkFilm.setFrame(playerWalkAnimIndex);
                playerWalkAnimIndex +=1;
                avatar.setTexture(playerWalkFilm);
            }
        } else if (avatar.getXMovement() == 0 || avatar.getyMovement() == 0) {
            playerWalkAnimIndex = 0;
            if(playerIdleAnimIndex == 0 || playerIdleAnimIndex == 32){
                playerIdleAnimIndex = 0;
                playerIdleFilm.setFrame(0);
                avatar.setTexture(playerIdleFilm);
                playerIdleAnimIndex += 1;
            }
            else {
                playerIdleFilm.setFrame(playerIdleAnimIndex);
                playerIdleAnimIndex += 1;
                avatar.setTexture(playerIdleFilm);
            }

        }


    }

    /**
     * The core gameplay loop of this world.
     *
     * This method contains the specific update code for this mini-game. It does
     * not handle collisions, as those are managed by the parent class WorldController.
     * This method is called after input is read, but before collisions are resolved.
     * The very last thing that it should do is apply forces to the appropriate objects.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void update(float dt) {
        // Process actions in object model
        float xVal = InputController.getInstance().getHorizontal() *avatar.getForce();
        float yVal = InputController.getInstance().getVertical() *avatar.getForce();
        timer += 1;
        avatar.setMovement(xVal,yVal);
        // avatar.setJumping(InputController.getInstance().didPrimary());
        //avatar.setShooting(InputController.getInstance().didSecondary());

        animatePlayerModel();
        animateEnemies();

        // Shake trees
        if (InputController.getInstance().didInteract()) {
            for (Tree tree : trees) {
                if (tree.canShake() && avatar.getPosition().dst(tree.getPosition()) < 2) {
                    tree.putOnShakeCooldown();
                    System.out.println("Tree shaken");
                    break;
                }
            }
        }

        avatar.applyForce();
        enemies[0].applyForce();
        enemies[1].applyForce();

        if (avatar.isJumping()) {
            jumpId = playSound( jumpSound, jumpId, volume );
        }
        enemies[0].setAlerted(enemies[0].isPlayerInLineOfSight(world, avatar));
        enemies[1].setAlerted(enemies[1].isPlayerInLineOfSight(world, avatar));
//        if (avatar.isJumping()) {
//            jumpId = playSound( jumpSound, jumpId, volume );
//        }

        canvas.clear();
        shadowController.update(this);
    }

    /**
     * Add a new bullet to the world and send it in the right direction.
     */
    private void createBullet() {
        JsonValue bulletjv = constants.get("bullet");
        float offset = bulletjv.getFloat("offset",0);
        offset *= (avatar.isFacingRight() ? 1 : -1);
        float radius = bulletTexture.getRegionWidth()/(2.0f*scale.x);
        WheelObstacle bullet = new WheelObstacle(avatar.getX()+offset, avatar.getY(), radius);

        bullet.setName("bullet");
        bullet.setDensity(bulletjv.getFloat("density", 0));
        bullet.setDrawScale(scale);
        bullet.setTexture(bulletTexture);
        bullet.setBullet(true);
        bullet.setGravityScale(0);

        // Compute position and velocity
        float speed = bulletjv.getFloat( "speed", 0 );
        speed  *= (avatar.isFacingRight() ? 1 : -1);
        bullet.setVX(speed);
        addQueuedObject(bullet);

        fireId = playSound( fireSound, fireId );
    }

    /**
     * Remove a new bullet from the world.
     *
     * @param  bullet   the bullet to remove
     */
    public void removeBullet(Obstacle bullet) {
        bullet.markRemoved(true);
        plopId = playSound( plopSound, plopId );
    }


    /**
     * Callback method for the start of a collision
     *
     * This method is called when we first get a collision between two objects.  We use
     * this method to test if it is the "right" kind of collision.  In particular, we
     * use it to test if we made it to the win door.
     *
     * @param contact The two bodies that collided
     */
    public void beginContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        try {
            Obstacle bd1 = (Obstacle)body1.getUserData();
            Obstacle bd2 = (Obstacle)body2.getUserData();

            // Test bullet collision with world
            if (bd1.getName().equals("bullet") && bd2 != avatar) {
                removeBullet(bd1);
            }

            if (bd2.getName().equals("bullet") && bd1 != avatar) {
                removeBullet(bd2);
            }

            // See if we have landed on the ground.
            if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                    (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
                avatar.setGrounded(true);
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground
            }

            // Check for win condition
            if ((bd1 == avatar   && bd2 == goalDoor) ||
                    (bd1 == goalDoor && bd2 == avatar)) {
                setComplete(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Callback method for the start of a collision
     *
     * This method is called when two objects cease to touch.  The main use of this method
     * is to determine when the characer is NOT on the ground.  This is how we prevent
     * double jumping.
     */
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
            }
        }
    }

    public static PooledList<ShadowModel> getShadows() {
        return shadows;
    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {}

    /**
     * Called when the Screen is paused.
     *
     * We need this method to stop all sounds when we pause.
     * Pausing happens when we switch game modes.
     */
    public void pause() {
        // jumpSound.stop(jumpId);
        plopSound.stop(plopId);
        fireSound.stop(fireId);
    }

    @Override
    public void preDraw(float dt) {
        shadowController.drawAllShadows(canvas, this);
    }
}
