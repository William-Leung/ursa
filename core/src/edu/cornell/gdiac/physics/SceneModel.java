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
    private TextureRegion salmonTexture;
    /** Texture asset for tint over the whole screen */
    private Texture blankTexture;
    /** Texture asset for trees in the polar map (first is snow, second is no snow) */
    private TextureRegion[] treeTextures = new TextureRegion[2];
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
    /** Texture asset for cave portal animation */
    private TextureRegion cavePortalAnimation;

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
    /** Filmstrip for cave shake animation */
    private FilmStrip cavePortalFilm;

    /** =========== Animation Variables =========== */
    /** Current index of the player walk animation */
    private int ursaWalkAnimIndex = 0;
    /** Current index of the player idling animation */
    private int ursaIdleAnimIndex = 0;
    /** The frame at which Ursa began idling */
    private int ursaBeganIdlingFrame = 0;
    /** Player's current state: true corresponds to walking, false for idling */
    private boolean ursaCurrentState = false;
    /** Ursa's idle animates 1/ursaIdleAnimBuffer slower */
    private int ursaIdleAnimBuffer = 3;
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
    /** Tree shaking animates 1/treeShakeAnimBuffer slower */
    private int treeShakeAnimBuffer = 3;
    /** Current frame number (used to slow down animations) */
    private int currentFrame = 0;
    /** The frame at which the tree began shaking */
    private int beganShakingTreeFrame = 0;

    /** =========== Tree Shaking Variables =========== */
    /** The tree that is currently shaking. */
    private Tree shakingTree = null;
    /** How far away the player must be to interact with trees (screen coords) */
    private float treeInteractionRange = 3;
    /** Within what distance will the enemy be stunned upon tree shaking. */
    private float enemyStunDistance = 4;


    private float maxX;
    /** =========== Tiled Parsing Variables =========== */
    private float maxY;
    /** The index of the first tile in the tile sprite sheet */
    private int firstTileIndex = 19;
    /** The index of the first tree in the tree sprite sheet */
    private int firstTreeIndex = 34;
    /** The index of the first tile in the 64x64 decoration sprite sheet*/
    private int firstSmallDecorationIndex = 1;
    /** An array of TextureRegions containing all tile textures */
    private TextureRegion[] tileTextures = new TextureRegion[15];
    /** An array of TextureRegions containing all decoration textures */
    private TextureRegion[] decorationTextures = new TextureRegion[16];

    /** =========== Tile Variables =========== */
    /** Total number of tiles in the y direction on the board. */
    private float numTilesY;
    /** Total number of tiles in the x direction on the board. */
    private float numTilesX;
    /** Height of a single tile (75% of 256) */
    private float tileHeight = 256;
    /** Width of a single tile (tileHeight75% of 256) */
    private float tileWidth = 256;
    private float deprecatedTileWidth;
    private float deprecatedTileHeight;
    private float timeRatio;
    private Vector2[] enemyPosList = new Vector2[10];
    private JsonReader json;
    private JsonValue jsonData;


    // Physics objects for the game
    /** Physics constants for initialization */
    private JsonValue constants;
    /** Reference to the character avatar */
    private UrsaModel ursa;
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

    private PooledList<Decoration> decorations = new PooledList<>();

    /**
     * Information about the gameboard used for pathfinding
     */
    private Board gameBoard;

    /**
     * List of all references to all generic obstacles
     */
    private PooledList<GenericObstacle> genericObstacles = new PooledList<>();

    private int framesSinceTreeAnimation = 0;
    /** Reference to the goalDoor (for collision detection) */
    private PolygonObstacle goalDoor;
    /** Controller for all dynamic shadows */
    private ShadowController shadowController;
    /** rock to be used to reset the day if interacted with */
    private GenericObstacle specialRock = null;


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
    /** Y offset of UI element, 0 represents half way showing */
    private float uiYOffset = 0f;
    /** How long the UI element will rise and fall for */
    private float uiRisingDuration = 0.05f;
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
        json = new JsonReader();
        jsonData = json.parse(Gdx.files.internal(levelJson));

        numTilesY = jsonData.get("layers").get(0).get(1).asFloat();
        numTilesX = jsonData.get("layers").get(0).get(7).asFloat();
        deprecatedTileHeight = numTilesY;
        deprecatedTileWidth = numTilesX;

        maxY = numTilesY * 256f;
        maxX = deprecatedTileWidth * 256f;


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

        treeTextures[0] = new TextureRegion(directory.getEntry("object:tundra_tree_snow_small", Texture.class));
        treeTextures[1] = new TextureRegion(directory.getEntry("object:tundra_tree",Texture.class));
        polarHouse = new TextureRegion(directory.getEntry("object:polar_house", Texture.class));
        polarRock1 = new TextureRegion(directory.getEntry("object:polar_rock_1", Texture.class));
        polarRock2 = new TextureRegion(directory.getEntry("object:polar_rock_2", Texture.class));
        polarRock3 = new TextureRegion(directory.getEntry("object:polar_rock_3", Texture.class));
        polarRock4 = new TextureRegion(directory.getEntry("object:polar_rock_4", Texture.class));
        polarTrunk1 = new TextureRegion(directory.getEntry("object:polar_trunk_1", Texture.class));
        polarTrunk2 = new TextureRegion(directory.getEntry("object:polar_trunk_2", Texture.class));
        polarCave = new TextureRegion(directory.getEntry("polar:cave",Texture.class));

        polarTreeShadow = new TextureRegion(directory.getEntry("shadows:polar_tree_shadow", Texture.class));
        polarTreeShadow.flip(true, true);

        playerWalkTextureAnimation = new TextureRegion(directory.getEntry("player:ursaWalk",Texture.class));
        playerWalkFilm = new FilmStrip(playerWalkTextureAnimation.getTexture(),3,8);
        playerIdleTextureAnimation = new TextureRegion(directory.getEntry("player:ursaIdle",Texture.class));
        playerIdleFilm = new FilmStrip(playerIdleTextureAnimation.getTexture(),4,8);

        salmonUprightWalkAnimation = new TextureRegion(directory.getEntry("enemies:salmonUprightWalk",Texture.class));
        salmonUprightWalkFilm = new FilmStrip(salmonUprightWalkAnimation.getTexture(),3,8);
        salmonConfusedAnimation = new TextureRegion(directory.getEntry("enemies:salmonConfused",Texture.class));
        salmonConfusedFilm = new FilmStrip(salmonConfusedAnimation.getTexture(),4,8);
        salmonIdleAnimation = new TextureRegion(directory.getEntry("enemies:salmonIdle",Texture.class));
        salmonIdleFilm = new FilmStrip(salmonIdleAnimation.getTexture(), 5, 8);
        salmonDetectedAnimation = new TextureRegion(directory.getEntry("enemies:salmonDetected",Texture.class));
        salmonDetectedFilm = new FilmStrip(salmonDetectedAnimation.getTexture(), 4, 8);

        treeShakeAnimation = new TextureRegion(directory.getEntry("object:polar_tree_shake",Texture.class));
        treeShakeFilm = new FilmStrip(treeShakeAnimation.getTexture(), 2, 8);

        cavePortalAnimation = new TextureRegion(directory.getEntry("polar:cave_animation",Texture.class));
        cavePortalFilm = new FilmStrip(cavePortalAnimation.getTexture(), 2, 8);

        levelMusic = directory.getEntry("soundtracks:level_track", Music.class);
        levelMusicNight = directory.getEntry("soundtracks:level_track_night", Music.class);
        levelMusicTense = directory.getEntry("soundtracks:level_track_tense", Music.class);

        gatherTiles(directory);
        gatherDecorations(directory);

        constants = directory.getEntry( "polar:constants", JsonValue.class );
        super.gatherAssets(directory);
    }
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
    }

    /**
     * Resets the status of the game so that we can play again.
     *
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

    private TextureRegion getRockTexture(String type) {
        switch (type) {
            case "1":
                return polarRock1;
            case "2":
                return polarRock2;
            case "3":
                return polarRock3;
            case "4":
                return polarRock4;
        }
        return polarRock1;
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {

        deprecatedTileHeight = jsonData.get("layers").get(0).get(1).asFloat();
        deprecatedTileWidth = jsonData.get("layers").get(0).get(7).asFloat();

        // Add level goal
        float dwidth;
        float dheight;

        // create shadow (idk if this does anything even)
        shadowController = new ShadowController();

        /**
         * Parsing Ursa
         */
        float playerX = jsonData.get("layers").get(1).get("objects").get(0).get(8).asFloat();
        float playerY = maxY - jsonData.get("layers").get(1).get("objects").get(0).get(9).asFloat();

        JsonValue ursaConstants = constants.get("ursa");
        float playerWidth = ursaConstants.get("width").asFloat();
        float playerHeight = ursaConstants.get("height").asFloat();

        ursa = new UrsaModel(drawToScreenCoordinates(playerX + tileWidth / 2), drawToScreenCoordinates(playerY) + playerHeight / 2,
                ursaConstants, playerWidth, playerHeight);
        ursa.setDrawScale(scale);
        ursa.setTexture(playerWalkFilm);
        addObject(ursa);

        renderTrees();
        renderCaves();
        renderDecorations();

        /**
         * This loop renders each cave in a given map.
         */
        JsonValue currLayer = jsonData.get("layers").get(6);
        for (int j = 0; j < currLayer.get("objects").size; j++) {
            float x = currLayer.get("objects").get(j).get(8).asFloat();
            float y = (maxY - (currLayer.get("objects").get(j).get(9).asFloat()));

            JsonValue currConstants = constants.get(currLayer.get("name").asString());

            goalDoor = new GameObject(currConstants.get("vertices").asFloatArray(), x / 64 + 4,
                    y / 64 + 8, currConstants.get("xScale").asFloat(),
                    currConstants.get("yScale").asFloat());
            goalDoor.setSensor(true);
            goalDoor.setDrawScale(scale);
            goalDoor.setTexture(getTextureRegion(currConstants.get("texture").asString()));
            goalDoor.setName("cave");

            // Cave shadows
            ShadowModel shadow = new ShadowModel(
                    new Vector2(goalDoor.getX(), goalDoor.getY()), 0.75f, 0.75f,
                    goalDoor.getName(), goalDoor.getDrawOrigin(), scale);
            shadows.add(shadow);

            addObject(shadow);
            addObject(goalDoor);

            // ===================
            genericObstacles.add(new GenericObstacle(goalDoor.getX(), goalDoor.getY(),
                    goalDoor.getWidth(), goalDoor.getHeight()));
            // ===================
        }




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
        // The array needs to be parsed from top to bottom
        for (int i = (int) numTilesY - 1; i >= 0 ; i--) {
            for(int j = 0; j < numTilesX; j++){
                int tileIndex = jsonData.get("layers").get(0).get(0).get(counter++).asInt();
                if(tileIndex == 0 ) {
                    continue;
                }
                canvas.draw(tileTextures[tileIndex - firstTileIndex], Color.WHITE,0,0,j * 12f * scale.x,i * 12f * scale.y,ursa.getAngle(), 0.75f,0.75f);
            }
        }

