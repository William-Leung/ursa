package edu.cornell.gdiac.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TideMapLoader;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.cave.Cave;
import edu.cornell.gdiac.physics.enemy.Enemy;
import edu.cornell.gdiac.physics.obstacle.*;
import edu.cornell.gdiac.physics.gameobjects.House;
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
import java.util.LinkedList;
import com.badlogic.gdx.maps.tiled.TiledMap;

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
    private float maxY;
    private TextureRegion enemyTexture;
    private TextureRegion enemyTexture2;
    private TextureRegion[] tileTextures = new TextureRegion[16];
    private FilmStrip salmonFilmStrip;
    private float playerStartX;
    private float playerStartY;
    private float tileY;
    private float tileX;
    private float tileWidth;
    private float tileHeight;
    private float timeRatio;

    private JsonReader json;
    private JsonValue jsonData;



    private OrthogonalTiledMapRenderer renderer;
    /** Texture asset for the spinning barrier */
    private TextureRegion barrierTexture;
    /** Texture asset for the bullet */
    private TextureRegion bulletTexture;
    private TextureRegion[] backgroundTextures = new TextureRegion[3];

    /** Texture asset for the bridge plank */
    private TextureRegion bridgeTexture;
    /** Texture asset for the shadows */
    private TextureRegion shadowTexture;
    private TextureRegion tundraTree;
    private TextureRegion tundraTreeWithSnow;
    private TextureRegion tundraTreeShadow;
    private TextureRegion tundraCaveShadow;

    private TextureRegion polarHouse;
    private TextureRegion polarRock1;
    private TextureRegion polarRock2;
    private TextureRegion polarRock3;
    private TextureRegion polarRock4;
    private TextureRegion polarTrunk1;
    private TextureRegion polarTrunk2;

    private TextureRegion polarHouseShadow;
    private TextureRegion polarRock1Shadow;
    private TextureRegion polarRock2Shadow;
    private TextureRegion polarRock3Shadow;
    private TextureRegion polarRock4Shadow;
    private TextureRegion polarTrunk1Shadow;
    private TextureRegion polarTrunk2Shadow;

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
    /** List of AIControllers */
    private LinkedList<AIController> controls = new LinkedList<>();

    /**
     * List of references to all shadows.
     */
    private PooledList<ShadowModel> shadows = new PooledList<>();

    /**
     * List of all references to all trees
     */
    private PooledList<Tree> trees = new PooledList<>();

    /**
     * List of all references to all houses
     */
    private PooledList<House> houses = new PooledList<>();

    private float timer = 0;
    private TextureRegion playerWalkTextureScript;
    private TextureRegion playerIdleTextureScript;
    private TextureRegion salmonUprightWalkScript;
    private TextureRegion salmonConfusedScript;
    private TextureRegion salmonIdleScript;
    private TextureRegion salmonDetectedScript;
    private TextureRegion polarCave;
    private TextureRegion whiteTexture;
    private FilmStrip playerWalkFilm;
    private FilmStrip salmonUprightWalkFilm;
    private FilmStrip playerIdleFilm;
    private FilmStrip salmonConfusedFilm;
    private FilmStrip salmonIdleFilm;
    private FilmStrip salmonDetectedFilm;
    private int playerWalkAnimIndex = 0;
    private int playerIdleAnimIndex =0;
    private int salmonWalkAnimIndex = 0;
    private int salmonConfusedAnimIndex = 0;
    private int salmonIdleAnimIndex = 0;
    private int salmonDetectedIndex = 0;
    /** Reference to the goalDoor (for collision detection) */
    private BoxObstacle goalDoor;
    /** Controller for all dynamic shadows */
    private ShadowController shadowController;

    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;
    protected Color backgroundColor = Color.BLACK;

    private final Color[] colors;
    private float[] intervals = {0f,0.03f,0.06f,0.08f,0.09f,0.10f,0.2f,0.8f,1f};

    private int nextPointer = 1;


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
        json = new JsonReader();

        jsonData = json.parse(Gdx.files.internal("level2.json"));
        tileHeight = jsonData.get("layers").get(0).get(1).asFloat();
        tileWidth = jsonData.get("layers").get(0).get(7).asFloat();
        playerStartX = jsonData.get("layers").get(1).get("objects").get(0).get(8).asFloat();
        playerStartY = jsonData.get("layers").get(1).get("objects").get(0).get(9).asFloat();
        maxY = (jsonData.get("layers").get(0).get("height").asFloat()) * 512f;
        playerStartY = 1- (playerStartY/maxY);
        playerStartX = (playerStartX/(tileWidth * 512f));
        tileX = tileWidth * 7f;
        tileY = tileHeight * 7f;

        colors = new Color[9];
        colors[0] = new Color(0f,0f,0f,0.7f);
        colors[1] = new Color(0.486f, 0.435f, 0.467f,0.7f);
        colors[2] = new Color(0.71f, 0.514f, 0.553f,0.5f);
        colors[3] = new Color(0.898f, 0.596f, 0.608f,0.5f);
        colors[4] = new Color(1f, 0.706f, 0.635f, 0.6f);
        colors[5] = new Color(1f, 0.804f, 0.698f,0.7f);
        colors[6] = new Color(1f,1f,1f,1f);
        colors[7] = new Color(1f,1f,1f,1f);
        colors[8] = new Color(0f,0f,0f,0.8f);
        //backgroundColor = new Color(0.98f,0.55f,0.11f,0.3f);


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
        tundraTreeWithSnow = new TextureRegion(directory.getEntry("object:tundra_tree_snow_small", Texture.class));
        polarHouse = new TextureRegion(directory.getEntry("object:polar_house", Texture.class));
        polarRock1 = new TextureRegion(directory.getEntry("object:polar_rock_1", Texture.class));
        polarRock2 = new TextureRegion(directory.getEntry("object:polar_rock_2", Texture.class));
        polarRock3 = new TextureRegion(directory.getEntry("object:polar_rock_3", Texture.class));
        polarRock4 = new TextureRegion(directory.getEntry("object:polar_rock_4", Texture.class));
        polarTrunk1 = new TextureRegion(directory.getEntry("object:polar_trunk_1", Texture.class));
        polarTrunk2 = new TextureRegion(directory.getEntry("object:polar_trunk_2", Texture.class));

        tundraTreeShadow = new TextureRegion(directory.getEntry("shadows:polar_tree_shadow", Texture.class));
        tundraCaveShadow = new TextureRegion(directory.getEntry("shadows:polar_cave_shadow", Texture.class));
        polarHouseShadow = new TextureRegion(directory.getEntry("shadows:polar_house_shadow", Texture.class));
        polarRock1Shadow = new TextureRegion(directory.getEntry("shadows:polar_rock_1_shadow", Texture.class));
        polarRock2Shadow = new TextureRegion(directory.getEntry("shadows:polar_rock_2_shadow", Texture.class));
        polarRock3Shadow = new TextureRegion(directory.getEntry("shadows:polar_rock_3_shadow", Texture.class));
        polarRock4Shadow = new TextureRegion(directory.getEntry("shadows:polar_rock_4_shadow", Texture.class));
        polarTrunk1Shadow = new TextureRegion(directory.getEntry("shadows:polar_trunk_1_shadow", Texture.class));
        polarTrunk2Shadow = new TextureRegion(directory.getEntry("shadows:polar_trunk_2_shadow", Texture.class));
        tundraTreeShadow.flip(true, true);

        whiteTexture = new TextureRegion(directory.getEntry("object:white", Texture.class));
        playerWalkTextureScript = new TextureRegion(directory.getEntry("player:ursaWalk",Texture.class));
        playerWalkFilm = new FilmStrip(playerWalkTextureScript.getTexture(),2,8);
        playerWalkFilm.setFrame(0);
        polarCave = new TextureRegion(directory.getEntry("object:cave",Texture.class));
        playerIdleTextureScript = new TextureRegion(directory.getEntry("player:ursaIdle",Texture.class));
        playerIdleFilm = new FilmStrip(playerIdleTextureScript.getTexture(),4,8);
        playerIdleFilm.setFrame(0);
        salmonUprightWalkScript = new TextureRegion(directory.getEntry("enemies:salmonUprightWalk",Texture.class));
        salmonUprightWalkFilm = new FilmStrip(salmonUprightWalkScript.getTexture(),3,8);
        salmonUprightWalkFilm.setFrame(0);
        salmonConfusedScript = new TextureRegion(directory.getEntry("enemies:salmonConfused",Texture.class));
        salmonConfusedFilm = new FilmStrip(salmonConfusedScript.getTexture(),4,8);
        salmonUprightWalkFilm.setFrame(0);
        salmonIdleScript = new TextureRegion(directory.getEntry("enemies:salmonIdle",Texture.class));
        salmonIdleFilm = new FilmStrip(salmonIdleScript.getTexture(), 5, 8);
        salmonIdleFilm.setFrame(0);
        salmonDetectedScript = new TextureRegion(directory.getEntry("enemies:salmonDetected",Texture.class));
        salmonDetectedFilm = new FilmStrip(salmonDetectedScript.getTexture(), 4, 8);
        salmonConfusedFilm.setFrame(0);

        gatherTiles(directory);

        jumpSound = directory.getEntry( "platform:jump", Sound.class );
        fireSound = directory.getEntry( "platform:pew", Sound.class );
        plopSound = directory.getEntry( "platform:plop", Sound.class );
        constants = directory.getEntry( "platform:constants", JsonValue.class );
        super.gatherAssets(directory);
    }
    public void gatherTiles(AssetDirectory directory ){
        System.out.println(jsonData.get("tilesets"));
        for(int i = 0; i < jsonData.get("tilesets").size;i++ ){
            System.out.println(jsonData.get("tilesets").size);
        }
        tileTextures[0] = new TextureRegion(directory.getEntry("tiles:polar_middle",Texture.class));
        tileTextures[1] = new TextureRegion(directory.getEntry("tiles:polar_corner_1",Texture.class));
        tileTextures[2] = new TextureRegion(directory.getEntry("tiles:polar_corner_2",Texture.class));
        tileTextures[3] = new TextureRegion(directory.getEntry("tiles:polar_corner_3",Texture.class));
        tileTextures[4] = new TextureRegion(directory.getEntry("tiles:polar_corner_4",Texture.class));
        tileTextures[5] = new TextureRegion(directory.getEntry("tiles:polar_edge_3",Texture.class));
        tileTextures[6] = new TextureRegion(directory.getEntry("tiles:polar_edge_2",Texture.class));
        tileTextures[7] = new TextureRegion(directory.getEntry("tiles:polar_edge_4",Texture.class));
        tileTextures[8] = new TextureRegion(directory.getEntry("tiles:polar_edge_5",Texture.class));
        tileTextures[9] = new TextureRegion(directory.getEntry("tiles:polar_edge_1",Texture.class));
        tileTextures[10] = new TextureRegion(directory.getEntry("tiles:polar_corner_5",Texture.class));
        tileTextures[11] = new TextureRegion(directory.getEntry("tiles:polar_corner_6",Texture.class));
        tileTextures[12] = new TextureRegion(directory.getEntry("tiles:polar_corner_8",Texture.class));
        tileTextures[13] = new TextureRegion(directory.getEntry("tiles:polar_edge_6",Texture.class));
        backgroundTextures[0] = new TextureRegion(directory.getEntry("tiles:polar_flower_1",Texture.class));
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
        shadows.clear();
        addQueue.clear();
        world.dispose();

        //for (AIController c : controls) c.reset();
        controls.clear();

        world = new World(gravity,false);
        world.setContactListener(this);
        setComplete(false);
        setFailure(false);
        populateLevel();
        nextPointer = 1;

    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        tileHeight = jsonData.get("layers").get(0).get(1).asFloat();
        tileWidth = jsonData.get("layers").get(0).get(7).asFloat();
        playerStartX = jsonData.get("layers").get(1).get("objects").get(0).get(8).asFloat();
        playerStartY = jsonData.get("layers").get(1).get("objects").get(0).get(9).asFloat();
        maxY = (jsonData.get("layers").get(0).get("height").asFloat()) * 512f;
        playerStartY = 1- (playerStartY/maxY);
        playerStartX = (playerStartX/(tileWidth * 512f));
        tileX = tileWidth * 7f;
        tileY = tileHeight * 7f;
        // Add level goal
        float dwidth  = goalTile.getRegionWidth()/scale.x;
        float dheight = goalTile.getRegionHeight()/scale.y;


        // create shadow (idk if this does anything even)
        shadowController = new ShadowController();

        String wname = "wall";
        JsonValue walljv = constants.get("walls");
        JsonValue defaults = constants.get("defaults");



        // This world is heavier
        //world.setGravity( new Vector2(0,defaults.getFloat("gravity",0)) );

        // Create ursa
        dwidth  = playerIdleFilm.getRegionWidth()/50f;
        dheight = playerIdleFilm.getRegionHeight()/100f;
        avatar = new UrsaModel(playerStartX * tileX + 5.5f,playerStartY * tileY +11.0f,constants.get("ursa"), dwidth, dheight);
        avatar.setDrawScale(scale);


        avatar.setTexture(playerWalkFilm);
        addObject(avatar);
        dwidth  = enemyTexture.getRegionWidth()/scale.x;
        dheight = enemyTexture.getRegionHeight()/scale.y;

        // place the enemies based on position in Tiled
        System.out.println((jsonData.get("layers").get(6)));
        for(int i = 0; i< jsonData.get("layers").get(6).get("objects").size;i++){
            float x = (jsonData.get("layers").get(6).get("objects").get(i).get(8).asFloat()) / (tileWidth * 512f);
            x = x * tileX + 5.5f;
            float y = (maxY - jsonData.get("layers").get(6).get("objects").get(i).get(9).asFloat())/(tileHeight * 512f);
            y = y * tileY +11.0f;

            JsonValue goal = constants.get("goal");
            JsonValue goalpos = goal.get("pos");
            goalDoor = new Cave(x,y,5,5);
            goalDoor.setBodyType(BodyDef.BodyType.StaticBody);
            goalDoor.setDensity(goal.getFloat("density", 0));
            goalDoor.setFriction(goal.getFloat("friction", 0));
            goalDoor.setRestitution(goal.getFloat("restitution", 0));
            goalDoor.setSensor(true);
            goalDoor.setDrawScale(scale);
            goalDoor.setTexture(polarCave);
            goalDoor.setName("goal");

            ShadowModel caveShadow = new ShadowModel(new Vector2(goalDoor.getX(), goalDoor.getY()), 0.75f, 0.75f,
                    tundraCaveShadow, new Vector2(tundraCaveShadow.getRegionWidth() / 2.0f, 85), scale);
            shadows.add(caveShadow);
            addObject(caveShadow);

            addObject(goalDoor);

        }

        for(int i = 0; i< jsonData.get("layers").get(3).get("objects").size;i++){
            float x = (jsonData.get("layers").get(3).get("objects").get(i).get(8).asFloat()) / (tileWidth * 512f);
            x = x * tileX + 5.5f;
            float y = (maxY - jsonData.get("layers").get(3).get("objects").get(i).get(9).asFloat())/(tileHeight * 512f);
            y = y * tileY +11.0f;

            float direction = 1;
            float maxX = 2500/60;
            float minX = 1250/60;
            enemies[i] = new Enemy(x,y,maxX,minX,constants.get("enemy"), dwidth, dheight);
            enemies[i].setLookDirection(direction, 0);
            enemies[i].setDrawScale(scale);
            enemies[i].setTexture(salmonUprightWalkFilm);
            addObject(enemies[i]);
        }

        //place trees in the level
        JsonValue treejv = constants.get("trees");
        String tname = "tree";
        System.out.println(jsonData.get("layers"));
        for(int i = 0; i < jsonData.get("layers").get(4).get("objects").size; i++){
            float x = (jsonData.get("layers").get(4).get("objects").get(i).get(8).asFloat())/ (tileWidth * 512f);
            x = x * tileX + 5.5f;
            float y = (maxY - jsonData.get("layers").get(4).get("objects").get(i).get(9).asFloat())/(tileHeight * 512f);
            y = y * tileY +11.0f;
            Tree obj = new Tree(treejv.get(0).asFloatArray(),x,y);
            obj.setBodyType(BodyDef.BodyType.StaticBody);
            obj.setDensity(defaults.getFloat( "density", 0.0f ));
            obj.setFriction(defaults.getFloat( "friction", 0.0f ));
            obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
            obj.setDrawScale(scale);
            obj.setTexture(tundraTreeWithSnow);
            obj.setName(tname+i);

            ShadowModel model = new ShadowModel(new Vector2(obj.getX(), obj.getY()), 0.75f, 0.75f,
                tundraTreeShadow, new Vector2(tundraTreeShadow.getRegionWidth() / 2.0f, 85), scale);
            shadows.add(model);
            addObject(model);

            addObject(obj);
            trees.add(obj);
        }

        //place houses in the level
//        JsonValue housejv = constants.get("houses");
//        String hname = "house";
//
//        for(int i = 0; i < jsonData.get("layers").get(7).get("objects").size; i++){
//            float x = (jsonData.get("layers").get(7).get("objects").get(i).get(8).asFloat())/ (tileWidth * 512f);
//            x = x * tileX + 5.5f;
//            float y = (maxY - jsonData.get("layers").get(7).get("objects").get(i).get(9).asFloat())/(tileHeight * 512f);
//            y = y * tileY + 11.0f;
//
//            House obj = new House(housejv.get(0).asFloatArray(),x,y);
//            obj.setBodyType(BodyDef.BodyType.StaticBody);
//            obj.setDensity(defaults.getFloat( "density", 0.0f ));
//            obj.setFriction(defaults.getFloat( "friction", 0.0f ));
//            obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
//            obj.setDrawScale(scale);
//            obj.setTexture(polarHouse);
//            obj.setName(hname+i);
//
//            ShadowModel model = new ShadowModel(new Vector2(obj.getX(), obj.getY()), 0.75f, 0.75f,
//                    polarHouseShadow, new Vector2(polarHouseShadow.getRegionWidth() / 2.0f, 85), scale);
//            shadows.add(model);
//            addObject(model);
//
//            addObject(obj);
//            houses.add(obj);
//        }

        for (int i = 0; i < enemies.length; i++) {
            if (enemies[i] != null) {
                controls.add(new AIController(enemies[i], avatar));
            }
        }


        float[] treeXCoords = new float[]{24,14, 17, 19, 27}; //
        float[] treeYCoords = new float[]{5.5f,3, 12, 6, 11}; //
        for(int ii = 0; ii < treeXCoords.length; ii++) {
            //Tree obj = new Tree(treejv.get(0).asFloatArray(),treeXCoords[ii],treeYCoords[ii]);
            //obj.setBodyType(BodyDef.BodyType.StaticBody);
            //obj.setDensity(defaults.getFloat( "density", 0.0f ));
            //obj.setFriction(defaults.getFloat( "friction", 0.0f ));
            //obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
            //obj.setDrawScale(scale);
            //obj.setTexture(tundraTreeWithSnow);
            //obj.setName(tname+ii);
            //addObject(obj);
            //trees.add(obj);
            //shadows.add(new ShadowModel(new Vector2(obj.getX(), obj.getY()), Tree.X_SCALE, Tree.Y_SCALE,
                  //  obj.getTexture(), obj.getDrawOrigin(), obj.getDrawScale()));
        }

        volume = constants.getFloat("volume", 1.0f);
        drawWalls();
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


    private void drawExtraObjects(){
        canvas.draw(backgroundTextures[0], Color.WHITE,0,0,1000,100,avatar.getAngle(), 0.1f,0.1f);
        for(int i = 0; i < jsonData.get("layers").get(5).get("objects").size;i++){
            float x = (jsonData.get("layers").get(5).get("objects").get(i).get(8).asFloat())/2;
            // System.out.println(x);
            float y = (maxY - jsonData.get("layers").get(5).get("objects").get(i).get(9).asFloat())/1.3f;

            // System.out.println(y);
            canvas.draw(backgroundTextures[0], Color.WHITE,0,0,x,y,avatar.getAngle(), 0.15f,0.15f);
        }
    }
    /**
    Draws all tiles based on the json data from Tiled
     */
    public void drawTiles(){
        int counter = 0;


        for (int i = (int) tileHeight; i >0 ; i--) {

            for(int j = 0; j < tileWidth;j++){



                if(jsonData.get("layers").get(0).get(0).get(counter).asInt() == 1){
                    //tileTextures[0] = new TextureRegion(directory.getEntry("tiles:polar_middle",Texture.class));
                    canvas.draw(tileTextures[0], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);

                } else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 2) {
                    //tileTextures[1] = new TextureRegion(directory.getEntry("tiles:polar_corner_1",Texture.class));


                    canvas.draw(tileTextures[1], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 3) {
                    //tileTextures[2] = new TextureRegion(directory.getEntry("tiles:polar_corner_2",Texture.class));
                    canvas.draw(tileTextures[2], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 4) {
                    //tileTextures[3] = new TextureRegion(directory.getEntry("tiles:polar_corner_3",Texture.class));
                    canvas.draw(tileTextures[3], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 5) {
                    //tileTextures[4] = new TextureRegion(directory.getEntry("tiles:polar_corner_4",Texture.class));
                    canvas.draw(tileTextures[4], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 6) {
                    //tileTextures[6] = new TextureRegion(directory.getEntry("tiles:polar_corner_6",Texture.class));
                    canvas.draw(tileTextures[5], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 7) {
                    //tileTextures[10] = new TextureRegion(directory.getEntry("tiles:polar_edge_2",Texture.class));
                    canvas.draw(tileTextures[6], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 8) {
                    //tileTextures[12] = new TextureRegion(directory.getEntry("tiles:polar_edge_4",Texture.class));

                    canvas.draw(tileTextures[7], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 9) {
                    //tileTextures[14] = new TextureRegion(directory.getEntry("tiles:polar_edge_6",Texture.class));


                    canvas.draw(tileTextures[8], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 10) {
                    //tileTextures[5] = new TextureRegion(directory.getEntry("tiles:polar_corner_5",Texture.class));

                    canvas.draw(tileTextures[9], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 22) {
                    //tileTextures[8] = new TextureRegion(directory.getEntry("tiles:polar_corner_8",Texture.class));

                    canvas.draw(tileTextures[10], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 23) {
                    //tileTextures[9] = new TextureRegion(directory.getEntry("tiles:polar_edge_1",Texture.class));

                    canvas.draw(tileTextures[11], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 24) {
                    //tileTextures[13] = new TextureRegion(directory.getEntry("tiles:polar_edge_5",Texture.class));

                    canvas.draw(tileTextures[12], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 25) {
                    //tileTextures[11] = new TextureRegion(directory.getEntry("tiles:polar_edge_3",Texture.class));

                    canvas.draw(tileTextures[13], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                counter += 1;



            }

            //draw the background objects



        }


    }
    public void drawWalls(){
        int counter = 0;
        InvivisbleWall wall;
        for (int i = (int) tileHeight; i >0 ; i--) {

            for(int j = 0; j < tileWidth;j++){



                if(jsonData.get("layers").get(0).get(0).get(counter).asInt() == 1){
                    //tileTextures[0] = new TextureRegion(directory.getEntry("tiles:polar_middle",Texture.class));


                } else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 2) {
                    //tileTextures[1] = new TextureRegion(directory.getEntry("tiles:polar_corner_1",Texture.class));
                    wall = new InvivisbleWall(4 + (j* 8),0+(i*8),3f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(5.5f + (j* 8),1+(i*8),.3f,2f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(6.5f + (j* 8),2.8f+(i*8),2.5f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);




                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 3) {
                    //tileTextures[2] = new TextureRegion(directory.getEntry("tiles:polar_corner_2",Texture.class));
                    wall = new InvivisbleWall( 1.3f + (j* 8),2.8f+(i*8),3f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(3f + (j* 8),1.4f+(i*8),.3f,2.8f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 4) {
                    //tileTextures[3] = new TextureRegion(directory.getEntry("tiles:polar_corner_3",Texture.class));
                    wall = new InvivisbleWall( 1.3f + (j* 8),5.5f+(i*8),3f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(2.5f + (j* 8),6.8f+(i*8),.3f,2.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 5) {
                    //tileTextures[4] = new TextureRegion(directory.getEntry("tiles:polar_corner_4",Texture.class));
                    wall = new InvivisbleWall( 7.5f + (j* 8),05.5f+(i*8),3f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(5.5f + (j* 8),6.8f+(i*8),.3f,2.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 6) {
                    //tileTextures[6] = new TextureRegion(directory.getEntry("tiles:polar_corner_6",Texture.class));
                    wall = new InvivisbleWall(3f + (j* 8),4f+(i*8),.3f,8f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 7) {
                    //tileTextures[10] = new TextureRegion(directory.getEntry("tiles:polar_edge_2",Texture.class));
                    wall = new InvivisbleWall(2.8f + (j* 8),.8f+(i*8),.3f,1.5f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(2.5f + (j* 8),7f+(i*8),.3f,2f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(3.7f + (j* 8),3.7f+(i*8),.3f,4f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 8) {
                    //tileTextures[12] = new TextureRegion(directory.getEntry("tiles:polar_edge_4",Texture.class));
                    wall = new InvivisbleWall( 3.9f + (j* 8),2.8f+(i*8),8f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 9) {
                    //tileTextures[14] = new TextureRegion(directory.getEntry("tiles:polar_edge_6",Texture.class));
                    wall = new InvivisbleWall( 4.9f + (j* 8),6.8f+(i*8),.3f,2.5f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall( 3.9f + (j* 8),3.8f+(i*8),.3f,2.5f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall( 5.3f + (j* 8),1.1f+(i*8),.3f,2.0f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 10) {
                    //tileTextures[5] = new TextureRegion(directory.getEntry("tiles:polar_corner_5",Texture.class));
                    wall = new InvivisbleWall( 4f + (j* 8),5f+(i*8),8f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 22) {
                    //tileTextures[8] = new TextureRegion(directory.getEntry("tiles:polar_corner_8",Texture.class));
                    wall = new InvivisbleWall( 6.2f +  (j* 8),4.7f+(i*8),3.5f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(3f + (j* 8),1.3f+(i*8),.3f,2.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(3.8f + (j* 8),3.3f+(i*8),.3f,1.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 23) {
                    //tileTextures[9] = new TextureRegion(directory.getEntry("tiles:polar_edge_1",Texture.class));
                    wall = new InvivisbleWall( 5.8f +  (j* 8),3.3f+(i*8),4f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(3f + (j* 8),6.3f+(i*8),.3f,4.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 24) {
                    //tileTextures[13] = new TextureRegion(directory.getEntry("tiles:polar_edge_5",Texture.class));
                    wall = new InvivisbleWall( 2.3f +  (j* 8),4.7f+(i*8),4.5f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall(5.2f + (j* 8),2.5f+(i*8),.3f,4.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == 25) {
                    //tileTextures[11] = new TextureRegion(directory.getEntry("tiles:polar_edge_3",Texture.class));
                    wall = new InvivisbleWall(5.2f + (j* 8),4f+(i*8),.3f,8f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                counter += 1;



            }

            //draw the background objects



        }


    }
    private void animateEnemies(){

        for (AIController i : controls) {
            if (i != null) {
                if (i.isWon()) {
                    salmonDetectedFilm.setFrame(salmonDetectedIndex);
                    i.getEnemy().setTexture(salmonDetectedFilm);
                    salmonDetectedIndex = (salmonDetectedIndex + 1) % 30;
                } else if (i.getEnemy().getVX() == 0 && i.getEnemy().getVY() == 0) {
                    salmonIdleFilm.setFrame(salmonIdleAnimIndex);
                    i.getEnemy().setTexture(salmonIdleFilm);
                    salmonIdleAnimIndex = (salmonIdleAnimIndex + 1) % 40;
                } else if (i.isStunned() || i.isConfused()) { // animate stunned
                    salmonConfusedFilm.setFrame(i.get_confused_anim_index());
                    i.getEnemy().setTexture(salmonConfusedFilm);
                    i.inc_anim_index();
                } else {
                    i.reset_anim_index();
                    salmonUprightWalkFilm.setFrame(salmonWalkAnimIndex);
                    i.getEnemy().setTexture(salmonUprightWalkFilm);
                }
            }
        }
        salmonWalkAnimIndex = (salmonWalkAnimIndex + 1) % 21;


//        if(salmonWalkAnimIndex == 0 || salmonWalkAnimIndex == 21){
//            salmonWalkAnimIndex = 0;
//            salmonUprightWalkFilm.setFrame(0);
//            for (Enemy enemy:enemies) {
//                if(enemy != null){
//                    enemy.setTexture(salmonUprightWalkFilm);
//                }
//
//            }
//            salmonWalkAnimIndex +=1;
////
//        }
//        else {
//            salmonUprightWalkFilm.setFrame(salmonWalkAnimIndex);
//            salmonWalkAnimIndex +=1;
//            for (Enemy enemy:enemies) {
//                if(enemy != null){
//                    enemy.setTexture(salmonUprightWalkFilm);
//                }
//
//            }
//        }
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

        timeRatio = shadowController.getTimeRatio();
        if(timeRatio > 1) {
            nextPointer = 1;
        } else {
            // Update nextPointer to next interval
            if(timeRatio > intervals[nextPointer]) {
                nextPointer++;
            }
            updateBackgroundColor(intervals[nextPointer-1],timeRatio);
        }

        canvas.moveCam(avatar.getPosition().x,avatar.getPosition().y);


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
            Tree nearest = null;
            float dst = 0;
            for (Tree tree : trees) {
                float tempDst = avatar.getPosition().dst(tree.getPosition());
                if (tree.canShake() && tempDst < 2 && (nearest == null || tempDst < dst)) {
                    nearest = tree;
                    dst = tempDst;
                }
            }

            if (nearest != null) {
                shakeTree(nearest);
            }
        }

        if (!isFailure()) {
            avatar.applyForce();
        } else {
            avatar.setVX(0);
            avatar.setVY(0);
        }
        //enemies[0].applyForce();
        //enemies[1].applyForce();

        for (AIController c : controls) {
            c.getAction();
            Enemy thisEnemy = c.getEnemy();
            //thisEnemy.applyForce();
            thisEnemy.setAlerted(thisEnemy.isPlayerInLineOfSight(world, avatar));


            if (c.isWon()) setFailure(true);

        }

        if (avatar.isJumping()) {
            jumpId = playSound( jumpSound, jumpId, volume );
        }

        for (Enemy enemy : enemies) {
            if(enemy != null) {
                if (enemy.isPlayerInLineOfSight(world,avatar)) {
                    enemy.getPlayerPos(avatar.getPosition());
                }
                enemy.setInShadow(avatar.isInShadow());
            }
        }


        canvas.clear();
        shadowController.update(this);

    }


    private void shakeTree(Tree tree) {
        if (tree.canShake()) {
            tree.putOnShakeCooldown();
            System.out.println("Tree shaken");

            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.getPosition().dst(tree.getPosition()) < 5) {
                    enemy.stun();
                }
            }
        }
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
            if ((avatar.getSensorName().equals(fd2) && bd1.getName().equals("shadow")) ||
                    (avatar.getSensorName().equals(fd1) && bd2.getName().equals("shadow"))) {
                avatar.setInShadow(true);
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

        Obstacle bd1 = (Obstacle) body1.getUserData();
        Obstacle bd2 = (Obstacle) body2.getUserData();

        if ((avatar.getSensorName().equals(fd2) && bd1.getName().equals("shadow")) ||
                (avatar.getSensorName().equals(fd1) && bd2.getName().equals("shadow"))) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setInShadow(false);
            }
        }
    }

    public PooledList<ShadowModel> getShadows() {
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

    /**
     * Invariant: intervals.length == colors.length
     * Transitions smoothly between colors[reachPointer] and colors[reachPointer-1] in the time
     * between intervals[reachPointer] and intervals[reachPointer-1]
     * @param timeRatio
     */
    private void updateBackgroundColor(float startTime, float timeRatio) {
        // Prevent IndexOutBoundsException
        //System.out.println("Red: " + (colors[nextPointer - 1].r + (colors[nextPointer].r - colors[nextPointer - 1].r) * (timeRatio - startTime) * 2f));

        backgroundColor.r = colors[nextPointer - 1].r + (colors[nextPointer].r - colors[nextPointer - 1].r) * (timeRatio - startTime) / (intervals[nextPointer] - intervals[nextPointer - 1]);
        backgroundColor.g = colors[nextPointer - 1].g + (colors[nextPointer].g - colors[nextPointer - 1].g) * (timeRatio - startTime) / (intervals[nextPointer] - intervals[nextPointer - 1]);
        backgroundColor.b = colors[nextPointer - 1].b + (colors[nextPointer].b - colors[nextPointer - 1].b) * (timeRatio - startTime) / (intervals[nextPointer] - intervals[nextPointer - 1]);
        backgroundColor.a = colors[nextPointer - 1].a + (colors[nextPointer].a - colors[nextPointer - 1].a) * (timeRatio - startTime) / (intervals[nextPointer] - intervals[nextPointer - 1]);
    }

    @Override
    public void preDraw(float dt) {


        canvas.draw(snowBackGround,backgroundColor,0,0,tileWidth* 256, tileHeight*256);
        drawTiles();
        drawExtraObjects();
        shadowController.drawAllShadows(canvas, this);
    }

    @Override
    public void draw(float dt) {
        super.draw(dt);
        if (complete && !failed) {
            displayFont.setColor(Color.YELLOW);
            canvas.begin(); // DO NOT SCALE
            canvas.drawText("WINNER!",displayFont,avatar.getPosition().x *31.9f, avatar.getPosition().y * 31.9f );

            canvas.end();
        } else if (failed) {
            displayFont.setColor(Color.RED);
            canvas.begin(); // DO NOT SCALE
            canvas.drawText("Loser!",displayFont,avatar.getPosition().x*31.9f, avatar.getPosition().y*31.9f);
            canvas.end();
        }
    }
}
