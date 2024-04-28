package edu.cornell.gdiac.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
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
import edu.cornell.gdiac.physics.enemy.Enemy;
import edu.cornell.gdiac.physics.objects.Cave;
import edu.cornell.gdiac.physics.objects.Decoration;
import edu.cornell.gdiac.physics.objects.GameObject;
import edu.cornell.gdiac.physics.objects.Moveable;
import edu.cornell.gdiac.physics.obstacle.*;
import edu.cornell.gdiac.physics.objects.GenericObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.pathing.AIController;
import edu.cornell.gdiac.physics.pathing.Board;
import edu.cornell.gdiac.physics.player.UrsaModel;
import edu.cornell.gdiac.physics.shadows.ShadowController;
import edu.cornell.gdiac.physics.shadows.ShadowModel;
import edu.cornell.gdiac.physics.objects.Tree;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.PooledList;
import java.util.Comparator;
import java.util.LinkedList;

public class SceneModel extends WorldController implements ContactListener {

    /*
     * Initialize the global blob shadow used for Ursa and enemies.
     * Since it's a shared texture, we can just use it statically across everything to make it easier.
     */

    private static final int BLOB_SHADOW_RESOLUTION = 512; // High resolution for lesser edge cuts (in theory that is)

    public static final Texture BLOB_SHADOW_TEXTURE;

    static {
        Pixmap pixmap = new Pixmap(BLOB_SHADOW_RESOLUTION * 2, BLOB_SHADOW_RESOLUTION * 2, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0f,0f,0f,0.3f));
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
    private TextureRegion salmonTexture;
    /** Texture asset for ursa texture */
    private TextureRegion ursaTexture;
    private TextureRegion smolUrsaTexture;
    /** Texture asset for tint over the whole screen */
    private final Texture blankTexture;
    /** Texture asset for trees in the polar map (first is snow, second is no snow) */
    private final TextureRegion[] treeTextures = new TextureRegion[2];
    /** Texture asset for objects in the polar map */
    private final TextureRegion[] objectTextures = new TextureRegion[9];
    /** Texture asset for the cave in the polar map */
    private TextureRegion polarCave;


    /** =========== Shadow Textures =========== */
    /** Texture asset for a tree's shadow in the polar map */
    private TextureRegion polarTreeShadow;


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
    /** Filmstrip for cave portal whirl animation */
    private FilmStrip cavePortalFilm;
    /** Filmstrip for little ursa idle animation */
    private FilmStrip smolUrsaIdleFilm;


    /** =========== Animation Variables =========== */
    /** Current frame number (used to slow down animations) */
    private int currentFrame = 0;
    /** Current index of the player walk animation */
    private int ursaWalkAnimIndex = 0;
    /** The frame at which Ursa began idling */
    private int ursaBeganWalkingFrame = 0;
    /** Ursa's walk animates every ursaWalkAnimBuffer update loops */
    private int ursaWalkAnimBuffer = 2;
    /** Current index of the player idling animation */
    private int ursaIdleAnimIndex = 0;
    /** The frame at which Ursa began idling */
    private int ursaBeganIdlingFrame = 0;
    /** Ursa's idle animates every ursaIdleAnimBuffer update loops */
    private int ursaIdleAnimBuffer = 3;
    /** Player's current state: true corresponds to walking, false for idling */
    private boolean ursaCurrentState = false;
    /** Current index of the salmon walking animation */
    private int salmonWalkAnimIndex = 0;
    /** Current index of the salmon confused animation */
    private int salmonConfusedAnimIndex = 0;
    /** Current index of the salmon idling animation */
    private int salmonIdleAnimIndex = 0;
    /** Current index of the salmon detection animation */
    private int salmonDetectedIndex = 0;
    /** Current index of the tree shaking animation */
    private int treeShakeIndex = 0;
    /** The frame at which the tree began shaking */
    private int beganShakingTreeFrame = 0;
    /** Tree shaking animates every treeShakeAnimBuffer update loops */
    private int treeShakeAnimBuffer = 4;
    /** Current index of the cave portal animation */
    private int cavePortalIndex = 0;
    /** Cave portal animates every cavePortalAnimBuffer update loops */
    private int cavePortalAnimBuffer = 3;
    /** Current index of the smol ursa idle animation */
    private int smolUrsaIdleIndex = 0;
    /** Smol ursa's idle animates every smolUrsaIdleAnimBuffer update loops */
    private int smolUrsaIdleAnimBuffer = 3;


    /** =========== Tree Shaking Variables =========== */
    /** The tree that is currently shaking. */
    private Tree shakingTree = null;
    /** How far away the player must be to interact with trees (screen coords) */
    private float treeInteractionRange = 7;
    /** Within what distance will the enemy be stunned upon tree shaking. */
    private float enemyStunDistance = 10;


    /** =========== Tiled Parsing Variables =========== */
    /** Maximum Y Coordinate (Screen) */
    private final float maxY;
    /** JSON containing all the information for the level */
    private final JsonValue jsonData;
    /** Constants used for initialization (width, height, scaling, etc) */
    private JsonValue constants;
    /** The following variables are HELLA hard coded so they can really break the code
     * The index of the first tile in the tile sprite sheet */
    private final int firstTileIndex = 19;
    /** The index of the first tree in the tree sprite sheet */
    private int firstTreeIndex = 34;
    /** The index of the first tile in the 64x64 decoration sprite sheet*/
    private int firstSmallDecorationIndex = 1;
    /** The index of the first tile in the 512x512 decoration sprite sheet*/
    private int firstLargeDecorationIndex = 40;
    /** The index of the first tile in the 256x256 decoration sprite sheet*/
    private int firstMediumObjectIndex = 16;
    /** The index of the first medium rock in the 256x256 sprite sheet */
    private int firstMediumRockIndex = 36;
    /** Number of rocks  in the 256x256 sprite sheet */
    private int numMediumRocks = 2;
    /** The index of the first house in terms of all textures in json*/
    private int firstHouseIndex = 39;
    /** The index of the large object in terms of all textures in json*/
    private int firstLargeObjectIndex = 42;


    /** =========== Tile Variables =========== */
    /** Total number of tiles in the y direction on the board. */
    private final float numTilesY;
    /** Total number of tiles in the x direction on the board. */
    private final float numTilesX;
    /** Height of a single tile */
    private final float tileSideLength = 256;


    private float timeRatio;