//        canvas.draw(redTextureRegion,Color.WHITE, 0f,256* deprecatedTileHeight, 10,10);
//        canvas.draw(redTextureRegion,Color.WHITE, 0f,0 * 256, 10,10);
//        canvas.draw(redTextureRegion,Color.WHITE, 0f,192, 10,10);


    }
    public void drawWalls(){
        int counter = 0;
        InvivisbleWall wall;
        for (int i = (int) deprecatedTileHeight; i >0 ; i--) {

            for(int j = 0; j < deprecatedTileWidth;j++){



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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(5.5f + (j* 8),1+(i*8),.3f,2f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(6.5f + (j* 8),2.8f+(i*8),2.5f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);


                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(3f + (j* 8),1.4f+(i*8),.3f,2.8f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(2.5f + (j* 8),6.8f+(i*8),.3f,2.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(5.5f + (j* 8),6.8f+(i*8),.3f,2.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(2.5f + (j* 8),7f+(i*8),.3f,2f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(3.7f + (j* 8),3.7f+(i*8),.3f,4f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall( 3.9f + (j* 8),3.8f+(i*8),.3f,2.5f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall( 5.3f + (j* 8),1.1f+(i*8),.3f,2.0f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(3f + (j* 8),1.3f+(i*8),.3f,2.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(3.8f + (j* 8),3.3f+(i*8),.3f,1.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(3f + (j* 8),6.3f+(i*8),.3f,4.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                    wall = new InvivisbleWall(5.2f + (j* 8),2.5f+(i*8),.3f,4.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

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

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================

                }
                else if (jsonData.get("layers").get(0).get(0).get(counter).asInt() == firstTileIndex+6){
                    wall = new InvivisbleWall( (j* 8),3+ (i*8),1f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================


                    wall = new InvivisbleWall( 1+(j* 8),3.2f+ (i*8),1f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================


                    wall = new InvivisbleWall( 1.8f+(j* 8),3.5f+ (i*8),1f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================


                    wall = new InvivisbleWall( 2.8f+(j* 8),4.0f+ (i*8),1f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================


                    wall = new InvivisbleWall( 3.5f+(j* 8),4.5f+ (i*8),1f,.3f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================


                    wall = new InvivisbleWall( 4.5f+(j* 8),5.2f+ (i*8),.1f,2f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    wall.setAngle(-0.523599f);
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================


                    wall = new InvivisbleWall( 5.15f+(j* 8),6.9f+ (i*8),.1f,2f);
                    wall.setDensity(0);
                    wall.setFriction(0);
                    wall.setRestitution(0);
                    wall.setDrawScale(scale);
                    wall.setName("invis wall " + (i + j));
                    wall.setAngle(-.2f);
                    addObject(wall);

                    // ===================
                    genericObstacles.add(new GenericObstacle(wall.getX(), wall.getY(),
                            wall.getWidth(), wall.getHeight()));
                    // ===================
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
            }
            ursaIdleAnimIndex = 0;
            // Rewind the film
            if(ursaWalkAnimIndex == 20){
                ursaWalkAnimIndex = 0;
            }
            playerWalkFilm.setFrame(ursaWalkAnimIndex);
            ursa.setTexture(playerWalkFilm);
            ursaWalkAnimIndex += 1;
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
        //System.out.println("player pos y is: " + avatar.getPosition().y);

        // Update the UI element to rise at sunrise and sunset and rotate if it's night
        if(timeRatio < uiRisingDuration) {
            uiYOffset = timeRatio / uiRisingDuration;
        } else if(timeRatio > 1f){
            uiRotationAngle = -timeRatio * (float) Math.PI + (float) Math.PI;
        } else if(timeRatio > 1f - uiRisingDuration){
            uiYOffset = (1f - timeRatio) / uiRisingDuration;
        }

        // Reset the rotation just before the UI rises and falls
        if(timeRatio == 0f) {
            uiRotationAngle = (float) Math.PI;
        } else if(timeRatio == 1f - uiRisingDuration) {
            uiRotationAngle = 0f;
        }

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
            if ((bd1 == ursa   && bd2 == goalDoor) ||
                    (bd1 == goalDoor && bd2 == ursa)) {
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
        canvas.draw(snowBackGround,Color.WHITE,0,0, deprecatedTileWidth * 256, deprecatedTileHeight
                *256);
        drawTiles();
        for(Decoration d: decorations) {
            d.draw(canvas);
        }
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

//        for(AIController control: controls) {
//            for(Vector2 v: control.getPatrol()) {
//                if(v != null) {
//                    canvas.draw(redTextureRegion,Color.WHITE, v.x * 32f,
//                            v.y * 32f, 10,10);
//                }
//            }
//        }
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

        float dwidth = dayNightUITexture.getRegionWidth()  / 2;
        float dheight = dayNightUITexture.getRegionHeight() / 2;

        canvas.draw(dayNightUITexture, Color.WHITE, dwidth, dheight, canvas.getCameraX(), canvas.getCameraY() + canvas.getHeight() / 2 + uiYOffset * uiDrawScale * dheight, uiRotationAngle, uiDrawScale, uiDrawScale);
    }

    public TextureRegion getTextureRegion(String s) {
        if(s.equals("cave")) {
            return polarCave;
        }
        throw new RuntimeException("Texture not recognized. Ask William if you get this. ");
    }

    public float drawToScreenCoordinates(float drawCoord) {
        // 0.75 converts between 2560 x 1440 resolution assets were drawn for and the 1920 x 1080 resolution we have
        // Then, convert to screen coordinates using the scale
        return drawCoord * 0.75f / scale.x;
    }

    /**
     * Renders all the trees on the map by reading them from JSON
     */
    public void renderTrees() {
        if(jsonData.get("layers").get(4) == null) { return; }
        JsonValue treeConstants = constants.get("tree");
        for(int i = 0; i < jsonData.get("layers").get(4).get("objects").size; i++) {
            int treeIndex = jsonData.get("layers").get(4).get("objects").get(i).get("gid").asInt();
            float x = jsonData.get("layers").get(4).get("objects").get(i).get(8).asFloat() + treeTextures[treeIndex - firstTreeIndex].getRegionWidth() / 2f;
            float y = maxY - jsonData.get("layers").get(4).get("objects").get(i).get(9).asFloat();

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
     * This loop renders the decorations
     */
    public void renderDecorations() {
        if(jsonData.get("layers").get(8) == null) { return; }
        for(int i = 0; i < jsonData.get("layers").get(8).get("objects").size; i++) {
            float x = jsonData.get("layers").get(8).get("objects").get(i).get(8).asFloat();
            float y = maxY - jsonData.get("layers").get(8).get("objects").get(i).get(9).asFloat();
            int decorationIndex = jsonData.get("layers").get(8).get("objects").get(i).get("gid").asInt();
            Decoration decoration = new Decoration(decorationTextures[decorationIndex - firstSmallDecorationIndex], scale, drawToScreenCoordinates(x),drawToScreenCoordinates(y));

            decorations.add(decoration);
        }
    }

    public void renderCaves() {
        if(jsonData.get("layers").get(5) == null) { return; }
        JsonValue treeConstants = constants.get("cave");
        for(int i = 0; i < jsonData.get("layers").get(5).get("objects").size; i++) {
            float x = jsonData.get("layers").get(5).get("objects").get(i).get(8).asFloat() + polarCave.getRegionWidth() / 2f;
            float y = maxY - jsonData.get("layers").get(5).get("objects").get(i).get(9).asFloat() + polarCave.getRegionHeight() / 2f;

            Cave obj = new Cave(treeConstants.get("vertices").asFloatArray(),drawToScreenCoordinates(x),drawToScreenCoordinates(y));
            obj.setDrawScale(scale);
            obj.setTexture(cavePortalFilm);
            obj.setName("cave" + i);

            addObject(obj);

            // Tree shadows
            ShadowModel model = new ShadowModel(new Vector2(obj.getX(), obj.getY()), 0.75f, 0.75f,
                    "cave shadow" + i, new Vector2(polarTreeShadow.getRegionWidth() / 2.0f, 85), scale);
            shadows.add(model);
            addObject(model);

            // ===================g
            genericObstacles.add(new GenericObstacle(obj.getX(), obj.getY(),
                    obj.getWidth(), obj.getHeight()));
            // ===================
        }
    }
}
