package edu.cornell.gdiac.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import edu.cornell.gdiac.physics.objects.Cave;
import edu.cornell.gdiac.physics.enemy.Enemy;
import edu.cornell.gdiac.physics.obstacle.*;
import edu.cornell.gdiac.physics.objects.House;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.player.UrsaModel;
import edu.cornell.gdiac.physics.shadows.ShadowController;
import edu.cornell.gdiac.physics.shadows.ShadowModel;
import edu.cornell.gdiac.physics.objects.Tree;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.PooledList;
import java.util.LinkedList;

public class SceneModel extends WorldController implements ContactListener {

    /*
     * Initialize the global blob shadow used for Ursa and enemies.
     * Since it's a shared texture, we can just use it statically across everything to make it easier.
     */

    private static final int BLOB_SHADOW_RESOLUTION = 1024; // High resolution for lesser edge cuts (in theory that is)

    public static final Texture BLOB_SHADOW_TEXTURE;

    static {
        Pixmap pixmap = new Pixmap(BLOB_SHADOW_RESOLUTION * 2, BLOB_SHADOW_RESOLUTION * 2, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0f,0f,0f,0.7f));
        int xcenter = pixmap.getWidth() / 2;
        int ycenter = pixmap.getHeight() / 2;
        pixmap.fillCircle(xcenter, ycenter, BLOB_SHADOW_RESOLUTION);
        BLOB_SHADOW_TEXTURE = new Texture(pixmap);
        pixmap.dispose();
    }

    /** =========== Textures =========== */
    /** Texture asset for day night UI */
    private TextureRegion dayNightUITexture;
    /** Texture asset for bipedal (2 legs) salmon */
    private TextureRegion bipedalSalmonTexture;
    /** Texture asset for swimming salmon */
    private TextureRegion swimmingSalmonTexture;
    /** Texture asset for tint over the whole screen */
    private Texture blankTexture;
    /** Texture asset for a tree without snow in the polar map */
    private TextureRegion polarTreeNoSnow;
    /** Texture asset for a tree with snow in the polar map */
    private TextureRegion polarTreeWithSnow;
    /** Texture asset for a house in the polar map */
    private TextureRegion polarHouse;
    /** Texture asset for the first rock in the polar map */
    private TextureRegion polarRock1;
    /** Texture asset for the second rock in the polar map */
    private TextureRegion polarRock2;
    /** Texture asset for the third rock in the polar map */
    private TextureRegion polarRock3;
    /** Texture asset for the fourth rock in the polar map */
    private TextureRegion polarRock4;
    /** Texture asset for the first trunk in the polar map */
    private TextureRegion polarTrunk1;
    /** Texture asset for the second trunk in the polar map */
    private TextureRegion polarTrunk2;
    /** Texture asset for the cave in the polar map */
    private TextureRegion polarCave;

    /** =========== Shadow Textures =========== */
    /** Texture asset for a tree's shadow in the polar map */
    private TextureRegion polarTreeShadow;
    /** Texture asset for a cave's shadow in the polar map */
    private TextureRegion polarCaveShadow;
    /** Texture asset for a house's shadow in the polar map */
    private TextureRegion polarHouseShadow;
    /** Texture asset for the first rock's shadow in the polar map */
    private TextureRegion polarRock1Shadow;
    /** Texture asset for the second rock's shadow in the polar map */
    private TextureRegion polarRock2Shadow;
    /** Texture asset for the third rock's shadow in the polar map */
    private TextureRegion polarRock3Shadow;
    /** Texture asset for the fourth rock's shadow in the polar map */
    private TextureRegion polarRock4Shadow;
    /** Texture asset for the first trunk's shadow in the polar map */
    private TextureRegion polarTrunk1Shadow;
    /** Texture asset for the second trunk's shadow in the polar map */
    private TextureRegion polarTrunk2Shadow;

    /** =========== Animation Textures =========== */
    /** Texture asset for player walking animation */
    private TextureRegion playerWalkTextureAnimation;
    /** Texture asset for player idle animation */
    private TextureRegion playerIdleTextureAnimation;
    /** Texture asset for salmon walking animation */
    private TextureRegion salmonUprightWalkAnimation;
    /** Texture asset for salmon confused animation */
    private TextureRegion salmonConfusedAnimation;
    /** Texture asset for salmon idle animation */
    private TextureRegion salmonIdleAnimation;
    /** Texture asset for salmon detecting animation */
    private TextureRegion salmonDetectedAnimation;
    /** Texture asset for tree shaking animation */
    private TextureRegion treeShakeAnimation;

    /** =========== Film Strips =========== */
    /** Filmstrip for player walking animation */
    private FilmStrip playerWalkFilm;
    /** Filmstrip for salmon walking animation */
    private FilmStrip salmonUprightWalkFilm;
    /** Filmstrip for player idling animation */
    private FilmStrip playerIdleFilm;
    /** Filmstrip for salmon confused animation */
    private FilmStrip salmonConfusedFilm;
    /** Filmstrip for salmon idling animation */
    private FilmStrip salmonIdleFilm;
    /** Filmstrip for salmon detecting animation */
    private FilmStrip salmonDetectedFilm;
    /** Filmstrip for tree shaking animation */
    private FilmStrip treeShakeFilm;

    private TextureRegion[] backgroundTextures = new TextureRegion[3];

    private float maxY;
    float firstTileIndex;
    private TextureRegion[] tileTextures = new TextureRegion[16];
    private float playerStartX;
    private float playerStartY;
    private float tileY;
    private float tileX;
    private float tileWidth;
    private float tileHeight;
    private float timeRatio;
    private Vector2[] enemyPosList = new Vector2[10];
    private JsonReader json;
    private JsonValue jsonData;

    private Tree shakingTree = null;


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

    private int playerWalkAnimIndex = 0;
    private int playerIdleAnimIndex = 0;
    private int salmonWalkAnimIndex = 0;
    private int salmonConfusedAnimIndex = 0;
    private int salmonIdleAnimIndex = 0;
    private int salmonDetectedIndex = 0;
    private int treeShakeIndex = 0;
    private int framesSinceTreeAnimation = 0;
    /** Reference to the goalDoor (for collision detection) */
    private BoxObstacle goalDoor;
    /** Controller for all dynamic shadows */
    private ShadowController shadowController;


    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;
    /** */
    protected Color backgroundColor = Color.BLACK;

    private final Color[] colors;
    private float[] intervals;

    private int nextPointer = 1;

    private float uiRotationAngle = 0f;
    private float uiYOffset = 0f;
    private float uiRisingDuration = 0.05f;
    private float uiDrawScale = 0.1f;


    /**
     * Creates and initialize a new instance of the platformer game
     *
     * The game has default gravity and other settings
     */
    public SceneModel(String levelJson) {
        super(DEFAULT_WIDTH,DEFAULT_HEIGHT,DEFAULT_GRAVITY);
        setDebug(false);
        setComplete(false);
        setFailure(false);
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();
        json = new JsonReader();
        jsonData = json.parse(Gdx.files.internal(levelJson));
        firstTileIndex = (jsonData.get("layers").get(0).get(0).get(0)).asFloat();

        tileHeight = jsonData.get("layers").get(0).get(1).asFloat();
        tileWidth = jsonData.get("layers").get(0).get(7).asFloat();
        playerStartX = jsonData.get("layers").get(1).get("objects").get(0).get(8).asFloat();
        playerStartY = jsonData.get("layers").get(1).get("objects").get(0).get(9).asFloat();
        maxY = (jsonData.get("layers").get(0).get("height").asFloat()) * 512f;
        playerStartY = 1- (playerStartY/maxY);
        playerStartX = (playerStartX/(tileWidth * 512f));
        tileX = tileWidth * 7f;
        tileY = tileHeight * 7f;

        colors = new Color[4];
        intervals = new float[4];
        // Night
        colors[0] = new Color(0f,0f,0f,0.5f);
        colors[1] = new Color(1f,1f,1f,0f);
        intervals[1] = 0.2f;
        // Maintain the white
        colors[2] = new Color(1f,1f,1f,0f);
        intervals[2] = 0.8f;
// Sunset Colors
        colors[3] = new Color(0f,0f,0f,0.5f);
        intervals[3] = 1f;

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1); // RGBA color with full opacity (white)
        pixmap.fill();
        blankTexture = new Texture(pixmap);
        pixmap.dispose();
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
        dayNightUITexture = new TextureRegion(directory.getEntry("ui:dayNightUI", Texture.class));
        bipedalSalmonTexture = new TextureRegion(directory.getEntry("enemies:bipedalSalmon", Texture.class));
        swimmingSalmonTexture = new TextureRegion(directory.getEntry("enemies:swimmingSalmon", Texture.class));

        polarTreeNoSnow = new TextureRegion(directory.getEntry("object:tundra_tree",Texture.class));
        polarTreeWithSnow = new TextureRegion(directory.getEntry("object:tundra_tree_snow_small", Texture.class));
        polarHouse = new TextureRegion(directory.getEntry("object:polar_house", Texture.class));
        polarRock1 = new TextureRegion(directory.getEntry("object:polar_rock_1", Texture.class));
        polarRock2 = new TextureRegion(directory.getEntry("object:polar_rock_2", Texture.class));
        polarRock3 = new TextureRegion(directory.getEntry("object:polar_rock_3", Texture.class));
        polarRock4 = new TextureRegion(directory.getEntry("object:polar_rock_4", Texture.class));
        polarTrunk1 = new TextureRegion(directory.getEntry("object:polar_trunk_1", Texture.class));
        polarTrunk2 = new TextureRegion(directory.getEntry("object:polar_trunk_2", Texture.class));

        polarTreeShadow = new TextureRegion(directory.getEntry("shadows:polar_tree_shadow", Texture.class));
        polarCaveShadow = new TextureRegion(directory.getEntry("shadows:polar_cave_shadow", Texture.class));
        polarHouseShadow = new TextureRegion(directory.getEntry("shadows:polar_house_shadow", Texture.class));
        polarRock1Shadow = new TextureRegion(directory.getEntry("shadows:polar_rock_1_shadow", Texture.class));
        polarRock2Shadow = new TextureRegion(directory.getEntry("shadows:polar_rock_2_shadow", Texture.class));
        polarRock3Shadow = new TextureRegion(directory.getEntry("shadows:polar_rock_3_shadow", Texture.class));
        polarRock4Shadow = new TextureRegion(directory.getEntry("shadows:polar_rock_4_shadow", Texture.class));
        polarTrunk1Shadow = new TextureRegion(directory.getEntry("shadows:polar_trunk_1_shadow", Texture.class));
        polarTrunk2Shadow = new TextureRegion(directory.getEntry("shadows:polar_trunk_2_shadow", Texture.class));
        polarTreeShadow.flip(true, true);

        playerWalkTextureAnimation = new TextureRegion(directory.getEntry("player:ursaWalk",Texture.class));
        playerWalkFilm = new FilmStrip(playerWalkTextureAnimation.getTexture(),2,8);
        playerWalkFilm.setFrame(0);
        polarCave = new TextureRegion(directory.getEntry("object:cave",Texture.class));
        playerIdleTextureAnimation = new TextureRegion(directory.getEntry("player:ursaIdle",Texture.class));
        playerIdleFilm = new FilmStrip(playerIdleTextureAnimation.getTexture(),4,8);
        playerIdleFilm.setFrame(0);
        salmonUprightWalkAnimation = new TextureRegion(directory.getEntry("enemies:salmonUprightWalk",Texture.class));
        salmonUprightWalkFilm = new FilmStrip(salmonUprightWalkAnimation.getTexture(),3,8);
        salmonUprightWalkFilm.setFrame(0);
        salmonConfusedAnimation = new TextureRegion(directory.getEntry("enemies:salmonConfused",Texture.class));
        salmonConfusedFilm = new FilmStrip(salmonConfusedAnimation.getTexture(),4,8);
        salmonUprightWalkFilm.setFrame(0);
        salmonIdleAnimation = new TextureRegion(directory.getEntry("enemies:salmonIdle",Texture.class));
        salmonIdleFilm = new FilmStrip(salmonIdleAnimation.getTexture(), 5, 8);
        salmonIdleFilm.setFrame(0);
        salmonDetectedAnimation = new TextureRegion(directory.getEntry("enemies:salmonDetected",Texture.class));
        salmonDetectedFilm = new FilmStrip(salmonDetectedAnimation.getTexture(), 4, 8);
        salmonConfusedFilm.setFrame(0);

        treeShakeAnimation = new TextureRegion(directory.getEntry("object:polar_tree_shake",Texture.class));
        treeShakeFilm = new FilmStrip(treeShakeAnimation.getTexture(), 2, 8);
        treeShakeFilm.setFrame(0);


        gatherTiles(directory);

        constants = directory.getEntry( "platform:constants", JsonValue.class );
        super.gatherAssets(directory);
    }
    public void gatherTiles(AssetDirectory directory ){

        for(int i = 0; i < jsonData.get("tilesets").size;i++ ){

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
        tileTextures[14] = new TextureRegion(directory.getEntry("tiles:polar_corner_7",Texture.class));
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
        playerStartY =  (maxY - playerStartY)/(tileHeight * 512f);
        playerStartX = (playerStartX/(tileWidth * 512f));
        tileX = tileWidth * 8f;
        tileY = tileHeight * 8f;
        // Add level goal
        float dwidth;
        float dheight;


        // create shadow (idk if this does anything even)
        shadowController = new ShadowController();

        String wname = "wall";
        JsonValue walljv = constants.get("walls");
        JsonValue defaults = constants.get("defaults");

        // Create ursa
        dwidth  = playerIdleFilm.getRegionWidth()/50f;
        dheight = playerIdleFilm.getRegionHeight()/100f;
        avatar = new UrsaModel(playerStartX * tileX + 3.5f,playerStartY * tileY +11.0f,constants.get("ursa"), dwidth, dheight);
        avatar.setDrawScale(scale);


        avatar.setTexture(playerWalkFilm);
        addObject(avatar);
        dwidth  = bipedalSalmonTexture.getRegionWidth()/scale.x;
        dheight = bipedalSalmonTexture.getRegionHeight()/scale.y;

        // place the cave

        for(int i = 0; i< jsonData.get("layers").get(6).get("objects").size;i++){
            float x = (jsonData.get("layers").get(6).get("objects").get(i).get(8).asFloat()) / (tileWidth * 512f);
            x = x * tileX + 5.5f;
            float y = (maxY - jsonData.get("layers").get(6).get("objects").get(i).get(9).asFloat())/(tileHeight * 512f);
            y = y * tileY +15 ;

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
            goalDoor.setName("cave");

            ShadowModel caveShadow = new ShadowModel(new Vector2(goalDoor.getX(), goalDoor.getY()), 0.75f, 0.75f,
                    goalDoor.getName(), goalDoor.getDrawOrigin(), scale);
            shadows.add(caveShadow);
            addObject(caveShadow);

            addObject(goalDoor);

        }

        for(int i = 0; i< jsonData.get("layers").get(3).get("objects").size;i++){
            //float markerCounter = 0;
            //enemyPosList = new Vector2[10];
            //float enemyNumber = jsonData.get("layers").get(3).get("objects").get(i).get("name").asFloat();
            float x = (jsonData.get("layers").get(3).get("objects").get(i).get(8).asFloat()) / (tileWidth * 512f);
            x = (x * (tileX + 5.5f))+2.5f;
            float y = (maxY - jsonData.get("layers").get(3).get("objects").get(i).get(9).asFloat())/(tileHeight * 512f);
            y = y * tileY +14.0f;

            float direction = 1;

            /**for(int e = 0; e < jsonData.get("layers").get(8).get("objects").size;e++){
                float MarkerName = jsonData.get("layers").get(8).get("objects").get(e).get("name").asFloat();
                if(MarkerName == enemyNumber ){
                    int orderNum = (jsonData.get("layers").get(8).get("objects").get(e).get("type").asInt());
                    float markerX = (jsonData.get("layers").get(8).get("objects").get(e).get("x").asFloat()) / (tileWidth * 512f);
                    float markerY = (jsonData.get("layers").get(8).get("objects").get(e).get("y").asFloat()) / (tileWidth * 512f);
                    markerX = (markerX * (tileX + 5.5f))+2.5f;
                    markerX = markerX * tileY +14.0f;
                    enemyPosList[orderNum-1] = new Vector2(markerX,markerY);
                    markerCounter += 1;
                }
            }*/

            enemies[i] = new Enemy(x,y,20,20,constants.get("enemy"), dwidth/2, dheight/2);
            enemies[i].setLookDirection(direction, 0);
            enemies[i].setDrawScale(scale);
            enemies[i].setTexture(salmonUprightWalkFilm);
            addObject(enemies[i]);
        }

        //place trees in the level
        JsonValue treejv = constants.get("trees");
        String tname = "tree";

        for(int i = 0; i < jsonData.get("layers").get(4).get("objects").size; i++){
            float x = (jsonData.get("layers").get(4).get("objects").get(i).get(8).asFloat())/ (tileWidth * 512f);
            x = (x * (tileX + 5.5f))+2.5f;
            float y = (maxY - jsonData.get("layers").get(4).get("objects").get(i).get(9).asFloat())/(tileHeight * 512f);
            y = y * tileY +11.0f;
            Tree obj = new Tree(treejv.get(0).asFloatArray(),x,y);
            obj.setBodyType(BodyDef.BodyType.StaticBody);
            obj.setDensity(defaults.getFloat( "density", 0.0f ));
            obj.setFriction(defaults.getFloat( "friction", 0.0f ));
            obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
            obj.setDrawScale(scale);
            obj.setTexture(polarTreeWithSnow);
            obj.setName(tname+i);

            ShadowModel model = new ShadowModel(new Vector2(obj.getX(), obj.getY()), 0.75f, 0.75f,
                    tname, new Vector2(polarTreeShadow.getRegionWidth() / 2.0f, 85), scale);
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
                controls.add(new AIController(enemies[i], avatar, trees));
            }
        }
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
            float y = (maxY - jsonData.get("layers").get(5).get("objects").get(i).get(9).asFloat())/1.3f;

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



                if(jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex){
                    // Polar middle Texture
                    canvas.draw(tileTextures[0], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);

                } else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 8) {
                    //tileTextures[1] = polar corner 1


                    canvas.draw(tileTextures[1], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 1) {
                    //tileTextures[2] = polar corner 2
                    canvas.draw(tileTextures[2], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 2) {
                    //tileTextures[3] = polar corner 3
                    canvas.draw(tileTextures[3], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 3) {
                    //tileTextures[4] = polar corner 4
                    canvas.draw(tileTextures[4], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 9) {
                    //tileTextures[5] = polar edge 3
                    canvas.draw(tileTextures[5], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 13) {
                    //tileTextures[6] = polar edge 2
                    canvas.draw(tileTextures[6], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 10) {
                    //tileTextures[7] = polar edge 4

                    canvas.draw(tileTextures[7], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 11) {
                    //tileTextures[8] = polar edge 5


                    canvas.draw(tileTextures[8], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 7) {
                    //tileTextures[9] =polar edge 1

                    canvas.draw(tileTextures[9], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 14) {
                    //tileTextures[10] = polar corner 5

                    canvas.draw(tileTextures[10], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+4) {
                    //tileTextures[11] = polar corner 6

                    canvas.draw(tileTextures[11], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+5) {
                    //tileTextures[12] = polar corner 8));


                    canvas.draw(tileTextures[12], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 12) {
                    //tileTextures[13] = polar edge 6

                    canvas.draw(tileTextures[13], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+6) {
                    //tileTextures[14] = polar corner 7

                    canvas.draw(tileTextures[14], Color.WHITE,0,0,8f * j*scale.x,i * 8f * scale.y,avatar.getAngle(), 0.5f,0.5f);
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



                if(jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex){
                    //tileTextures[0] = new TextureRegion(directory.getEntry("tiles:polar_middle",Texture.class));


                } else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex +8) {
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
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex +1) {
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
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex +2) {
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
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+3) {
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
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+9) {
                    //tileTextures[6] = new TextureRegion(directory.getEntry("tiles:polar_corner_6",Texture.class));
                    wall = new InvivisbleWall(3f + (j* 8),4f+(i*8),.3f,8f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+13) {
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
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+10) {
                    //tileTextures[12] = new TextureRegion(directory.getEntry("tiles:polar_edge_4",Texture.class));
                    wall = new InvivisbleWall( 3.9f + (j* 8),2.8f+(i*8),8f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 11) {
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
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 7) {
                    //tileTextures[5] = new TextureRegion(directory.getEntry("tiles:polar_corner_5",Texture.class));
                    wall = new InvivisbleWall( 4f + (j* 8),5f+(i*8),8f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex + 14) {
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
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+4) {
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
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+5) {
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
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+12) {
                    //tileTextures[11] = new TextureRegion(directory.getEntry("tiles:polar_edge_3",Texture.class));
                    wall = new InvivisbleWall(5.2f + (j* 8),4f+(i*8),.3f,8f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+6){
                    wall = new InvivisbleWall( (j* 8),3+ (i*8),1f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall( 1+(j* 8),3.2f+ (i*8),1f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall( 1.8f+(j* 8),3.5f+ (i*8),1f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall( 2.8f+(j* 8),4.0f+ (i*8),1f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall( 3.5f+(j* 8),4.5f+ (i*8),1f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);
                    wall = new InvivisbleWall( 4.5f+(j* 8),5.2f+ (i*8),.1f,2f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    wall.setAngle(-0.523599f);
                    addObject(wall);
                    wall = new InvivisbleWall( 5.15f+(j* 8),6.9f+ (i*8),.1f,2f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    wall.setAngle(-.2f);
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
                if (i.isWon() || i.isSurprised()) {
                    salmonDetectedFilm.setFrame(salmonDetectedIndex);
                    i.getEnemy().setTexture(salmonDetectedFilm);
                    salmonDetectedIndex = (salmonDetectedIndex + 1) % 30;
                } else if (i.isConfused() || i.isStunned()) {
                    salmonConfusedFilm.setFrame(i.get_confused_anim_index());
                    i.getEnemy().setTexture(salmonConfusedFilm);
                    i.inc_anim_index();
                } else if (i.getEnemy().getVX() == 0 && i.getEnemy().getVY() == 0) {
                    salmonIdleFilm.setFrame(salmonIdleAnimIndex);
                    i.getEnemy().setTexture(salmonIdleFilm);
                    salmonIdleAnimIndex = (salmonIdleAnimIndex + 1) % 40;
                } else {
                    i.reset_anim_index();
                    salmonUprightWalkFilm.setFrame(salmonWalkAnimIndex);
                    i.getEnemy().setTexture(salmonUprightWalkFilm);
                }
            }
        }
        salmonWalkAnimIndex = (salmonWalkAnimIndex + 1) % 21;

    }

    /**
     Animates the player model based on the conditions of the player
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

    private void animateTree() {
        if (shakingTree != null) {
            treeShakeFilm.setFrame(treeShakeIndex);
            shakingTree.setTexture(treeShakeFilm);
            if(framesSinceTreeAnimation == 0) {
                treeShakeIndex = (treeShakeIndex + 1) % 12;
                framesSinceTreeAnimation++;
            } else {
                framesSinceTreeAnimation++;
                if(framesSinceTreeAnimation == 3) { framesSinceTreeAnimation = 0;}
            }

            if(treeShakeIndex == 11) {
                shakingTree.setTexture(polarTreeNoSnow);
                treeShakeIndex = 0;
                framesSinceTreeAnimation = 0;
                shakingTree = null;
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
        if(timeRatio < uiRisingDuration) {
            uiYOffset = timeRatio / uiRisingDuration;
        } else if(timeRatio > 1f){
            uiRotationAngle = -timeRatio * (float) Math.PI + (float) Math.PI;
        } else if(timeRatio > 1f - uiRisingDuration){
            uiYOffset = (1f - timeRatio) / uiRisingDuration;
        }

        if(timeRatio == 0f) {
            uiRotationAngle = (float) Math.PI;
        } else if(timeRatio == 1f - uiRisingDuration) {
            uiRotationAngle = 0f;
        }

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
        avatar.setMovement(xVal,yVal);
        // avatar.setJumping(InputController.getInstance().didPrimary());
        //avatar.setShooting(InputController.getInstance().didSecondary());

        animatePlayerModel();
        animateEnemies();

        // Shake trees
        if (treeShakeIndex == 0 && InputController.getInstance().didInteract()) {
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

        animateTree();

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
            tree.setTexture(polarTreeNoSnow);
            shakingTree = tree;

            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.getPosition().dst(tree.getPosition()) < 5) {
                    enemy.stun();
                }
            }
        }
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
     * is to determine when the character is NOT on the ground.  This is how we prevent
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
     * Invariant: intervals.length == colors.length
     * Transitions smoothly between colors[reachPointer] and colors[reachPointer-1] in the time
     * between intervals[reachPointer] and intervals[reachPointer-1]
     * @param timeRatio
     */
    private void updateBackgroundColor(float startTime, float timeRatio) {
        backgroundColor.r = colors[nextPointer - 1].r + (colors[nextPointer].r - colors[nextPointer - 1].r) * (timeRatio - startTime) / (intervals[nextPointer] - intervals[nextPointer - 1]);
        backgroundColor.g = colors[nextPointer - 1].g + (colors[nextPointer].g - colors[nextPointer - 1].g) * (timeRatio - startTime) / (intervals[nextPointer] - intervals[nextPointer - 1]);
        backgroundColor.b = colors[nextPointer - 1].b + (colors[nextPointer].b - colors[nextPointer - 1].b) * (timeRatio - startTime) / (intervals[nextPointer] - intervals[nextPointer - 1]);
        backgroundColor.a = colors[nextPointer - 1].a + (colors[nextPointer].a - colors[nextPointer - 1].a) * (timeRatio - startTime) / (intervals[nextPointer] - intervals[nextPointer - 1]);
    }

    @Override
    public void preDraw(float dt) {


        canvas.draw(snowBackGround,Color.WHITE,0,0,tileWidth* 256, tileHeight*256);
        drawTiles();
        drawExtraObjects();
        System.out.println(backgroundColor.r + " " + backgroundColor.g + " " + backgroundColor.b + " " + backgroundColor.a);
        canvas.draw(blankTexture,backgroundColor, canvas.getCameraX() - canvas.getWidth() / 2, canvas.getCameraY() - canvas.getHeight() / 2, canvas.getWidth(), canvas.getHeight());
        super.updateTinting(backgroundColor);

        // Draws shadows for moving objects (enemy/player) and static objects
        // If it's night, don't draw shadows
        if(timeRatio > 1) {
            return;
        }
        for(Obstacle obj : objects) {
            obj.preDraw(canvas);
        }
        //shadowController.drawAllShadows(canvas, this);
    }

    @Override
    public void draw(float dt) {
        super.draw(dt);
        if (complete && !failed && active) {
            displayFont.setColor(Color.YELLOW);
            canvas.begin(); // DO NOT SCALE
            canvas.drawText("WIN!: Press r to restart, p to return to level select",displayFont,avatar.getPosition().x *31.9f, avatar.getPosition().y * 31.9f );


            canvas.end();
        } else if (failed&&active) {
            displayFont.setColor(Color.RED);
            canvas.begin(); // DO NOT SCALE
            canvas.drawText("Lose!:",displayFont,avatar.getPosition().x*31.9f, avatar.getPosition().y*31.9f);
            canvas.end();

        }
    }

    public void postDraw(float dt) {
        super.postDraw(dt);

        float dwidth = dayNightUITexture.getRegionWidth()  / 2;
        float dheight = dayNightUITexture.getRegionHeight() / 2;

        canvas.draw(dayNightUITexture, Color.WHITE, dwidth, dheight, canvas.getCameraX(), canvas.getCameraY() + canvas.getHeight() / 2 + uiYOffset * uiDrawScale * dheight, uiRotationAngle, uiDrawScale, uiDrawScale);
    }
}