    /** =========== Collections of References =========== */
    /** Reference to the character avatar */
    private UrsaModel ursa;
    /** An array of TextureRegions containing all tile textures */
    private final TextureRegion[] tileTextures = new TextureRegion[15];
    /** An array of TextureRegions containing all decoration textures */
    private final TextureRegion[] decorationTextures = new TextureRegion[16];
    /** List of references to enemies */
    private Enemy[] enemies;
    /** List of references to all AIControllers */
    private LinkedList<AIController> controls = new LinkedList<>();
    /** List of references to all shadows. */
    private PooledList<ShadowModel> shadows = new PooledList<>();
    /** List of references to all trees */
    private PooledList<Tree> trees = new PooledList<>();
    /** List of references to all decorations */
    private PooledList<Decoration> decorations =  new PooledList<>();;

    /**
     * Information about the gameboard used for pathfinding
     */
    private Board gameBoard;

    /**
     * List of all references to all generic obstacles
     */
    private PooledList<GenericObstacle> genericObstacles = new PooledList<>();

    /** Reference to goal */
    private PolygonObstacle goal;
    /** Controller for all dynamic shadows */
    private ShadowController shadowController;
    /** rock to be used to reset the day if interacted with */
    private GenericObstacle specialRock = null;
    private Comparator<Decoration> decorationComparator = (o1, o2) -> Float.compare(o2.getIndex(), o1.getIndex());


    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;


    /** =========== Day/Night Screen Tinting =========== */
    /** Background color that changes with the day */
    protected Color backgroundColor = Color.BLACK;
    /** A list of colors that the tinting with linear interpolate between */
    private final Color[] colors;
    /** The intervals at which the tintings will occur
     * colors[i] happens at intervals[i] */
    private float[] intervals;
    /** Points to the next color we interpolate to */
    private int colorNextPointer = 1;

    /** =========== UI Constants =========== */
    /** Rotation angle of UI element */
    private float uiRotationAngle = 0f;
    /** Draw scale of UI */
    private float uiDrawScale = 0.1f;


    /** =========== Soundtrack assets =========== */
    private Music levelMusic;
    private Music levelMusicNight;
    private Music levelMusicTense;

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
        JsonReader json = new JsonReader();
        jsonData = json.parse(Gdx.files.internal(levelJson));

        numTilesY = jsonData.get("layers").get(0).get(1).asFloat();
        numTilesX = jsonData.get("layers").get(0).get(7).asFloat();


        maxY = numTilesY * tileSideLength;

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
        salmonTexture = new TextureRegion(directory.getEntry("enemies:salmon", Texture.class));
        ursaTexture = new TextureRegion(directory.getEntry("player:ursa", Texture.class));
        smolUrsaTexture = new TextureRegion(directory.getEntry("smolursa:model", Texture.class));

        treeTextures[0] = new TextureRegion(directory.getEntry("object:tundra_tree_snow_small", Texture.class));
        treeTextures[1] = new TextureRegion(directory.getEntry("object:tundra_tree",Texture.class));

        polarCave = new TextureRegion(directory.getEntry("polar:cave",Texture.class));

        polarTreeShadow = new TextureRegion(directory.getEntry("shadows:polar_tree_shadow", Texture.class));
        polarTreeShadow.flip(true, true);

        levelMusic = directory.getEntry("soundtracks:level_track", Music.class);
        levelMusicNight = directory.getEntry("soundtracks:level_track_night", Music.class);
        levelMusicTense = directory.getEntry("soundtracks:level_track_tense", Music.class);

        gatherAnimations(directory);
        gatherTiles(directory);
        gatherDecorations(directory);
        gatherObjects(directory);

        constants = directory.getEntry( "polar:constants", JsonValue.class );
        super.gatherAssets(directory);
    }

    /**
     * Gathers all textures for animations and converts them into film strips.
     * @param directory AssetDirectory from assets.json
     */
    public void gatherAnimations(AssetDirectory directory)
    {
        TextureRegion playerWalkTextureAnimation = new TextureRegion(directory.getEntry("player:ursaWalk", Texture.class));
        playerWalkFilm = new FilmStrip(playerWalkTextureAnimation.getTexture(),2,8);
        TextureRegion playerIdleTextureAnimation = new TextureRegion(directory.getEntry("player:ursaIdle", Texture.class));
        playerIdleFilm = new FilmStrip(playerIdleTextureAnimation.getTexture(),4,8);

        TextureRegion salmonUprightWalkAnimation = new TextureRegion(directory.getEntry("enemies:salmonUprightWalk", Texture.class));
        salmonUprightWalkFilm = new FilmStrip(salmonUprightWalkAnimation.getTexture(),4,8);
        TextureRegion salmonConfusedAnimation = new TextureRegion(directory.getEntry("enemies:salmonConfused", Texture.class));
        salmonConfusedFilm = new FilmStrip(salmonConfusedAnimation.getTexture(),4,8);
        TextureRegion salmonIdleAnimation = new TextureRegion(directory.getEntry("enemies:salmonIdle", Texture.class));
        salmonIdleFilm = new FilmStrip(salmonIdleAnimation.getTexture(), 5, 8);
        TextureRegion salmonDetectedAnimation = new TextureRegion(directory.getEntry("enemies:salmonDetected", Texture.class));
        salmonDetectedFilm = new FilmStrip(salmonDetectedAnimation.getTexture(), 4, 8);

        TextureRegion treeShakeAnimation = new TextureRegion(directory.getEntry("object:polar_tree_shake", Texture.class));
        treeShakeFilm = new FilmStrip(treeShakeAnimation.getTexture(), 2, 8);

        TextureRegion cavePortalAnimation = new TextureRegion(directory.getEntry("polar:cave_animation", Texture.class));
        cavePortalFilm = new FilmStrip(cavePortalAnimation.getTexture(), 2, 8);

        TextureRegion smolUrsaIdleAnimation = new TextureRegion(directory.getEntry("smolursa:idle", Texture.class));
        smolUrsaIdleFilm = new FilmStrip(smolUrsaIdleAnimation.getTexture(), 5, 8);
    }
    /**
     * Gathers all tile textures into a single array.
     * @param directory AssetDirectory from assets.json
     */
    public void gatherTiles(AssetDirectory directory){
        tileTextures[0] = new TextureRegion(directory.getEntry("tiles:polar_cliff_center",Texture.class));
        tileTextures[1] = new TextureRegion(directory.getEntry("tiles:polar_corner_1",Texture.class));
        tileTextures[2] = new TextureRegion(directory.getEntry("tiles:polar_corner_2",Texture.class));
        tileTextures[3] = new TextureRegion(directory.getEntry("tiles:polar_corner_3",Texture.class));
        tileTextures[4] = new TextureRegion(directory.getEntry("tiles:polar_corner_4",Texture.class));
        tileTextures[5] = new TextureRegion(directory.getEntry("tiles:polar_corner_5",Texture.class));
        tileTextures[6] = new TextureRegion(directory.getEntry("tiles:polar_corner_6",Texture.class));
        tileTextures[7] = new TextureRegion(directory.getEntry("tiles:polar_corner_7",Texture.class));
        tileTextures[8] = new TextureRegion(directory.getEntry("tiles:polar_corner_8",Texture.class));
        tileTextures[9] = new TextureRegion(directory.getEntry("tiles:polar_edge_1",Texture.class));
        tileTextures[10] = new TextureRegion(directory.getEntry("tiles:polar_edge_2",Texture.class));
        tileTextures[11] = new TextureRegion(directory.getEntry("tiles:polar_edge_3",Texture.class));
        tileTextures[12] = new TextureRegion(directory.getEntry("tiles:polar_edge_4",Texture.class));
        tileTextures[13] = new TextureRegion(directory.getEntry("tiles:polar_edge_5",Texture.class));
        tileTextures[14] = new TextureRegion(directory.getEntry("tiles:polar_edge_6",Texture.class));
    }
    /**
     * Gathers all decoration textures into a single array.
     * @param directory AssetDirectory from assets.json
     */
    public void gatherDecorations(AssetDirectory directory) {
        decorationTextures[0] = new TextureRegion(directory.getEntry("decoration:plant_1",Texture.class));
        decorationTextures[1] = new TextureRegion(directory.getEntry("decoration:plant_2",Texture.class));
        decorationTextures[2] = new TextureRegion(directory.getEntry("decoration:plant_3",Texture.class));
        decorationTextures[3] = new TextureRegion(directory.getEntry("decoration:plant_4",Texture.class));
        decorationTextures[4] = new TextureRegion(directory.getEntry("decoration:ground_3",Texture.class));
        decorationTextures[5] = new TextureRegion(directory.getEntry("decoration:owl",Texture.class));
        decorationTextures[6] = new TextureRegion(directory.getEntry("decoration:mushroom",Texture.class));
        decorationTextures[7] = new TextureRegion(directory.getEntry("decoration:bird",Texture.class));
        decorationTextures[8] = new TextureRegion(directory.getEntry("decoration:shrew",Texture.class));
        decorationTextures[9] = new TextureRegion(directory.getEntry("decoration:flower_1",Texture.class));
        decorationTextures[10] = new TextureRegion(directory.getEntry("decoration:flower_2",Texture.class));
        decorationTextures[11] = new TextureRegion(directory.getEntry("decoration:ground_4",Texture.class));
        decorationTextures[12] = new TextureRegion(directory.getEntry("decoration:ground_1",Texture.class));
        decorationTextures[13] = new TextureRegion(directory.getEntry("decoration:ground_2",Texture.class));
    }
    /**
     * Gathers all object textures and converts them into a single array.
     * @param directory AssetDirectory from assets.json
     */
    public void gatherObjects(AssetDirectory directory) {
        objectTextures[0] = new TextureRegion(directory.getEntry("polar:rock_1",Texture.class));
        objectTextures[1] = new TextureRegion(directory.getEntry("polar:statue",Texture.class));
        objectTextures[2] = new TextureRegion(directory.getEntry("polar:goat",Texture.class));
        objectTextures[3] = new TextureRegion(directory.getEntry("polar:rock_4",Texture.class));
        objectTextures[4] = new TextureRegion(directory.getEntry("polar:rock_3",Texture.class));
        objectTextures[5] = new TextureRegion(directory.getEntry("polar:house",Texture.class));
        objectTextures[6] = new TextureRegion(directory.getEntry("polar:rock_2",Texture.class));
        objectTextures[7] = new TextureRegion(directory.getEntry("polar:trunk_1",Texture.class));
        objectTextures[8] = new TextureRegion(directory.getEntry("polar:trunk_2",Texture.class));
    }

    /**
     * Resets the status of the game so that we can play again.
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        uiRotationAngle = 0;
        Vector2 gravity = new Vector2(0,0 );

        for(Obstacle obj : objects) {
            obj.deactivatePhysics(world);
        }
        objects.clear();
        shadows.clear();
        addQueue.clear();
        trees.clear();
        decorations.clear();
        world.dispose();
        colorNextPointer = 1;
        uiRotationAngle = 0;
        currentFrame = 0;
        shakingTree = null;
        treeShakeIndex = 0;

        //for (AIController c : controls) c.reset();
        controls.clear();

        world = new World(gravity,false);
        world.setContactListener(this);
        setComplete(false);
        setFailure(false);
        levelMusicNight.stop();
        levelMusic.stop();
        levelMusicTense.stop();
        levelMusicNight.setLooping(true);
        levelMusicTense.setLooping(true);
        levelMusicTense.setVolume(0);
        levelMusicTense.setVolume(0);


        populateLevel();
        System.out.println(" ====== Reset ===== ");
    }


    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        // create shadow (idk if this does anything even)
        shadowController = new ShadowController();

        renderUrsa();
        renderWalls();
        renderEnemies();
        renderTrees();
        renderCaves();
        renderSmolUrsa();
        renderGameObjects();
        renderDecorations();
        //renderRocks();






//
//        dwidth  = salmonTexture.getRegionWidth()/scale.x;
//        dheight = salmonTexture.getRegionHeight()/scale.y;
//
//        /**
//         * This loop renders each cave in a given map.
//         */
//        JsonValue currLayer = jsonData.get("layers").get(6);
//        for(int i = 6; i <= 6; i++) {
//            for (int j = 0; j < currLayer.get("objects").size; j++) {
//                float x = currLayer.get("objects").get(j).get(8).asFloat();
//                float y = (maxY - (currLayer.get("objects").get(j).get(9).asFloat()));
//
//                JsonValue currConstants = constants.get(currLayer.get("name").asString());
//
//                goalDoor = new GameObject(currConstants.get("vertices").asFloatArray(), x / 64 + 4,
//                        y / 64 + 8, currConstants.get("xScale").asFloat(),
//                        currConstants.get("yScale").asFloat());
//                goalDoor.setSensor(true);
//                goalDoor.setDrawScale(scale);
//                goalDoor.setTexture(getTextureRegion(currConstants.get("texture").asString()));
//                goalDoor.setName("cave");
//
//                // Cave shadows
//                ShadowModel shadow = new ShadowModel(
//                        new Vector2(goalDoor.getX(), goalDoor.getY()), 0.75f, 0.75f,
//                        goalDoor.getName(), goalDoor.getDrawOrigin(), scale);
//                shadows.add(shadow);
//
//                addObject(shadow);
//                addObject(goalDoor);
//
//                // ===================
//                genericObstacles.add(new GenericObstacle(goalDoor.getX(), goalDoor.getY(),
//                        goalDoor.getWidth(), goalDoor.getHeight()));
//                // ===================
//            }
//        }
//
//
//        /**
//         * This loop renders each rock in a given map.
//         */
//
//        for(int i = 0; i< jsonData.get("layers").get(7).get("objects").size; i++) {
//            float x = (jsonData.get("layers").get(7).get("objects").get(i).get(8).asFloat());
//            float y = (maxY - jsonData.get("layers").get(7).get("objects").get(i).get(9).asFloat());
//            float ratioX = x/maxX;
//            float ratioY = y/maxY;
//
//            GenericObstacle rock = new GenericObstacle(ratioX * deprecatedTileWidth * 9,(ratioY * deprecatedTileHeight
//                    * 9) + 8,3,3,0.3f, 0.3f, 20);
//            rock.setDrawScale(scale);
//
//            rock.setTexture(getRockTexture(jsonData.get("layers").get(7).get("objects").get(i).get(5).asString()));
//            rock.setName("rock"+i);
//
//            if (specialRock == null) { specialRock = rock; }
//
//            // Cave shadows
//            ShadowModel rockShadow = new ShadowModel(new Vector2(rock.getX(), rock.getY()), 0.75f, 0.75f,
//                    "rock", rock.getDrawOrigin(), scale);
//            shadows.add(rockShadow);
//
//            addObject(rockShadow);
//            addObject(rock);
//
//            // ===================
//            genericObstacles.add(new GenericObstacle(rock.getX(), rock.getY(),
//                    rock.getWidth(), rock.getHeight()));
//            // ===================
//        }
//
//
//
//        /**
//         * This loop renders each enemy in a given map.
//         */
//        for(int i = 0; i< jsonData.get("layers").get(3).get("objects").size;i++){
//            float markerCounter = 0;
//            enemyPosList = new Vector2[10];
//            float enemyNumber = jsonData.get("layers").get(3).get("objects").get(i).get("name").asFloat();
//            float x = (jsonData.get("layers").get(3).get("objects").get(i).get(8).asFloat()) ;
//
//            float y = (maxY - jsonData.get("layers").get(3).get("objects").get(i).get(9).asFloat());
//            float ratioX = x/maxX;
//            float ratioY = y/maxY;
//
//            float direction = 1;
//
//            for(int e = 0; e < jsonData.get("layers").get(8).get("objects").size;e++){
//                float MarkerName = jsonData.get("layers").get(8).get("objects").get(e).get("name").asFloat();
//                if(MarkerName == enemyNumber ){
//                    int orderNum = (jsonData.get("layers").get(8).get("objects").get(e).get("type").asInt());
//                    float markerX = (jsonData.get("layers").get(8).get("objects").get(e).get("x").asFloat());
//                    float markerY = ((maxY - jsonData.get("layers").get(8).get("objects").get(e).get("y").asFloat())) ;
//                    float markerRatioX = markerX/maxX;
//                    float markerRatioY = markerY/maxY;
//
//                    enemyPosList[orderNum-1] = new Vector2(markerRatioX * deprecatedTileWidth * 9  ,(markerRatioY * deprecatedTileWidth
//                            * 8) +8);
//                    markerCounter += 1;
//                }
//            }
//
//            enemies[i] = new Enemy(ratioX * deprecatedTileWidth * 9,(ratioY * deprecatedTileHeight * 9) + 8,20,20,constants.get("enemy"), dwidth/2, dheight/2);
//            enemies[i].setLookDirection(direction, 0);
//            enemies[i].setDrawScale(scale);
//            enemies[i].setTexture(salmonUprightWalkFilm);
//            addObject(enemies[i]);
//
//
//            if (enemies[i] != null) {
//                Board board = new Board(genericObstacles, enemies);
//                if(i == 0) {
//                    controls.add(new AIController(enemies[i], ursa, null, enemyPosList, true));
//                } else {
//                    controls.add(new AIController(enemies[i], ursa, null, enemyPosList));
//                }
//            }
//        }
//
//
//
//
//
//        /**
//         * This loop renders each house in a given map.
//         */
//
//        // TODO: Uncouple Tiled-generated layers from obstacle rendering
//        for(int i = 0; jsonData.get("layers").get(9) != null &&
//                i < jsonData.get("layers").get(9).get("objects").size; i++) {
//
//            float x = (jsonData.get("layers").get(9).get("objects").get(i).get(8).asFloat());
//            float y = (maxY - jsonData.get("layers").get(9).get("objects").get(i).get(9).asFloat());
//            float ratioX = x/maxX;
//            float ratioY = y/maxY;
//
//            GenericObstacle house = new GenericObstacle(ratioX * deprecatedTileWidth * 9,(ratioY * deprecatedTileHeight
//                    * 9) + 8,3,3, 8);
//            house.setDrawScale(scale);
//            house.setTexture(polarHouse);
//            house.setName("house"+i);
//
//            // Cave shadows
//            ShadowModel houseShadow = new ShadowModel(new Vector2(house.getX(), house.getY()), 0.75f, 0.75f,
//                    "rock", house.getDrawOrigin(), scale);
//            shadows.add(houseShadow);
//
//            addObject(houseShadow);
//            addObject(house);
//
//            // ===================
//            genericObstacles.add(new GenericObstacle(house.getX(), house.getY(),
//                    house.getWidth(), house.getHeight()));
//            // ===================
//        }
//
//        drawWalls();

        // Music stuff

        // make gameboard

        // populate genericObjects

//        gameBoard = new Board(genericObstacles, enemies);
//        for (AIController i : controls) { i.setGameBoard(gameBoard); }
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

        if (!isFailure() && ursa.getY() < -1) {
            setFailure(true);
            return false;
        }

        return true;
    }

    /**
     Draws all tiles based on the json data from Tiled
     */
    public void drawTiles(){
        int counter = 0;
        float x;
        float y;
        // The array needs to be parsed from top to bottom
        for (int i = (int) numTilesY - 1; i >= 0 ; i--) {
            for(int j = 0; j < numTilesX; j++){
                int tileIndex = jsonData.get("layers").get(0).get(0).get(counter++).asInt();
                if(tileIndex == 0) {
                    continue;
                }
                x = j * 12f * scale.x;
                y = i * 12f * scale.y;
                canvas.draw(tileTextures[tileIndex - firstTileIndex], Color.WHITE,0,0, x, y, ursa.getAngle(), 0.75f,0.75f);
            }
        }

//        canvas.draw(redTextureRegion,Color.WHITE, 0f,256* deprecatedTileHeight, 10,10);
//        canvas.draw(redTextureRegion,Color.WHITE, 0f,0 * 256, 10,10);
//        canvas.draw(redTextureRegion,Color.WHITE, 0f,192, 10,10);


    }

    private void animateEnemies(){
        for (AIController i : controls) {
            if (i != null) {
                if (i.isWon() || i.isSurprised()) {
                    salmonDetectedFilm.setFrame(salmonDetectedIndex);
                    i.getEnemy().setTexture(salmonDetectedFilm);
                    salmonDetectedIndex = (salmonDetectedIndex + 1) % 30;
                } else if (i.isConfused() || i.isStunned() || i.earlyLooking()) {
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
        salmonWalkAnimIndex = (salmonWalkAnimIndex + 1) % 25;

    }

    /**
     * Animates the player.
     * If the player is moving, uses the walking animation and if not walking, plays idling animation
     * Idling animation is slowed by 1/ursaIdleAnimBuffer
     */
    private void animatePlayerModel(){
        // If the player is moving
        if(ursa.getXMovement() != 0 || ursa.getyMovement() != 0){
            // If the player was idling, now changing states
            if(ursaCurrentState == false) {
                ursaCurrentState = true;
                ursaBeganWalkingFrame = currentFrame;
            }
            ursaIdleAnimIndex = 0;
            // Rewind the film
            if(ursaWalkAnimIndex == 12){
                ursaWalkAnimIndex = 0;
            }
            playerWalkFilm.setFrame(ursaWalkAnimIndex);
            ursa.setTexture(playerWalkFilm);
            if((currentFrame - ursaBeganWalkingFrame) % ursaWalkAnimBuffer == 0) {
                ursaWalkAnimIndex += 1;
            }
            // If the player is not moving
        } else {
            // If the player changed states
            if(ursaCurrentState) {
                ursaCurrentState = false;
                ursaBeganIdlingFrame = currentFrame;
            }
            ursaWalkAnimIndex = 0;
            // Rewind the film
            if(ursaIdleAnimIndex == 30){
                ursaIdleAnimIndex = 0;
            }
            playerIdleFilm.setFrame(ursaIdleAnimIndex);
            ursa.setTexture(playerIdleFilm);
            if((currentFrame - ursaBeganIdlingFrame) % ursaIdleAnimBuffer == 0) {
                ursaIdleAnimIndex += 1;
            }
        }
    }

    /**
     * Animates the tree in the shakingTree variable
     * Resets shakingTree if the animation has finished.
     */
    private void animateTree() {
        if (shakingTree == null) {
            return;
        }

        treeShakeFilm.setFrame(treeShakeIndex);
        shakingTree.setTexture(treeShakeFilm);
        // Increment the film by one every 3 frames
        if((currentFrame - beganShakingTreeFrame) % treeShakeAnimBuffer == 0) {
            treeShakeIndex = (treeShakeIndex + 1) % 12;
        }

        if(treeShakeIndex == 11) {
            // Change the shakingTree to no snow texture
            shakingTree.setTexture(treeTextures[1]);
            treeShakeIndex = 0;
            // Reset the currently shaking tree
            shakingTree = null;
        }
    }

    private void animateCaves() {
        cavePortalFilm.setFrame(cavePortalIndex);
        if(currentFrame % cavePortalAnimBuffer == 0) {
            cavePortalIndex = (cavePortalIndex + 1) % 11;
        }
    }

    private void animateSmolUrsa() {
        smolUrsaIdleFilm.setFrame(smolUrsaIdleIndex);
        if(currentFrame % smolUrsaIdleAnimBuffer == 0) {
            smolUrsaIdleIndex = (smolUrsaIdleIndex + 1) % 39;
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
        // Increment the current frame (used for animation slow downs)
        currentFrame++;

        // Continuously rotate the day/night UI
        uiRotationAngle = -timeRatio * (float) Math.PI + (float) Math.PI;

        // Update the timeRatio (used for UI element and tinting)
        timeRatio = shadowController.getTimeRatio();
        // If it's night, reset the tinting
        if(timeRatio > 1) {
            colorNextPointer = 1;
            if (!isComplete()) {
                levelMusicNight.setVolume(Math.max(0, levelMusicNight.getVolume() + 0.01f));
            }
        } else {
            levelMusicNight.setVolume(Math.max(0, levelMusicNight.getVolume() - 0.01f));
            // Update colorNextPointer to next interval
            if(timeRatio > intervals[colorNextPointer]) {
                colorNextPointer++;
            }
            updateBackgroundColor(intervals[colorNextPointer-1],timeRatio);
        }

        // Move the camera to Ursa
        canvas.moveCam(ursa.getPosition().x,ursa.getPosition().y);

        if (!levelMusic.isPlaying()) {
            levelMusicNight.play();
            levelMusicTense.play();
            levelMusic.play();
            levelMusic.setLooping(true);
        }


        // Move Ursa
        float xVal = InputController.getInstance().getHorizontal() * ursa.getForce();
        float yVal = InputController.getInstance().getVertical() * ursa.getForce();
        ursa.setMovement(xVal,yVal);

        // If the player tried to interact with a tree
        if (treeShakeIndex == 0 && InputController.getInstance().didInteract()) {
            Tree nearest = null;
            float minDistance = 0;
            float tempDistance;
            for (Tree tree : trees) {
                tempDistance = ursa.getPosition().dst(tree.getPosition());
                // This 3 is a hard coded constant for interactionrange
                if (tree.canShake() && tempDistance < treeInteractionRange && (nearest == null || tempDistance < minDistance)) {
                    nearest = tree;
                    minDistance = tempDistance;
                }
            }

            if (nearest != null) {
                shakeTree(nearest);
            }
        }

        // reset day if interact w special rock
        if (specialRock != null && InputController.getInstance().didInteract()
                && ursa.getPosition().dst(specialRock.getPosition()) <= treeInteractionRange) {
            for (ShadowModel s : shadows) {
                s.rotateDirection((float) (-1 * shadowController.getTime() * (360 / 240)/5));
            }
            System.out.println(shadowController.getTime());
            shadowController.setTime(0);
            System.out.println(shadowController.getTime());
//            shadowController.update(this);
//            shadowController.reset(this);
            for (AIController i : controls) { i.reset(); }
        }


        // Animate the players, trees, and enemies
        animatePlayerModel();
        animateEnemies();
        animateTree();
        animateCaves();
        animateSmolUrsa();

        // If the game is lost, move the player
        if (!isFailure()) {
            ursa.applyForce();
        } else {
            ursa.setVX(0);
            ursa.setVY(0);
        }

        boolean alerted = false;
        for (AIController c : controls) {
            c.getAction();
            Enemy thisEnemy = c.getEnemy();
            thisEnemy.setAlerted(thisEnemy.isPlayerInLineOfSight(world, ursa));
            if (!alerted && thisEnemy.isAlerted()) {
                alerted = true;
            }

            if (c.isWon()) setFailure(true);
        }

        for (Enemy enemy : enemies) {
            if(enemy != null) {
                if (enemy.isPlayerInLineOfSight(world,ursa)) {
                    enemy.getPlayerPos(ursa.getPosition());
                }
                enemy.setInShadow(ursa.isInShadow());
            }
        }

        if (alerted) {
            levelMusicTense.setVolume(Math.min(levelMusicTense.getVolume() + 0.01f, 1f));
        } else {
            levelMusicTense.setVolume(Math.max(levelMusicTense.getVolume() - 0.01f, 0f));
        }

        canvas.clear();
        shadowController.update(this);
    }

    @Override
    public void hide() {
        super.hide();
        levelMusic.stop();
        levelMusicTense.stop();
        levelMusicNight.stop();
    }

    private void shakeTree(Tree tree) {
        if (tree.canShake()) {
            tree.putOnShakeCooldown();
            shakingTree = tree;
            beganShakingTreeFrame = currentFrame;

            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.getPosition().dst(tree.getPosition()) < enemyStunDistance) {
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
            if ((ursa.getSensorName().equals(fd2) && bd1.getName().equals("shadow")) ||
                    (ursa.getSensorName().equals(fd1) && bd2.getName().equals("shadow"))) {
                ursa.setInShadow(true);
                sensorFixtures.add(ursa == bd1 ? fix2 : fix1); // Could have more than one ground
            }

            // Check for win condition
            if ((bd1 == ursa   && bd2 == goal) ||
                    (bd1 == goal && bd2 == ursa)) {
                setComplete(true);
                levelMusic.stop();
                levelMusicTense.stop();
                levelMusicNight.stop();
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

        if ((ursa.getSensorName().equals(fd2) && bd1.getName().equals("shadow")) ||
                (ursa.getSensorName().equals(fd1) && bd2.getName().equals("shadow"))) {
            sensorFixtures.remove(ursa == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                ursa.setInShadow(false);
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
        backgroundColor.r = colors[colorNextPointer - 1].r + (colors[colorNextPointer].r - colors[colorNextPointer - 1].r) * (timeRatio - startTime) / (intervals[colorNextPointer] - intervals[colorNextPointer - 1]);
        backgroundColor.g = colors[colorNextPointer - 1].g + (colors[colorNextPointer].g - colors[colorNextPointer - 1].g) * (timeRatio - startTime) / (intervals[colorNextPointer] - intervals[colorNextPointer - 1]);
        backgroundColor.b = colors[colorNextPointer - 1].b + (colors[colorNextPointer].b - colors[colorNextPointer - 1].b) * (timeRatio - startTime) / (intervals[colorNextPointer] - intervals[colorNextPointer - 1]);
        backgroundColor.a = colors[colorNextPointer - 1].a + (colors[colorNextPointer].a - colors[colorNextPointer - 1].a) * (timeRatio - startTime) / (intervals[colorNextPointer] - intervals[colorNextPointer - 1]);
    }

    @Override
    public void preDraw(float dt) {
        canvas.draw(snowBackGround,Color.WHITE,0,0, numTilesX * tileSideLength, numTilesY * tileSideLength);

        for(Decoration d: decorations) {
            d.draw(canvas);
        }

        drawTiles();
        canvas.draw(blankTexture,backgroundColor, canvas.getCameraX() - canvas.getWidth() / 2f, canvas.getCameraY() - canvas.getHeight() / 2, canvas.getWidth(), canvas.getHeight());
        super.updateTinting(backgroundColor);

        for(Obstacle obj : objects) {
            obj.preDraw(canvas);
        }
        // Draws shadows for moving objects (enemy/player) and static objects
        // If it's night, don't draw shadows
        if(timeRatio > 1) {
            return;
        }

        //shadowController.drawAllShadows(canvas, this);
    }

    @Override
    public void draw(float dt) {
        super.draw(dt);
        if (complete && !failed && active) {
            displayFont.setColor(Color.YELLOW);
            canvas.begin(); // DO NOT SCALE
            // canvas.drawText("WIN!: Press r to restart, p to return to level select",displayFont,avatar.getPosition().x *31.9f, avatar.getPosition().y * 31.9f );

            canvas.end();
        } else if (failed&&active) {
            displayFont.setColor(Color.RED);
            canvas.begin(); // DO NOT SCALE
            canvas.drawText("Lose!:",displayFont,ursa.getPosition().x*31.9f, ursa.getPosition().y*31.9f);
            canvas.end();
        }
    }

    public void postDraw(float dt) {
        super.postDraw(dt);

        float dwidth = dayNightUITexture.getRegionWidth()  / 2f;
        float dheight = dayNightUITexture.getRegionHeight() / 2f;

        canvas.draw(dayNightUITexture, Color.WHITE, dwidth, dheight, canvas.getCameraX(), canvas.getCameraY() + canvas.getHeight() / 2f, uiRotationAngle, uiDrawScale, uiDrawScale);

    }


    public float drawToScreenCoordinates(float drawCoord) {
        // 0.75 converts between 2560 x 1440 resolution assets were drawn for and the 1920 x 1080 resolution we have
        // Then, convert to screen coordinates using the scale
        return drawCoord * 0.75f / scale.x;
    }

    public void renderWalls(){
        int counter = 0;
        float x;
        float y;
        JsonValue wallConstants = constants.get("wall");
        // The array needs to be parsed from top to bottom
        for (int i = (int) numTilesY - 1; i >= 0 ; i--) {
            for(int j = 0; j < numTilesX; j++){
                int tileIndex = jsonData.get("layers").get(0).get(0).get(counter++).asInt();
                if(tileIndex == 0 || tileIndex == firstTileIndex) {
                    continue;
                }
                x = j * 12f;
                y = i * 12f;
                float[] coords = wallConstants.get(Integer.toString(tileIndex - firstTileIndex)).asFloatArray();
                Barrier wall = new Barrier(coords,x, y);
                wall.setDrawScale(scale);
                wall.setName("wall" + i + " " + j);
                addObject(wall);
                // ===================
                genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                        wall.getWidth(), wall.getHeight()));
                // ===================
            }
        }
    }

    public void renderEnemies() {
        if (jsonData.get("layers").get(7) == null) {
            return;
        }
        JsonValue enemyConstants = constants.get("enemy");
        JsonValue enemyObjectData = jsonData.get("layers").get(7).get("objects");

        enemies = new Enemy[enemyObjectData.size];
        for (int i = 0; i < enemyObjectData.size; i++) {
            float x = enemyObjectData.get(i).get(8).asFloat() + salmonTexture.getRegionWidth() / 2f;
            float y = maxY - enemyObjectData.get(i).get(9).asFloat();
            String enemyName = enemyObjectData.get(i).get("name").asString();

            float width = enemyConstants.get("width").asFloat();
            float height = enemyConstants.get("height").asFloat();
            Enemy obj = new Enemy(drawToScreenCoordinates(x), drawToScreenCoordinates(y) + height / 2,20,20,constants.get("enemy"), width, height);
            obj.setDrawScale(scale);
            obj.setLookDirection(1, 0);
            obj.setTexture(salmonUprightWalkFilm);
            obj.setName("enemy" + i);

            addObject(obj);
            enemies[i] = obj;

            if(jsonData.get("layers").get(8) == null) {
                System.out.println("Please put down some markers.");
                return;
            }

            JsonValue markerObjectData = jsonData.get("layers").get(8).get("objects");
            Vector2[] enemyPosList = new Vector2[markerObjectData.size];
            for(int e = 0; e < enemyPosList.length; e++) {
                String markerName = markerObjectData.get(e).get("name").asString();
                if (markerName.equals(enemyName)) {
                    int orderNum = (jsonData.get("layers").get(8).get("objects").get(e).get("type")
                            .asInt());
                    float markerX = markerObjectData.get(e).get("x").asFloat();
                    float markerY = maxY - markerObjectData.get(e).get("y").asFloat();

                    Vector2 marker = new Vector2(drawToScreenCoordinates(markerX), drawToScreenCoordinates(markerY));
                    enemyPosList[orderNum - 1] = marker;
                }
            }

            if (enemies[i] != null) {
                Board board = new Board(genericObstacles, enemies);
                if(i == 0) {
                    controls.add(new AIController(enemies[i], ursa, null, enemyPosList, true));
                } else {
                    controls.add(new AIController(enemies[i], ursa, null, enemyPosList));
                }
            }
        }
    }


    /**
     * Renders the player into the world by parsing JSON
     */
    public void renderUrsa() {
        float playerX = jsonData.get("layers").get(9).get("objects").get(0).get(8).asFloat();
        float playerY = maxY - jsonData.get("layers").get(9).get("objects").get(0).get(9).asFloat();

        JsonValue ursaConstants = constants.get("ursa");
        float playerWidth = ursaConstants.get("width").asFloat();
        float playerHeight = ursaConstants.get("height").asFloat();

        ursa = new UrsaModel(drawToScreenCoordinates(playerX + ursaTexture.getRegionWidth() / 2f), drawToScreenCoordinates(playerY) + playerHeight / 2,
                ursaConstants, playerWidth, playerHeight);
        ursa.setDrawScale(scale);
        ursa.setTexture(playerWalkFilm);
        addObject(ursa);
    }

    /**
     * Renders all the trees into the map by parsing JSON
     */
    public void renderTrees() {
        if(jsonData.get("layers").get(3) == null) { return; }

        JsonValue treeConstants = constants.get("tree");
        JsonValue treeObjectData = jsonData.get("layers").get(3).get("objects");

        for(int i = 0; i < treeObjectData.size; i++) {
            int treeIndex = treeObjectData.get(i).get("gid").asInt();
            float x = treeObjectData.get(i).get(8).asFloat() + treeTextures[treeIndex - firstTreeIndex].getRegionWidth() / 2f;
            float y = maxY - treeObjectData.get(i).get(9).asFloat();

            Tree obj = new Tree(treeConstants.get("vertices").asFloatArray(),drawToScreenCoordinates(x),drawToScreenCoordinates(y));
            obj.setDrawScale(scale);
            obj.setTexture(treeTextures[treeIndex - firstTreeIndex]);
            if(treeIndex - firstTreeIndex == 1) {
                obj.putOnShakeCooldown();
            }
            obj.setName("tree" + i);

            addObject(obj);
            trees.add(obj);

            // Tree shadows
            ShadowModel model = new ShadowModel(new Vector2(obj.getX(), obj.getY()), 0.75f, 0.75f,
                    "tree shadow" + i, new Vector2(polarTreeShadow.getRegionWidth() / 2.0f, 85), scale);
            shadows.add(model);
            addObject(model);

            // ===================
            genericObstacles.add(new GenericObstacle(obj.getX(), obj.getY(),
                    obj.getWidth(), obj.getHeight()));
            // ===================
        }
    }

    /**
     * Renders all the caves into the map by parsing JSON
     */
    public void renderCaves() {
        if (jsonData.get("layers").get(2) == null) {
            return;
        }
        JsonValue caveConstants = constants.get("cave");
        JsonValue caveObjectData = jsonData.get("layers").get(2).get("objects");

        for (int i = 0; i < caveObjectData.size; i++) {
            float x = caveObjectData.get(i).get(8).asFloat() + polarCave.getRegionWidth() / 2f;
            float y = maxY - caveObjectData.get(i).get(9).asFloat() + polarCave.getRegionHeight() / 2f;

            Cave obj = new Cave(caveConstants.get("vertices").asFloatArray(),
                    drawToScreenCoordinates(x), drawToScreenCoordinates(y));
            obj.setDrawScale(scale);
            obj.setTexture(cavePortalFilm);
            obj.setName("cave" + i);

            addObject(obj);

            // Tree shadows
            ShadowModel model = new ShadowModel(new Vector2(obj.getX(), obj.getY()), 0.75f, 0.75f,
                    "cave shadow" + i, new Vector2(polarTreeShadow.getRegionWidth() / 2.0f, 85),
                    scale);
            shadows.add(model);
            addObject(model);

            // ===================g
            genericObstacles.add(new GenericObstacle(obj.getX(), obj.getY(),
                    obj.getWidth(), obj.getHeight()));
            // ===================
        }
    }

//    public void renderRocks() {
//        if (jsonData.get("layers").get(9) == null) {
//            return;
//        }
//        JsonValue rockObjectData = jsonData.get("layers").get(9).get("objects");
//
//        for (int i = 0; i < rockObjectData.size; i++) {
//            float x = rockObjectData.get(i).get(8).asFloat() + polarCave.getRegionWidth() / 2f;
//            float y = maxY - rockObjectData.get(i).get(9).asFloat() + polarCave.getRegionHeight() / 2f;
//
//            Moveable rock = new Moveable(drawToScreenCoordinates(x),drawToScreenCoordinates(y),3,3);
//            rock.setDrawScale(scale);
//            rock.setTexture(polarRock3);
//            rock.setName("rock"+i);
//            addObject(rock);
//        }
//    }

    /**
     * Renders smol ursa into the map by parsing JSON
     */
    public void renderSmolUrsa() {
        if (jsonData.get("layers").get(6) == null) {
            return;
        }
        JsonValue smolUrsaConstants = constants.get("smolursa");
        JsonValue smolUrsaObjectData = jsonData.get("layers").get(6).get("objects");

        float x = smolUrsaObjectData.get(0).get(8).asFloat()
                + smolUrsaTexture.getRegionWidth() / 2f;
        float y = maxY - smolUrsaObjectData.get(0).get(9).asFloat();


        goal = new GameObject(smolUrsaConstants.get("vertices").asFloatArray(), drawToScreenCoordinates(x),
                drawToScreenCoordinates(y));
        goal.setSensor(true);
        goal.setDrawScale(scale);
        goal.setTexture(smolUrsaIdleFilm);
        goal.setName("smolursa");
        addObject(goal);

        // ===================
        genericObstacles.add(new GenericObstacle(goal.getX(), goal.getY(),
                goal.getWidth(), goal.getHeight()));
        // ===================
    }

    /**
     * Renders all the game objects into the map by parsing JSON
     */
    public void renderGameObjects() {
        if(jsonData.get("layers").get(4) == null) { return; }

        JsonValue treeConstants = constants.get("tree");
        JsonValue objectData = jsonData.get("layers").get(4).get("objects");

        for(int i = 0; i < objectData.size; i++) {
            int objectIndex = objectData.get(i).get("gid").asInt();
            int textureIndex;
            if(objectIndex - firstMediumObjectIndex < objectTextures.length) {
                textureIndex = objectIndex - firstMediumObjectIndex;
            } else if(objectIndex - firstMediumRockIndex < numMediumRocks) {
                textureIndex = objectIndex - firstMediumRockIndex + 3;
            } else if(objectIndex - firstHouseIndex < 1) {
                textureIndex = objectIndex - firstHouseIndex + 5;
            } else if(objectIndex - firstLargeObjectIndex < objectTextures.length) {
                textureIndex = objectIndex - firstLargeObjectIndex + 6;
            } else {
                System.out.println("Unidentified object (UFO).");
                continue;
            }

            float x = objectData.get(i).get(8).asFloat() + objectTextures[textureIndex].getRegionWidth() / 2f;
            float y = maxY - objectData.get(i).get(9).asFloat();

            GameObject obj = new GameObject(treeConstants.get("vertices").asFloatArray(),drawToScreenCoordinates(x),drawToScreenCoordinates(y));
            obj.setDrawScale(scale);
            obj.setTexture(objectTextures[textureIndex]);
            obj.setName("game object" + i);

            addObject(obj);

            // Tree shadows
            ShadowModel model = new ShadowModel(new Vector2(obj.getX(), obj.getY()), 0.75f, 0.75f,
                    "tree shadow" + i, new Vector2(polarTreeShadow.getRegionWidth() / 2.0f, 85), scale);
            shadows.add(model);
            addObject(model);

            // ===================
            genericObstacles.add(new GenericObstacle(obj.getX(), obj.getY(),
                    obj.getWidth(), obj.getHeight()));
            // ===================
        }
    }

    /**
     * Renders all the decorations into the map by parsing JSON
     */
    public void renderDecorations() {
        if(jsonData.get("layers").get(1) == null) { return; }
        JsonValue decorationData = jsonData.get("layers").get(1).get("objects");

        for(int i = 0; i < decorationData.size; i++) {
            float x = decorationData.get(i).get(8).asFloat();
            float y = maxY - decorationData.get(i).get(9).asFloat();
            int decorationIndex = decorationData.get(i).get("gid").asInt();
            int textureIndex;
            if(decorationIndex - firstSmallDecorationIndex < decorationTextures.length) {
                textureIndex = decorationIndex -firstSmallDecorationIndex;
            } else if(decorationIndex - firstLargeDecorationIndex < decorationTextures.length) {
                textureIndex = decorationIndex - firstLargeDecorationIndex + 12;
            } else {
                System.out.println("Unidentified decoration.");
                continue;
            }
            Decoration decoration = new Decoration(decorationTextures[textureIndex], scale, drawToScreenCoordinates(x),drawToScreenCoordinates(y), decorationIndex);

            decorations.add(decoration);
        }
        decorations.sort(decorationComparator);
    }
}


