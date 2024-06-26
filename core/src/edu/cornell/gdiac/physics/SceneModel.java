package edu.cornell.gdiac.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.objects.CustomGameObject;
import edu.cornell.gdiac.physics.pathing.EnemyMarker;
import edu.cornell.gdiac.physics.units.Enemy;
import edu.cornell.gdiac.physics.objects.Cave;
import edu.cornell.gdiac.physics.objects.Decoration;
import edu.cornell.gdiac.physics.objects.GameObject;
import edu.cornell.gdiac.physics.objects.Moveable;
import edu.cornell.gdiac.physics.obstacle.*;
import edu.cornell.gdiac.physics.objects.GenericObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.pathing.AIController;
import edu.cornell.gdiac.physics.pathing.Board;
import edu.cornell.gdiac.physics.units.UrsaModel;
import edu.cornell.gdiac.physics.shadows.ShadowController;
import edu.cornell.gdiac.physics.shadows.ShadowModel;
import edu.cornell.gdiac.physics.objects.Tree;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.PooledList;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * This class contains references to all game objects and stores logic for update loops, where it animates and moves objects.
 * It also parses level JSON data into corresponding game objects.
 */
public class SceneModel extends WorldController implements ContactListener {


    /* =========== Textures =========== */
    /** Texture asset for day night UI */
    private TextureRegion dayNightUITexture;
    /** Texture asset for bipedal (2 legs) salmon */
    private TextureRegion salmonTexture;
    /** Texture asset for salmon shadow texture */
    private TextureRegion salmonShadowTexture;
    /** Texture asset for ursa texture */
    private TextureRegion ursaTexture;
    /** Texture asset for ursa shadow texture */
    private TextureRegion ursaShadowTexture;
    /** Texture asset for smol ursa texture */
    private TextureRegion smolUrsaTexture;
    private TextureRegion bearIndicator;
    private TextureRegion arrowIndicator;

    /** Texture asset for trees in the polar map (first is snow, second is no snow) */
    private final TextureRegion[] treeTextures = new TextureRegion[2];
    /** Texture asset for objects in the polar map */
    private final TextureRegion[] objectTextures = new TextureRegion[9];
    private final TextureRegion[] dialogueTextures = new TextureRegion[7];
    /** Texture asset for the cave in the polar map */
    private TextureRegion polarCaveTexture;
    /** Texture asset for the portal in the polar map */
    private TextureRegion polarPortalTexture;
    /** Texture asset for the ZZZ fully realized in the polar map */
    private TextureRegion polarZZZTexture;
    /** Texture asset for the ice in the polar map (moveable) */
    private TextureRegion polarIceTexture;
    /** Texture asset for a single white pixel (tinting) */
    protected TextureRegion whiteTexture;
    /** Texture asset for a single black pixel (shadows) */
    protected TextureRegion blackTexture;
    /** Texture asset for the ground */
    private TextureRegion groundTexture;
    private boolean paused;
    private boolean doGriddy;


    /* =========== Film Strips =========== */
    /** Filmstrip for cave portal whirl animation */
    private FilmStrip cavePortalFilm;
    /** Filmstrip for cave displaying sleep ui animation */
    private FilmStrip caveZZZFilm;
    /** Filmstrip for cave looping sleep ui animation */
    private FilmStrip caveZZZLoopFilm;
    /** Filmstrip for player walking animation */
    private FilmStrip playerWalkFilm;
    /** Filmstrip for player idling animation */
    private FilmStrip playerIdleFilm;
    /** Filmstrip for salmon walking animation */
    private FilmStrip salmonUprightWalkFilm;
    /** Filmstrip for salmon confused animation */
    private FilmStrip salmonConfusedFilm;
    /** Filmstrip for salmon idling animation */
    private FilmStrip salmonIdleFilm;
    /** Filmstrip for salmon detecting animation */
    private FilmStrip salmonDetectedFilm;
    /** Filmstrip for little ursa idle animation */
    private FilmStrip smolUrsaIdleFilm;
    /** Filmstrip for little ursa rescue animation */
    private FilmStrip smolUrsaRescueFilm;
    /** Filmstrip for tree shaking animation */
    private FilmStrip treeShakeFilm;
    /** Filmstrip for enemy diving animation */
    private FilmStrip salmonDiveFilm;
    /** Filmstrip for player caught animation */
    private FilmStrip playerCaughtFilm;
    /** Filmstrip for player rescue animation */
    private FilmStrip playerRescueFilm;
    /** Array of film strips for sun animations */
    private FilmStrip[] sunAnimations = new FilmStrip[3];
    /** Which of the 3 sun animations we will play */
    private int sunIndex = 0;

    /* =========== Animation Variables =========== */
    /** Current frame number (used to slow down animations) */
    private int currentFrame = 0;
    /** The frame at which Ursa began idling */
    private int ursaBeganWalkingFrame = 0;
    /** The frame at which Ursa began idling */
    private int ursaBeganIdlingFrame = 0;
    /** Player's current state: true corresponds to walking, false for idling */
    private boolean ursaCurrentState = false;
    /** Current index of the salmon walking animation */
    private int salmonWalkAnimIndex = 0;
    /** Current index of the salmon idling animation */
    private int salmonIdleAnimIndex = 0;
    /** Current index of the salmon detection animation */
    private int salmonDetectedIndex = 0;
    /** The frame at which the tree began shaking */
    private int beganShakingTreeFrame = 0;
    /**
     * A default listing of cave rotations if none are provided.
     * Stores the starting rotation of shadows and subsequent rotations when caves are interacted
     * */
    private float[] caveRotations = new float[]{0, 90, 180, 360, 720};
    /** Pointer to the current rotation of the shadows in caveRotations. */
    private int currCaveRotation = 0;


    /* =========== Tree Shaking Variables =========== */
    /** The tree that is currently shaking. */
    private Tree shakingTree = null;
    private float timer;
    private boolean startWin;
    private boolean startLose;


    /* =========== Cave Interaction Variables =========== */
    /** Is the time fast forwarding right now? */
    private boolean isTimeSkipping = false;
    /** The frame at which time began to skip */
    private int timeBeganSkippingFrame = 0;
    private TextureRegion[] pauseScreen = new TextureRegion[3];
    /** The cave recently interacted with. */
    private Cave interactedCave = null;
    /** The position at which Ursa was when interacting with the cave.*/
    private Vector2 ursaStartingPosition;
    /** How long Ursa will walk to the cave for */
    private int walkingDuration = 0;
    /** How long the UI for cave transition will play */
    private final int transitionDuration = 142;


    /* =========== Tiled Parsing Variables =========== */
    /** Maximum Y Coordinate (Screen) */
    private final float maxY;
    /** JSON containing all the information for the level */
    private final JsonValue jsonData;
    /** Constants used for initialization (width, height, scaling, etc) */
    private JsonValue constants;
    /** The index of the first tile in the tile sprite sheet */
    private int firstTileIndex;
    /** The index of the first tree in the tree sprite sheet */
    private int firstTreeIndex;
    /** The index of the first small direction in the 64x64 decoration sprite sheet */
    private int firstSmallDecorationIndex;
    /** The index of the first large decoration in the 512x512 decoration sprite sheet */
    private int firstLargeDecorationIndex;
    /** The index of the first large decoration in the 1024x512 decoration sprite sheet */
    private int firstLargeOceanDecorationIndex;
    /** The index of the first medium object in the 256x256 decoration sprite sheet */
    private int firstMediumObjectIndex;
    /** The index of the first medium rock in the 256x256 sprite sheet */
    private int firstMediumRockIndex;
    /** The index of the first house in terms of all textures in json*/
    private int firstHouseIndex;
    /** The index of trunk 1 in terms of all textures in json*/
    private int polarTrunk1Index;
    /** The index of trunk 2 in terms of all textures in json*/
    private int polarTrunk2Index;
    private ParticleEffect effect;
    /** The index of rock 2 in terms of all textures in json*/
    private int polarRock2Index;
    /** Scaling between textures and drawing (256x256 -> 192x192)
     * Converts between 2560 x 1440 resolution assets were drawn for and 1024 x 576 resolution we have
     */
    private final float textureScale = 0.4f;


    /* =========== Tile Variables =========== */
    /** Total number of tiles in the y direction on the board. */
    private final float numTilesY;
    /** Total number of tiles in the x direction on the board. */
    private final float numTilesX;
    /** 2d array of tile textures */
    private int[][] tiles;
    /** Height of the player hitbox. */
    private float playerHeight;
    /** The starting rotation of all shadows for this level. */
    private float shadowStartingRotation;

    /* =========== Collections of References =========== */
    /** Reference to the character avatar */
    private UrsaModel ursa;
    /** An array of TextureRegions containing all tile textures */
    private final TextureRegion[] tileTextures = new TextureRegion[15];
    /** An array of TextureRegions containing all decoration textures */
    private final TextureRegion[] decorationTextures = new TextureRegion[21];
    /** List of references to enemies */
    private Enemy[] enemies;
    /** List of references to all AIControllers */
    private final LinkedList<AIController> controls = new LinkedList<>();
    /** List of references to all interactable trees */
    private final PooledList<Tree> interactableTrees = new PooledList<>();
    /** List of references to all decorations */
    private final PooledList<Decoration> decorations= new PooledList<>();
    /** List of references to decorations on the ground */
    private final PooledList<Decoration> groundDecorations = new PooledList<>();
    /** List of references to decorations in the ocean */
    private final PooledList<Decoration> oceanDecorations = new PooledList<>();

    /** List of references to all interactable caves */
    private final PooledList<Cave> interactableCaves = new PooledList<>();
    /** List of references to dynamic objects (ursa + enemies) */
    private final PooledList<Obstacle> dynamicObjects = new PooledList<>();


    /* =========== Day/Night Screen Tinting =========== */
    /** Background color that changes with the day */
    protected Color backgroundColor = Color.BLACK;
    /** A list of colors that the tinting with linear interpolate between */
    private final Color[] colors;
    /** The intervals at which the tintings will occur
     * colors[i] happens at intervals[i] */
    private final float[] intervals;
    /** Points to the next color we interpolate to */
    private int colorNextPointer = 1;
    /** The darkest tinting of any shadow */
    private static final float shadowAlpha = 0.35f;

    /*
     * Initialize the global blob shadow used for Ursa and enemies.
     * Since it's a shared texture, we can just use it statically across everything to make it easier.
     */

    private static final int BLOB_SHADOW_RESOLUTION = 512; // High resolution for lesser edge cuts (in theory that is)

    public static final Texture BLOB_SHADOW_TEXTURE;

    private static final int DIVE_ANIM_DIFF = 10;
    private static final int ENEMY_DIVE_FRAMES = 49;
    int player_dive_anim = 0;
    boolean player_caught = false;
    private TextureRegion[] retryTextures = new TextureRegion[4];

    static {
        Pixmap pixmap = new Pixmap(BLOB_SHADOW_RESOLUTION * 2, BLOB_SHADOW_RESOLUTION * 2, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0f,0f,0f,shadowAlpha));
        int xcenter = pixmap.getWidth() / 2;
        int ycenter = pixmap.getHeight() / 2;
        pixmap.fillCircle(xcenter, ycenter, BLOB_SHADOW_RESOLUTION);
        BLOB_SHADOW_TEXTURE = new Texture(pixmap);
        pixmap.dispose();
    }


    /* =========== UI Constants =========== */
    /** Time as a float where 0 represents sunrise, 0.5 is sunset. Always stays between 0 and 1. */
    private float timeRatio;
    /** Y Offset of the horizontal black bars*/
    private float barYOffset = 0f;
    /** How long the bars will rise for */
    private final int barRiseDuration = 20;
    /** Height of the black bars */
    private float barHeight = 100f;
    private float blackTextureAlpha;
    private float caughtBarXOffset = 0;
    private float ursaCaughtXOffset = 0;

    private int beganStartLoseFrame = 0;
    private int caughtBarMoveDuration = 60;
    private int ursaCaughtMoveDuration = 10;


    /* =========== Soundtrack Assets =========== */
    private Music levelMusic;
    private Music levelMusicNight;
    private Music levelMusicTense;


    /**
     * List of all references to all generic obstacles
     */
    private final PooledList<GenericObstacle> genericObstacles = new PooledList<>();

    /** Reference to goal */
    private PolygonObstacle goal;
    /** Controller for all dynamic shadows */
    private ShadowController shadowController = new ShadowController();
    private final Comparator<Decoration> decorationComparator = (o1, o2) -> Float.compare(o2.getIndex(), o1.getIndex());
    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    private FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), false);

    private boolean hasWon = false;
    private float newTimer;
    private boolean reverse;



    /**
     * Creates and initialize a new instance of the platformer game
     * The game has default gravity and other settings
     */
    public SceneModel(String levelJson) {
        super(DEFAULT_WIDTH,DEFAULT_HEIGHT,DEFAULT_GRAVITY);
        doGriddy = false;
        setDebug(false);
        setComplete(false);
        setFailure(false);
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<>();
        JsonReader json = new JsonReader();
        jsonData = json.parse(Gdx.files.internal(levelJson));
        paused = false;
        numTilesY = jsonData.get("layers").get(0).get("height").asFloat();
        numTilesX = jsonData.get("layers").get(0).get("width").asFloat();
        timer = 30;
        float tileSideLength = 256;
        maxY = numTilesY * tileSideLength;
        newTimer = 0;
        colors = new Color[5];
        intervals = new float[5];
        colors[0] = new Color(1f,1f,1f,0f);
        intervals[0] = 0f;
        colors[1] = new Color(1f,1f,1f,0f);
        intervals[1] = 0.4f;
        colors[2] = new Color(0f,0f,0f,shadowAlpha);
        intervals[2] = 0.5f;
        colors[3] = new Color(0f,0f,0f,shadowAlpha);
        intervals[3] = 0.9f;
        colors[4] = new Color(1f,1f,1f,0f);
        intervals[4] = 1.0f;
        blackTextureAlpha = -0.2f;
        effect = new ParticleEffect();
        effect.load(Gdx.files.internal("particle.p"),Gdx.files.internal(""));
    }
    /**
     * Gather the assets for this controller.
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     *
     * @param directory	Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        whiteTexture = new TextureRegion(directory.getEntry("polar:white", Texture.class));
        blackTexture = new TextureRegion(directory.getEntry("polar:black", Texture.class));
        groundTexture = new TextureRegion(directory.getEntry("polar:ground", Texture.class));
        dayNightUITexture = new TextureRegion(directory.getEntry("ui:dayNightUI", Texture.class));
        salmonTexture = new TextureRegion(directory.getEntry("enemies:salmon", Texture.class));
        salmonShadowTexture = new TextureRegion(directory.getEntry("enemies:salmonShadow", Texture.class));
        ursaTexture = new TextureRegion(directory.getEntry("player:ursa", Texture.class));
        ursaShadowTexture = new TextureRegion(directory.getEntry("player:ursaShadow", Texture.class));
        smolUrsaTexture = new TextureRegion(directory.getEntry("smolursa:model", Texture.class));
        bearIndicator = new TextureRegion(directory.getEntry("smolursa:bearIndicator", Texture.class));
        arrowIndicator = new TextureRegion(directory.getEntry("smolursa:arrowIndicator", Texture.class));

        pauseScreen[0] = new TextureRegion(directory.getEntry("UI:pause1", Texture.class));
        pauseScreen[1] = new TextureRegion(directory.getEntry("UI:pause2", Texture.class));
        pauseScreen[2] = new TextureRegion(directory.getEntry("UI:pause3", Texture.class));
        treeTextures[0] = new TextureRegion(directory.getEntry("polar:tree_snow", Texture.class));
        treeTextures[1] = new TextureRegion(directory.getEntry("polar:tree_no_snow",Texture.class));
        retryTextures[0] =  new TextureRegion(directory.getEntry( "retry:lose1",Texture.class));
        retryTextures[1] =  new TextureRegion(directory.getEntry( "retry:lose2",Texture.class));
        retryTextures[2] =  new TextureRegion(directory.getEntry( "retry:victory1",Texture.class));
        retryTextures[3] =  new TextureRegion(directory.getEntry( "retry:victory2",Texture.class));

        dialogueTextures[0] = new TextureRegion(directory.getEntry( "tutorial:avoidShadow1",Texture.class));
        dialogueTextures[1] = new TextureRegion(directory.getEntry( "tutorial:avoidShadow2",Texture.class));
        dialogueTextures[2] = new TextureRegion(directory.getEntry( "tutorial:goToCave1",Texture.class));
        dialogueTextures[3] = new TextureRegion(directory.getEntry( "tutorial:goToCave2",Texture.class));
        dialogueTextures[4] = new TextureRegion(directory.getEntry( "tutorial:shakeTree1",Texture.class));
        dialogueTextures[5] = new TextureRegion(directory.getEntry( "tutorial:shakeTree2",Texture.class));
        dialogueTextures[6] = new TextureRegion(directory.getEntry( "tutorial:shakeTree3",Texture.class));


        polarCaveTexture = new TextureRegion(directory.getEntry("polar:cave",Texture.class));
        polarPortalTexture = new TextureRegion(directory.getEntry("polar:portal",Texture.class));
        polarZZZTexture = new TextureRegion(directory.getEntry("polar:ZZZ",Texture.class));
        polarIceTexture = new TextureRegion(directory.getEntry("polar:ice",Texture.class));

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
    public void gatherAnimations(AssetDirectory directory) {
        TextureRegion playerWalkTextureAnimation = new TextureRegion(
                directory.getEntry("player:ursaWalk", Texture.class));
        playerWalkFilm = new FilmStrip(playerWalkTextureAnimation.getTexture(), 2, 16);
        TextureRegion playerIdleTextureAnimation = new TextureRegion(
                directory.getEntry("player:ursaIdle", Texture.class));
        playerIdleFilm = new FilmStrip(playerIdleTextureAnimation.getTexture(), 4, 16);
        TextureRegion playerCaughtAnimation = new TextureRegion(
                directory.getEntry("player:ursaDown", Texture.class));
        playerCaughtFilm = new FilmStrip(playerCaughtAnimation.getTexture(), 2, 16);
        TextureRegion playerRescueAnimation = new TextureRegion(
                directory.getEntry("player:ursaRescue", Texture.class));
        playerRescueFilm = new FilmStrip(playerRescueAnimation.getTexture(), 4, 16);

        TextureRegion salmonUprightWalkAnimation = new TextureRegion(
                directory.getEntry("enemies:salmonUprightWalk", Texture.class));
        salmonUprightWalkFilm = new FilmStrip(salmonUprightWalkAnimation.getTexture(), 2, 16);
        TextureRegion salmonConfusedAnimation = new TextureRegion(
                directory.getEntry("enemies:salmonConfused", Texture.class));
        salmonConfusedFilm = new FilmStrip(salmonConfusedAnimation.getTexture(), 2, 16);
        TextureRegion salmonIdleAnimation = new TextureRegion(
                directory.getEntry("enemies:salmonIdle", Texture.class));
        salmonIdleFilm = new FilmStrip(salmonIdleAnimation.getTexture(), 3, 16);
        TextureRegion salmonDetectedAnimation = new TextureRegion(
                directory.getEntry("enemies:salmonDetected", Texture.class));
        salmonDetectedFilm = new FilmStrip(salmonDetectedAnimation.getTexture(), 2, 16);
        TextureRegion salmonDiveAnimation = new TextureRegion(
                directory.getEntry("enemies:salmonDive", Texture.class));
        salmonDiveFilm = new FilmStrip(salmonDiveAnimation.getTexture(), 4, 16);

        TextureRegion treeShakeAnimation = new TextureRegion(
                directory.getEntry("polar:tree_shake_animation", Texture.class));
        treeShakeFilm = new FilmStrip(treeShakeAnimation.getTexture(), 1, 16);

        TextureRegion cavePortalAnimation = new TextureRegion(
                directory.getEntry("polar:cave_portal_animation", Texture.class));
        cavePortalFilm = new FilmStrip(cavePortalAnimation.getTexture(), 2, 8);
        TextureRegion caveSleepAnimation = new TextureRegion(
                directory.getEntry("polar:cave_sleep_animation", Texture.class));
        caveZZZFilm = new FilmStrip(caveSleepAnimation.getTexture(), 1, 16);
        caveZZZFilm.setFrame(0);
        TextureRegion caveSleepLoopAnimation = new TextureRegion(
                directory.getEntry("polar:cave_sleep_loop_animation", Texture.class));
        caveZZZLoopFilm = new FilmStrip(caveSleepLoopAnimation.getTexture(), 2, 16);
        caveZZZLoopFilm.setFrame(0);

        TextureRegion smolUrsaIdleAnimation = new TextureRegion(
                directory.getEntry("smolursa:idle", Texture.class));
        smolUrsaIdleFilm = new FilmStrip(smolUrsaIdleAnimation.getTexture(), 3, 16);
        TextureRegion smolUrsaRescueAnimation = new TextureRegion(
                directory.getEntry("smolursa:rescue", Texture.class));
        smolUrsaRescueFilm = new FilmStrip(smolUrsaRescueAnimation.getTexture(), 5, 16);

        for (int i = 0; i < 3; i++) {
            TextureRegion sunAnimation = new TextureRegion(
                    directory.getEntry("ui:Sun" + i, Texture.class));
            sunAnimations[i] = new FilmStrip(sunAnimation.getTexture(), 5, 16);
            sunAnimations[i].setFrame(0);
        }
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
        decorationTextures[14] = new TextureRegion(directory.getEntry("decoration:ocean_1",Texture.class));
        decorationTextures[15] = new TextureRegion(directory.getEntry("decoration:ocean_4",Texture.class));
        decorationTextures[16] = new TextureRegion(directory.getEntry("decoration:ocean_8",Texture.class));
        decorationTextures[17] = new TextureRegion(directory.getEntry("decoration:ocean_2",Texture.class));
        decorationTextures[18] = new TextureRegion(directory.getEntry("decoration:ocean_3",Texture.class));
        decorationTextures[19] = new TextureRegion(directory.getEntry("decoration:ocean_5",Texture.class));
        decorationTextures[20] = new TextureRegion(directory.getEntry("decoration:ocean_6",Texture.class));
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
        Vector2 gravity = new Vector2(0,0 );
        blackTextureAlpha = -2f;
        for(Obstacle obj : objects) {
            obj.deactivatePhysics(world);
        }
        effect.reset();
        startWin = false;
        startLose = false;
        // Dispose and clear and references to objects
        objects.clear();
        addQueue.clear();
        dynamicObjects.clear();
        groundDecorations.clear();
        interactableCaves.clear();
        interactableTrees.clear();
        shadowController.reset();
        oceanDecorations.clear();
        world.dispose();
        reverse = false;
        // Rewind all film strips to the beginning
        cavePortalFilm.setFrame(0);
        caveZZZFilm.setFrame(0);
        playerWalkFilm.setFrame(0);
        playerIdleFilm.setFrame(0);
        playerRescueFilm.setFrame(0);
        treeShakeFilm.setFrame(0);
        smolUrsaIdleFilm.setFrame(0);
        smolUrsaRescueFilm.setFrame(0);

        colorNextPointer = 1;
        currentFrame = 0;
        shakingTree = null;
        isTimeSkipping = false;
        currCaveRotation = 0;
        interactedCave = null;
        player_dive_anim = 0;
        hasWon = false;
        barYOffset = 0;
        caughtBarXOffset = 0;
        ursaCaughtXOffset = 0;
        beganStartLoseFrame = 0;
        paused = false;
        for (int i = 0; i < 3; i++) {
            sunAnimations[i].setFrame(0);
        }

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
        levelMusicNight.setVolume(0);
        levelMusicTense.setVolume(0);
        paused = false;
        populateLevel();
    }


    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        // False for static shadows, true for dynamic
        boolean doShadowsMove = false;
        shadowController = new ShadowController(blackTexture, doShadowsMove);

        findTileIndices();
        renderShadows();
        renderUrsa();
        renderWalls();
        renderEnemies();
        renderTrees();
        renderCaves();
        renderSmolUrsa();
        renderGameObjects();
        renderIce();
        renderDecorations();
    }


    /**
     * Returns whether to process the update loop
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode.  If not, the update proceeds
     * normally.
     *
     * @param dt	Number of seconds since last animation frame
     *
     * @return whether to process the update loop
     */
    public boolean preUpdate(float dt) {


            effect.update(dt);
            if (!super.preUpdate(dt)) {
                return false;
            }

            if (!isFailure() && ursa.getY() < -1) {
                setFailure(true);
                return false;
            }

            return true;

        }


    private void animateEnemies(){
        for (AIController i : controls) {
            if (i != null) {
                if (i.isWon()) {
                    salmonDiveFilm.setFrame(player_dive_anim);
                    i.getEnemy().setTexture(salmonDiveFilm);
                    player_dive_anim = Math.min(player_dive_anim + 1, ENEMY_DIVE_FRAMES);
                    player_caught = true;
                } else if (i.isSurprised()) {

                    salmonDetectedFilm.setFrame(salmonDetectedIndex);
                    i.getEnemy().setTexture(salmonDetectedFilm);
                    if (currentFrame % 2 == 0) {
                        salmonDetectedIndex = (salmonDetectedIndex + 1) % 30;
                    }
                } else if (i.isConfused() || i.isStunned() || i.earlyLooking()) {
                    salmonConfusedFilm.setFrame(i.get_confused_anim_index());
                    i.getEnemy().setTexture(salmonConfusedFilm);
                    i.inc_anim_index();
                } else if (i.getEnemy().getVX() == 0 && i.getEnemy().getVY() == 0) {
                    salmonIdleFilm.setFrame(i.get_idle_anim_index());
                    i.getEnemy().setTexture(salmonIdleFilm);
//                    if (currentFrame % 2 == 0) {
//                        salmonIdleAnimIndex = (salmonIdleAnimIndex + 1) % 46;
//                    }
                    i.inc_idle_anim_index();
                } else {
                    i.reset_anim_index();
                    salmonDetectedIndex = 0;
                    i.reset_dive_anim_index();
                    i.reset_idle_anim_index();
                    salmonUprightWalkFilm.setFrame(salmonWalkAnimIndex);
                    i.getEnemy().setTexture(salmonUprightWalkFilm);
                }
            }
        }

        if (currentFrame % 2 == 0) {
            salmonWalkAnimIndex = (salmonWalkAnimIndex + 1) % 24;
        }
    }

    /**
     * Animates the player.
     * If the player is moving, uses the walking animation and if not walking, plays idling animation
     * Uses ursaBeganWalkingFrame and ursaBeganIdlingFrame to reset animation speeds
     * If time is skipping, we should also animating Ursa as walking since she's only drawn when walking to cave
     */
    private void animatePlayerModel(){
        // If the player is moving
        if (player_caught && player_dive_anim - DIVE_ANIM_DIFF >= 0) {

            playerCaughtFilm.setFrame(Math.min(9, player_dive_anim - DIVE_ANIM_DIFF));
            ursa.setTexture(playerCaughtFilm);
        } else if(ursa.getXMovement() != 0 || ursa.getYMovement() != 0 || isTimeSkipping){
            // If the player was idling, now changing states
            if(!ursaCurrentState) {
                ursaCurrentState = true;
                ursaBeganWalkingFrame = currentFrame;
                playerIdleFilm.setFrame(0);
            }

            if((currentFrame - ursaBeganWalkingFrame) % 2 == 0) {
                if(playerWalkFilm.getFrame() == 23) {
                    playerWalkFilm.setFrame(0);
                }
                playerWalkFilm.setFrame(playerWalkFilm.getFrame() + 1);
            }
            ursa.setTexture(playerWalkFilm);
        // If the player is not moving
        } else {
            // If the player changed states
            if(ursaCurrentState) {
                ursaCurrentState = false;
                ursaBeganIdlingFrame = currentFrame;
                playerWalkFilm.setFrame(0);
            }
            if((currentFrame - ursaBeganIdlingFrame) % 2 == 0) {
                if(playerIdleFilm.getFrame() == 45){
                    playerIdleFilm.setFrame(0);
                }
                playerIdleFilm.setFrame(playerIdleFilm.getFrame() + 1);
            }
            ursa.setTexture(playerIdleFilm);
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

        if((currentFrame - beganShakingTreeFrame) % 2 == 0) {
            if(treeShakeFilm.getFrame() == 15){
                treeShakeFilm.setFrame(0);
                shakingTree.setTexture(treeTextures[1]);
                shakingTree = null;
                return;
            }
            treeShakeFilm.setFrame(treeShakeFilm.getFrame() + 1);
        }
        shakingTree.setTexture(treeShakeFilm);
    }

    /**
     * Animates the portal of all interactable caves.
     * Once they become non-interactable, they should not animate.
     */
    private void animateCaves() {
        if(currentFrame % 2 == 0) {
            cavePortalFilm.setFrame(cavePortalFilm.getFrame() + 1);
            if(cavePortalFilm.getFrame() == 15){
                cavePortalFilm.setFrame(0);
            }
            caveZZZLoopFilm.setFrame(caveZZZLoopFilm.getFrame() + 1);
            if(caveZZZLoopFilm.getFrame() == 31){
                caveZZZLoopFilm.setFrame(0);
            }
        }

        for(Cave cave: interactableCaves) {
            cave.setZZZTexture(caveZZZLoopFilm);
            cave.setPortalTexture(cavePortalFilm);
        }
    }

    /**
     * Animates smol ursa to idle.
     */
    private void animateSmolUrsa() {
        if(currentFrame % 2 == 0) {
            smolUrsaIdleFilm.setFrame(smolUrsaIdleFilm.getFrame() + 1);
            if(smolUrsaIdleFilm.getFrame() == 47){
                smolUrsaIdleFilm.setFrame(0);
            }
        }
        goal.setTexture(smolUrsaIdleFilm);
    }


    /**
     * The core gameplay loop of this world.
     * This method contains the specific update code for this mini-game. It does
     * not handle collisions, as those are managed by the parent class WorldController.
     * This method is called after input is read, but before collisions are resolved.
     * The very last thing that it should do is apply forces to the appropriate objects.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void update(float dt) {
        //System.out.println("FPS: " + (1/dt));
        // Increment the current frame (used for animation slow downs)
        timer += 1;
        newTimer +=1;
        currentFrame++;
        if(Gdx.input.isKeyPressed(Keys.NUM_0)){
            doGriddy = true;
        }
        if((Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) && paused){
            if(timer >= 30){
                paused = false;
                doGriddy = false;
                timer = 0;
            }

        }
        if((Gdx.input.isKeyPressed(Input.Keys.P)) || (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) && !paused && !startLose && !startWin){
            if(timer >= 30){
                paused = true;
                setPaused(true);
                timer = 0;

                doGriddy = false;
            }
        }

        if(paused){


            for (Enemy enemy : enemies) {
                if (enemy != null) {
                    enemy.setVX(0);
                    enemy.setVY(0);
                }
            }
            ursa.setVX(0);
            ursa.setVY(0);
            return;
        }

        // If we've won, perform the rescue animations
        if (hasWon) {
            ursa.setVX(0);
            ursa.setVY(0);
            // Animate Ursa's rescue and then
            if (playerRescueFilm.getFrame() < 50) {
                if (currentFrame % 2 == 0) {
                    playerRescueFilm.setFrame(playerRescueFilm.getFrame() + 1);
                }
                ursa.setTexture(playerRescueFilm);
                ursaBeganIdlingFrame = currentFrame;
            }

            if (smolUrsaRescueFilm.getFrame() < 73) {
                if (currentFrame % 2 == 0) {
                    smolUrsaRescueFilm.setFrame(smolUrsaRescueFilm.getFrame() + 1);
                }
                goal.setTexture(smolUrsaRescueFilm);
                return;
            } else {
                setComplete(true);
                return;
            }
        }

        // Update the timeRatio then UI rotation + shadow tinting correspondingly
        timeRatio = shadowController.getTimeRatio();
        // If it's night, reset the tinting
        if (timeRatio > 0.5) {
            colorNextPointer = 1;
            if (!isComplete()) {
                levelMusicNight.setVolume(Math.max(0, levelMusicNight.getVolume() + 0.01f));
            }
        } else {
            levelMusicNight.setVolume(Math.max(0, levelMusicNight.getVolume() - 0.01f));
            // Update colorNextPointer to next interval
            if (timeRatio > intervals[colorNextPointer]) {
                colorNextPointer++;
            }
            updateBackgroundColor(intervals[colorNextPointer - 1], timeRatio);
        }
        shadowController.update(backgroundColor);

        // Center the camera around Ursa
        canvas.moveCam(ursa.getPosition().x, ursa.getPosition().y);
        effect.getEmitters().first().setPosition(canvas.getWidth() / 2, canvas.getHeight());
        effect.start();

        // Play the music if it is not
        if (!levelMusic.isPlaying()) {
            levelMusicNight.play();
            levelMusicTense.play();
            levelMusic.play();
            levelMusic.setLooping(true);
        }

        // Always animate the cave portals even if time is fast forwarding
        animateCaves();
        animatePlayerModel();
        // If the time is fast forwarding
        if (isTimeSkipping) {
            for (Enemy e : enemies) {
                if (e != null) {
                    e.setVY(0);
                    e.setVX(0);
                }
            }

            // Ursa Walks to the Cave
            if ((currentFrame - timeBeganSkippingFrame) < walkingDuration) {
                walkToPoint(interactedCave.getX(), interactedCave.getY());
                return;
            }
            ursa.stopDrawing();

            // Rotate the shadows over fastForwardDuration update loops
            int fastForwardDuration = transitionDuration + walkingDuration;

            if ((currentFrame - timeBeganSkippingFrame) % fastForwardDuration == 0) {
                isTimeSkipping = false;
                ursa.resumeDrawing();
                interactedCave.setPortalTexture(polarPortalTexture);
                caveZZZFilm.setFrame(0);
                interactableCaves.remove(interactedCave);
            } else {
                int framesIntoTransition = currentFrame - timeBeganSkippingFrame - walkingDuration;
                // Move the shadows
                shadowController.animateFastForward(
                        framesIntoTransition + 1,
                        fastForwardDuration - walkingDuration);
                // Animate bars
                if(framesIntoTransition < barRiseDuration) {
                    barYOffset = framesIntoTransition / (float) barRiseDuration * barHeight;
                } else if(transitionDuration - framesIntoTransition - 1 < barRiseDuration) {
                    barYOffset = (transitionDuration - framesIntoTransition - 1) / (float) barRiseDuration * barHeight;
                }
                // Animate the Day night UI
                if(framesIntoTransition % 2 == 0) {
                    sunAnimations[sunIndex].setFrame(sunAnimations[sunIndex].getFrame() + 1);

                    if(sunAnimations[sunIndex].getFrame() == 71) {
                        sunAnimations[sunIndex].setFrame(0);
                    }
                }
            }
            return;
        }

        // Move Ursa
        float xVal = InputController.getInstance().getHorizontal() * ursa.getForce();
        float yVal = InputController.getInstance().getVertical() * ursa.getForce();
        if(InputController.getInstance().getVertical() != 0 || InputController.getInstance().getHorizontal() !=0){
            doGriddy = false;
        }
        ursa.setMovement(xVal, yVal);

        checkForInteraction();

        // Animates the game objects
        animateEnemies();
        animateTree();
        animateSmolUrsa();

        boolean alerted = false;
        for (AIController c : controls) {
            c.getAction();
            Enemy thisEnemy = c.getEnemy();
            thisEnemy.setAlerted(thisEnemy.isPlayerInLineOfSight(world, ursa));
            if (!alerted && thisEnemy.isAlerted()) {
                alerted = true;
            }

            // check aggro nearby
            c.changeAggro(updateAggro(c));

            if (c.isWon() && player_dive_anim >= ENEMY_DIVE_FRAMES)
                if(!startLose) {
                   setFailure(true);

                }

        }

        for (Enemy enemy : enemies) {
            if (enemy != null) {

                if (enemy.isPlayerInLineOfSight(world, ursa)) {
                    enemy.getPlayerPos(ursa.getPosition());
                }
                enemy.setInShadow(ursa.isInShadow());
                enemy.createSightCone(world);
            }
        }

        if (alerted) {
            levelMusicTense.setVolume(Math.min(levelMusicTense.getVolume() + 0.01f, 1f));
        } else {
            levelMusicTense.setVolume(Math.max(levelMusicTense.getVolume() - 0.01f, 0f));
        }

        canvas.clear();

        // If the game is lost, stop the player
        if (!isFailure() && player_dive_anim < 16) {
            ursa.applyForce();
        } else {
            ursa.setVX(0);
            ursa.setVY(0);
        }

        if(doGriddy){

            ursa.setTexture(playerCaughtFilm);
            if(playerCaughtFilm.getFrame() <1 ){

                reverse = false;
                playerCaughtFilm.setFrame(1);
            }
            if(playerCaughtFilm.getFrame() > 7){
                playerCaughtFilm.setFrame(7);
                reverse = true;
            }

            if(playerCaughtFilm.getFrame() >=1 && playerCaughtFilm.getFrame()<=7 && !reverse && timer % 3 == 0){

                playerCaughtFilm.setFrame(playerCaughtFilm.getFrame()+1);
            }
            if(playerCaughtFilm.getFrame() >=1 && playerCaughtFilm.getFrame()<=7 && reverse && timer % 3 == 0){

                playerCaughtFilm.setFrame(playerCaughtFilm.getFrame()-1);
            }


        }

    }

    @Override
    public void hide() {
        super.hide();
        levelMusic.stop();
        levelMusicTense.stop();
        levelMusicNight.stop();
    }

    /**
     *  Find the closest interactable obstacle among caves and trees.
     *  Then, shake the tree or fast forward the time if applicable.
     */
    public void checkForInteraction() {
        float treeInteractionRange = 5;
        Tree closestInteractableTree = null;
        Cave closestInteractableCave = null;
        float minDistance = 1000;
        float tempDistance;
        for(Tree tree: interactableTrees) {
            tempDistance = ursa.getPosition().dst(tree.getPosition());
            if (tree.canShake() && tempDistance < treeInteractionRange && (closestInteractableTree == null || tempDistance < minDistance)) {
                closestInteractableTree = tree;
                minDistance = tempDistance;
            }
        }
        for(Cave cave: interactableCaves) {
            float caveInteractionRange = 3;
            tempDistance = ursa.getPosition().dst(cave.getPosition());
            if (cave.canInteract() && tempDistance < caveInteractionRange && (closestInteractableCave == null || tempDistance < minDistance)) {
                closestInteractableCave = cave;
                minDistance = tempDistance;
            }
        }

        // Take cave entering as a priority
        if(InputController.getInstance().didEnterCave()) {
            if(closestInteractableCave != null) {
                beginCaveFastForward(closestInteractableCave);
                return;
            }
        }
        if(InputController.getInstance().didInteract()) {
            if(closestInteractableTree != null) {
                shakeTree(closestInteractableTree);
                interactableTrees.remove(closestInteractableTree);
            }
        }
    }

    /**
     * Shakes tree if able.
     * Stuns nearby enemies and prevents tree from shaking again.
     * @param tree Tree attempting to be interacted with
     */
    private void shakeTree(Tree tree) {
        if (!tree.canShake()) {
            return;
        }
        tree.putOnShakeCooldown();
        shakingTree = tree;
        beganShakingTreeFrame = currentFrame;

        for (Enemy enemy : enemies) {
            float enemyStunDistance = 10;
            if (enemy != null && enemy.getPosition().dst(tree.getPosition()) < enemyStunDistance) {
                enemy.stun();
            }
        }
    }

    /**
     * fastForward(cave) fast forwards the time via ShadowController.
     * Then, the cave is set such that it cannot be interacted with again.
     * @param cave Cave being interacted with
     */
    public void beginCaveFastForward(Cave cave) {
        if(!cave.canInteract()) {
            return;
        }
        cave.interact();
        walkingDuration = (int) ((cave.getPosition().dst(ursa.getPosition()) * 10));
        if(currCaveRotation < caveRotations.length - 1) {
            shadowController.forwardTimeRatio(caveRotations[currCaveRotation + 1] - caveRotations[currCaveRotation]);
            currCaveRotation++;
        } else {
            shadowController.forwardTimeRatio(180);
        }
        isTimeSkipping = true;
        timeBeganSkippingFrame = currentFrame;
        interactedCave = cave;
        interactedCave.setZZZTexture(caveZZZFilm);
        sunIndex = (int) (Math.random() * 3);
        // Record where Ursa started to smoothly walk her to cave
        ursaStartingPosition = new Vector2(ursa.getPosition());
        ursa.setIsFacingRight(cave.getX() > ursa.getX());
    }

    /**
     * Walks Ursa to the cave she interacted with.
     */
    private void walkToPoint(float pointX, float pointY) {
        ursa.setVX(0f);
        ursa.setVY(0f);
        // Move Ursa towards the cave over walkingDuration
        float newX = ursaStartingPosition.x + (pointX - ursaStartingPosition.x) * (currentFrame - timeBeganSkippingFrame) / walkingDuration;
        float newY = ursaStartingPosition.y + (pointY - ursaStartingPosition.y) * (currentFrame - timeBeganSkippingFrame) / walkingDuration;
        ursa.setX(newX);
        ursa.setY(newY);
    }

    /**
     * Callback method for the start of a collision
     * This method is called when we first get a collision between two objects.
     * We use this method to see if Ursa is in shadow.
     * Secondarily, it's used to see if Ursa reaches the goal.
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

            if ((ursa.getSensorName().equals(fd2) && bd1.getName().equals("shadow")) ||
                    (ursa.getSensorName().equals(fd1) && bd2.getName().equals("shadow"))) {
                ursa.setInShadow(true);
                sensorFixtures.add(ursa == bd1 ? fix2 : fix1);
            }

            // Check for win condition
            if ((bd1 == ursa && bd2 == goal) || (bd1 == goal && bd2 == ursa)) {
                ursa.setIsFacingRight(goal.getX() - ursa.getX() > 0);
                hasWon = true;
                levelMusic.stop();
                levelMusicTense.stop();
                levelMusicNight.stop();
            }
            if(((bd1.getName().contains("enemy")) && bd2.getName().contains("ice")) || ((bd1.getName().contains("ice")) && bd2.getName().contains("enemy"))){
                bd1.setLinearDamping(10000);
                bd2.setLinearDamping(10000);
                bd2.setVX(0);
                bd2.setVY(0);
                bd1.setVY(0);
                bd1.setVX(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * Callback method for the start of a collision
     * This method is called when two objects cease to touch.
     * The main use of this method is to determine when Ursa exits a shadow.
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
        if(bd1.getLinearDamping() > 100){
            bd1.setLinearDamping(0);
        }
        if(bd2.getLinearDamping() > 100){
            bd1.setLinearDamping(0);
        }

        if ((ursa.getSensorName().equals(fd2) && bd1.getName().equals("shadow")) ||
                (ursa.getSensorName().equals(fd1) && bd2.getName().equals("shadow"))) {
            sensorFixtures.remove(ursa == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                ursa.setInShadow(false);
            }
        }
    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {}

    /**
     * Invariant: intervals.length == colors.length
     * Transitions smoothly between colors[reachPointer] and colors[reachPointer-1] in the time
     * between intervals[reachPointer] and intervals[reachPointer-1]
     * @param timeRatio the time of day where 0 is night -> day, 1 is day -> night
     */
    private void updateBackgroundColor(float startTime, float timeRatio) {
        backgroundColor.r = colors[colorNextPointer - 1].r + (colors[colorNextPointer].r - colors[colorNextPointer - 1].r) * (timeRatio - startTime) / (intervals[colorNextPointer] - intervals[colorNextPointer - 1]);
        backgroundColor.g = colors[colorNextPointer - 1].g + (colors[colorNextPointer].g - colors[colorNextPointer - 1].g) * (timeRatio - startTime) / (intervals[colorNextPointer] - intervals[colorNextPointer - 1]);
        backgroundColor.b = colors[colorNextPointer - 1].b + (colors[colorNextPointer].b - colors[colorNextPointer - 1].b) * (timeRatio - startTime) / (intervals[colorNextPointer] - intervals[colorNextPointer - 1]);
        backgroundColor.a = colors[colorNextPointer - 1].a + (colors[colorNextPointer].a - colors[colorNextPointer - 1].a) * (timeRatio - startTime) / (intervals[colorNextPointer] - intervals[colorNextPointer - 1]);
    }

    /**
     * predraw(dt) draws all the background elements of the game
     * This includes the background (obviously), decorations, tiles, and shadows
     * It also includes tinting for now, but this will change later on with shaders
     * @param dt	Number of seconds since last animation frame
     */
    @Override
    public void preDraw(float dt) {
        // Draw an ocean bordering
        canvas.draw(tileTextures[0], Color.WHITE,canvas.getCameraX() - canvas.getWidth() / 2f, canvas.getCameraY() - canvas.getHeight() / 2f, canvas.getWidth(), canvas.getHeight());

        // Draw snow on the map
        canvas.draw(groundTexture, Color.WHITE, 0, 0, numTilesX * 16 * textureScale * scale.x, numTilesY * 16 * textureScale * scale.y);

        for(Decoration d: groundDecorations) {
            d.draw(canvas);
        }


        for(Decoration d: decorations) {
            d.draw(canvas);
        }
        // Draws shadows for moving objects (enemy/player)
        for(Obstacle obj: dynamicObjects) {
            obj.preDraw(canvas);
        }
        // Draw shadow for static objects (trees, etc)
        shadowController.drawShadows(canvas);


        drawTiles();

        for(Decoration d: oceanDecorations) {
            d.draw(canvas);
        }

        // Draw a tinting over everything
        canvas.draw(whiteTexture,backgroundColor, canvas.getCameraX() - canvas.getWidth() / 2f, canvas.getCameraY() - canvas.getHeight() / 2f, canvas.getWidth(), canvas.getHeight());
        super.updateTinting(backgroundColor);

//        fb.begin();
//        Gdx.gl.glClearColor(1, 1, 1, 1);
//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        // Draws shadows for static objects (trees, rocks, trunks, etc)
//        canvas.draw(salmonTexture,Color.WHITE,128,128,400f,400f);
//        fb.end();
    }

    @Override
    public void draw(float dt) {
        super.draw(dt);
    }

    /**
     * Draws all UI elements over the game objects
     * @param dt	Number of seconds since last animation frame
     */
    public void postDraw(float dt) {
        super.postDraw(dt);

        // Draw the ZZZ UI
        for(Cave cave: interactableCaves) {
            cave.postDraw(canvas);
        }
        for(Decoration d: decorations) {
            d.postDraw(canvas);
        }

        effect.draw(canvas.getSpriteBatch());
        float retryUIScale = 1.6f;
        if(startLose){
            if(blackTextureAlpha < 1 && blackTextureAlpha >= 0){
                blackTextureAlpha += .1f;
            }
            else if(blackTextureAlpha<0){
                blackTextureAlpha = 0;
            }
            Color color = new Color(255,255,255,blackTextureAlpha);
            canvas.draw(blackTexture,color, canvas.getCameraX() - canvas.getWidth() /2f, canvas.getCameraY() - canvas.getHeight() /2f, canvas.getWidth(), canvas.getHeight());
            canvas.draw(retryTextures[0],Color.WHITE, 0,retryTextures[0].getRegionHeight() / 2f,canvas.getCameraX() - canvas.getWidth()/2 -925 + caughtBarXOffset, canvas.getCameraY(),0,textureScale* retryUIScale,textureScale*retryUIScale );
            canvas.draw(retryTextures[1],Color.WHITE,retryTextures[1].getRegionWidth(),retryTextures[1].getRegionHeight() /2f,canvas.getCameraX() + canvas.getWidth() + 70f  - ursaCaughtXOffset, canvas.getCameraY(),0,retryUIScale * textureScale,retryUIScale * textureScale);
            if(blackTextureAlpha >= 1){
                setFailure(true);
            }
        }

        drawGoalIndicator();


        if(paused && !startWin && !startLose){
            Color color = new Color(255,255,255,.5f);
            canvas.draw(blackTexture,color, canvas.getCameraX() - canvas.getWidth() / 2f, canvas.getCameraY() - canvas.getHeight() / 2f, canvas.getWidth() ,canvas.getHeight());
            canvas.draw(pauseScreen[0],Color.WHITE,0,0,canvas.getCameraX() - canvas.getWidth()/2.5f , canvas.getCameraY() - canvas.getHeight() /5.5f,0,textureScale,textureScale );
            canvas.draw(pauseScreen[1],Color.WHITE,0,0,canvas.getCameraX() - canvas.getWidth() /3.2f, canvas.getCameraY() - canvas.getHeight() /7f,0,textureScale,textureScale );
            canvas.draw(pauseScreen[2],Color.WHITE,0,0,canvas.getCameraX() - canvas.getWidth() /11f, canvas.getCameraY() - canvas.getHeight() /7f,0,textureScale,textureScale );
        }

        // Animates cave skip transition
        barHeight = canvas.getHeight() / 8f;
        canvas.draw(blackTexture, Color.WHITE, canvas.getCameraX() - canvas.getWidth() / 2f, canvas.getCameraY() + canvas.getHeight() /2f - barYOffset, canvas.getWidth(), barHeight);
        canvas.draw(blackTexture, Color.WHITE, canvas.getCameraX() - canvas.getWidth() / 2f, canvas.getCameraY() - canvas.getHeight() /2f - barHeight + barYOffset, canvas.getWidth(), barHeight);

        canvas.draw(sunAnimations[sunIndex], Color.WHITE, sunAnimations[sunIndex].getRegionWidth() / 2f,0, canvas.getCameraX() - canvas.getWidth() / 4f,
                canvas.getCameraY() - canvas.getHeight() / 2f,0,textureScale, textureScale);
    }


    /**
     * @param drawCoord A coordinate in terms of the drawing coordinates
     * @return drawCoord in terms of the screen coordinates
     */
    public float drawToScreenCoordinates(float drawCoord) {
        // First, convert between 2560 x 1440 resolution assets were drawn for and 1920 x 1080 resolution we have
        // Then, convert to screen coordinates using the scale
        return drawCoord * textureScale / scale.x;
    }

    public float screenToDrawCoordinates(float screenCoord) {
        return screenCoord * scale.x / textureScale;
    }

    private void drawGoalIndicator() {
        // If small ursa on screen don't do anything

        if(canvas.inView(new Vector2(screenToDrawCoordinates(goal.getPosition().x) * textureScale, screenToDrawCoordinates(goal.getPosition().y) * textureScale))) {
            return;
        }
        float deltaY = screenToDrawCoordinates(goal.getPosition().y) * textureScale - canvas.getCameraY();
        float deltaX = screenToDrawCoordinates(goal.getPosition().x) * textureScale - canvas.getCameraX();
        float angle = (float) Math.atan2(deltaY, deltaX);
        float slope = deltaY / deltaX;
        float intercept = canvas.getCameraY() - slope * canvas.getCameraX();
        if(angle < 0) {
            angle += 2 * Math.PI;
        }

        float indicatorX;
        float indicatorY;
        float topRight = (float) Math.atan(canvas.getHeight() / (float) canvas.getWidth());
        float topLeft = (float) (Math.PI - Math.atan(canvas.getHeight()/(float) canvas.getWidth()));
        float bottomLeft = (float) (Math.PI + Math.atan(canvas.getHeight()/(float) canvas.getWidth()));
        float bottomRight = (float) (2 * Math.PI - Math.atan(canvas.getHeight()/(float) canvas.getWidth()));

        if(angle > topRight && angle < topLeft) {
            indicatorY = canvas.getCameraY() + canvas.getHeight() / 2f;
            indicatorX = (indicatorY - intercept) / slope;
        } else if(angle >= topLeft && angle < bottomLeft) {
            indicatorX = canvas.getCameraX() - canvas.getWidth() / 2f;
            indicatorY = slope * indicatorX + intercept;
        } else if(angle >= bottomLeft && angle < bottomRight) {
            indicatorY = canvas.getCameraY() - canvas.getHeight() / 2f;
            indicatorX = (indicatorY - intercept) / slope;
        } else {
            indicatorX = canvas.getCameraX() + canvas.getWidth() / 2f;
            indicatorY = slope * indicatorX + intercept;
        }

        canvas.draw(arrowIndicator, Color.WHITE, arrowIndicator.getRegionWidth(), arrowIndicator.getRegionHeight() /2f, indicatorX, indicatorY, angle, textureScale, textureScale);
    }




    /**
     Draws all tiles based on the tile textures parsed.
     */
    private void drawTiles(){
        float x;
        float y;
        // The array needs to be parsed from top to bottom

        for(int i = 0; i < numTilesY; i++) {
            for(int j = 0; j < numTilesX; j++) {
                int tileIndex = tiles[j][i];
                if(tileIndex == 0) {
                    continue;
                }
                x = j * (16f * textureScale) * scale.x;
                y = i * (16f * textureScale) * scale.y;
                canvas.draw(tileTextures[tileIndex - firstTileIndex], Color.WHITE,0,0, x, y, ursa.getAngle(), textureScale, textureScale);
            }
        }
    }

    /*
     * RENDERING Methods that parse a level JSON into a world
     * Layers Invariant: player/markers/enemies/smolursa/ice/objects/trees/cave/decorations
     */

    /**
     * Finds indices of tile textures that were loaded into levels.
     * These are hard coded to what images are in the tilsets so if you change them, let William know :3
     * There's similar hard coding in renderDecorations() and renderGameObjects()
     */
    private void findTileIndices() {
        JsonValue tilesetData = jsonData.get("tilesets");
        for(int i = 0; i < tilesetData.size; i++) {
            int id = tilesetData.get(i).get("firstgid").asInt();
            String source = tilesetData.get(i).get("source").asString();
            switch (source) {
                case "maps/tiles.tsx":
                    firstTileIndex = id;
                    break;
                case "maps/256x512.tsx":
                    firstTreeIndex = id;
                    firstMediumRockIndex = id + 2;
                    break;
                case "maps/64x64.tsx":
                    firstSmallDecorationIndex = id;
                    break;
                case "maps/512x512.tsx":
                    firstHouseIndex = id + 1;
                    firstLargeDecorationIndex = id + 2;
                    break;
                case "maps/256x256(Ursa + Salmon).tsx":
                    firstMediumObjectIndex = id + 3;
                    break;
                case "maps/Polar_Rock_2.tsx":
                    polarRock2Index = id;
                    break;
                case "maps/Polar_Trunk_1.tsx":
                    polarTrunk1Index = id;
                    break;
                case "maps/Polar_Trunk_2.tsx":
                    polarTrunk2Index = id;
                    break;
                case "maps/1024x512.tsx":
                    firstLargeOceanDecorationIndex = id;
            }
        }
    }

    /**
     * Parses the shadow starting rotations or the degrees caves rotate the shadows by
     */
    private void renderShadows() {
        JsonValue tileProperties = jsonData.get("layers").get(0).get("properties");
        if (tileProperties != null){
            for(JsonValue property: tileProperties) {
                switch (property.get("name").asString()) {
                    case "cave_rotations":
                        String[] split = property.get("value").asString().split(",");
                        caveRotations = new float[split.length];

                        for (int j = 0; j < split.length; j++) {
                            if(j == 0) { shadowStartingRotation = Float.parseFloat(split[j]); }
                            caveRotations[j] = Float.parseFloat(split[j]);
                        }
                        break;
                    case "starting_rotation":
                        shadowStartingRotation = property.get("value").asFloat();
                        break;
                }
            }
        }
    }

    /**
     * Creates invisible walls depending on the tile types
     */
    private void renderWalls(){
        int counter = 0;
        float x;
        float y;
        JsonValue wallConstants = constants.get("wall");
        GameObject wall;
        tiles = new int[(int) numTilesX][(int) numTilesY];
        // The array needs to be parsed from top to bottom
        for (int i = (int) numTilesY - 1; i >= 0 ; i--) {
            for(int j = 0; j < numTilesX; j++){
                int tileIndex = jsonData.get("layers").get(0).get("data").get(counter++).asInt();
                tiles[j][i] = tileIndex;
                if(tileIndex == 0 || tileIndex == firstTileIndex) {
                    continue;
                }
                x = j * (16f * textureScale);
                y = i * (16f * textureScale);
                float[] points = wallConstants.get(Integer.toString(tileIndex - firstTileIndex)).asFloatArray();
                for(int k = 0; k < points.length; k++) {
                    points[k] *= textureScale / 0.75f;
                }
                wall = new GameObject(points,x, y, 0, textureScale);
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

    /**
     * Renders the player into the world by parsing JSON
     */
    private void renderUrsa() {
        float playerX = jsonData.get("layers").get(9).get("objects").get(0).get(8).asFloat();
        float playerY = maxY - jsonData.get("layers").get(9).get("objects").get(0).get(9).asFloat();

        JsonValue ursaConstants = constants.get("ursa");
        float playerWidth = ursaConstants.get("width").asFloat() * textureScale;
        playerHeight = ursaConstants.get("height").asFloat() * textureScale;

        ursa = new UrsaModel(drawToScreenCoordinates(playerX + ursaTexture.getRegionWidth() / 2f), drawToScreenCoordinates(playerY) + playerHeight / 2,
                ursaConstants, playerWidth, playerHeight, textureScale);
        ursa.setDrawScale(scale);
        ursa.setTexture(playerWalkFilm);
        ursa.setShadowTexture(ursaShadowTexture);
        addObject(ursa);
        dynamicObjects.add(ursa);
    }

    /**
     * Renders the enemies and their corresponding patrol markers by parsing JSON
     */
    private void renderEnemies() {
        if (jsonData.get("layers").get(7) == null) {
            return;
        }
        JsonValue enemyConstants = constants.get("enemy");
        JsonValue enemyObjectData = jsonData.get("layers").get(7).get("objects");

        enemies = new Enemy[enemyObjectData.size];
        for (int i = 0; i < enemyObjectData.size; i++) {
            float x = enemyObjectData.get(i).get("x").asFloat() + salmonTexture.getRegionWidth() / 2f;
            float y = maxY - enemyObjectData.get(i).get("y").asFloat();
            String enemyName = enemyObjectData.get(i).get("name").asString();

            float width = enemyConstants.get("width").asFloat();
            float height = enemyConstants.get("height").asFloat();

            boolean is_stupid = false;
            int starting_rotation = 0;
            float speed = 8f;
            try {
                JsonValue propertyData = enemyObjectData.get(i).get("properties");
                for(int j = 0; j < propertyData.size; j++) {
                    JsonValue property = propertyData.get(j);
                    String name = property.get("name").asString();
                    switch (name) {
                        case "is_stupid":
                            is_stupid = property.get("value").asBoolean();
                            break;

                        case "starting_rotation":
                            starting_rotation = property.get("value").asInt();
                            break;

                        case "speed":
                            speed = property.get("value").asFloat();
                            break;
                    }
                }
            } catch (NullPointerException ignored) { }

            Enemy enemy = new Enemy(drawToScreenCoordinates(x), drawToScreenCoordinates(y) + height / 2,20,20,constants.get("enemy"), width, height, textureScale);
            enemy.setDrawScale(scale);
            enemy.setBodyType(BodyDef.BodyType.DynamicBody);
            enemy.setDensity(.0f);
            enemy.setMass(.01f);

            enemy.setLookDirection(1, 0);
            enemy.setTexture(salmonUprightWalkFilm);
            enemy.setShadowTexture(salmonShadowTexture);
            enemy.setName("enemy" + i);
            enemy.setSpeed(speed);

            addObject(enemy);
            dynamicObjects.add(enemy);
            enemies[i] = enemy;

            if(jsonData.get("layers").get(8) == null && !is_stupid) {
                System.out.println("Please put down some markers.");
                return;
            }

            // Parse the markers
            JsonValue markerObjectData = jsonData.get("layers").get(8).get("objects");
            EnemyMarker[] enemyPosList = new EnemyMarker[markerObjectData.size];
            for(int e = 0; e < enemyPosList.length; e++) {
                String markerName = markerObjectData.get(e).get("name").asString();
                if (markerName.equals(enemyName)) {
                    int orderNum = (jsonData.get("layers").get(8).get("objects").get(e).get("type")
                            .asInt());
                    float markerX = markerObjectData.get(e).get("x").asFloat();
                    float markerY = maxY - markerObjectData.get(e).get("y").asFloat();

                    EnemyMarker marker = new EnemyMarker(new Vector2(drawToScreenCoordinates(markerX), drawToScreenCoordinates(markerY)),
                        markerObjectData.get(e).get("properties"));
                    enemyPosList[orderNum - 1] = marker;
                }
            }

            Board board = new Board(genericObstacles, enemies);
            controls.add(new AIController(enemy, ursa, null, enemyPosList, is_stupid, starting_rotation));
        }
    }

    /**
     * Renders smol ursa into the map by parsing JSON
     */
    private void renderSmolUrsa() {
        if (jsonData.get("layers").get(6) == null) {
            return;
        }
        JsonValue smolUrsaConstants = constants.get("smolursa");
        JsonValue smolUrsaObjectData = jsonData.get("layers").get(6).get("objects");

        float x = smolUrsaObjectData.get(0).get(8).asFloat() + smolUrsaTexture.getRegionWidth() / 2f;
        float y = maxY - smolUrsaObjectData.get(0).get(9).asFloat();

        goal = new GameObject(getVertices(smolUrsaConstants), drawToScreenCoordinates(x),
                drawToScreenCoordinates(y), 0, textureScale);
        goal.setDoesTint(false);
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
     * Renders all the ice into the map by parsing JSON
     */
    private void renderIce() {
        if (jsonData.get("layers").get(5) == null) {
            return;
        }
        JsonValue iceConstants = constants.get("ice");
        JsonValue iceObjectData = jsonData.get("layers").get(5).get("objects");

        for (int i = 0; i < iceObjectData.size; i++) {
            float yOffset = iceConstants.get("yOffset").asFloat();
            float x = iceObjectData.get(i).get(8).asFloat() + polarIceTexture.getRegionWidth() / 2f;
            float y = maxY - iceObjectData.get(i).get(9).asFloat() + yOffset;

            Moveable ice = new Moveable(getVertices(iceConstants), drawToScreenCoordinates(x),drawToScreenCoordinates(y), yOffset, textureScale);
            ice.setDrawScale(scale);
            ice.setTexture(polarIceTexture);
            ice.setName("ice" + i);
            addObject(ice);
        }
    }

    /**
     * Renders all the game objects into the map by parsing JSON
     * If you change the sprite sheets, you need to adjust these numbers (ask William)
     */
    private void renderGameObjects() {
        if(jsonData.get("layers").get(4) == null) { return; }

        JsonValue objectData = jsonData.get("layers").get(4).get("objects");

        String name;
        for(int i = 0; i < objectData.size; i++) {
            int objectIndex = objectData.get(i).get("gid").asInt();
            int textureIndex;

            if(objectIndex - firstMediumObjectIndex == 0) {
                textureIndex = 0;
                name = "rock_1";
            } else if(objectIndex - firstMediumObjectIndex == 1) {
                textureIndex = 1;
                 name = "statue";
            } else if(objectIndex - firstMediumObjectIndex == 2) {
                textureIndex = 2;
                name = "goat";
            } else if(objectIndex - firstMediumRockIndex == 0) {
                textureIndex = 3;
                name = "rock_4";
            }  else if(objectIndex - firstMediumRockIndex == 1) {
                textureIndex = 4;
                name = "rock_3";
            } else if(objectIndex - firstHouseIndex == 0) {
                textureIndex = 5;
                name = "house";
            } else if(objectIndex == polarRock2Index) {
                textureIndex = 6;
                name = "rock_2";
            } else if(objectIndex == polarTrunk1Index) {
                textureIndex = 7;
                name = "trunk_1";
            } else if(objectIndex == polarTrunk2Index) {
                textureIndex = 8;
                name = "trunk_2";
            } else {
                System.out.println("Unidentified object (UFO).");
                continue;
            }

            JsonValue objectConstants = constants.get(name);
            float yOffset = objectConstants.get("yOffset").asFloat();
            float x = objectData.get(i).get("x").asFloat() + objectTextures[textureIndex].getRegionWidth() / 2f;
            float y = maxY - objectData.get(i).get("y").asFloat() + yOffset;

            // House and Rock 3 have special drawing order
            GameObject obj;
            if(name.equals("house") || name.equals("rock_3")) {
                obj = new CustomGameObject(getVertices(objectConstants),drawToScreenCoordinates(x),drawToScreenCoordinates(y), yOffset, textureScale, playerHeight);
            } else {
                obj = new GameObject(getVertices(objectConstants),drawToScreenCoordinates(x),drawToScreenCoordinates(y), yOffset, textureScale);
            }
            obj.setDrawScale(scale);
            obj.setTexture(objectTextures[textureIndex]);
            obj.setName("game object" + i);
            addObject(obj);
            makeShadow(objectConstants,obj);

            // ===================
            genericObstacles.add(new GenericObstacle(obj.getX(), obj.getY(),
                    obj.getWidth(), obj.getHeight()));
            // ===================
        }
    }

    /**
     * Renders all the trees into the map by parsing JSON
     */
    private void renderTrees() {
        if(jsonData.get("layers").get(3) == null) { return; }

        JsonValue treeConstants = constants.get("tree");
        JsonValue treeObjectData = jsonData.get("layers").get(3).get("objects");

        for(int i = 0; i < treeObjectData.size; i++) {
            int treeIndex = treeObjectData.get(i).get("gid").asInt();
            float yOffset = treeConstants.get("yOffset").asFloat();
            float x = treeObjectData.get(i).get(8).asFloat() + treeTextures[treeIndex - firstTreeIndex].getRegionWidth() / 2f;
            float y = maxY - treeObjectData.get(i).get(9).asFloat() + yOffset;

            Tree tree = new Tree(getVertices(treeConstants),drawToScreenCoordinates(x),drawToScreenCoordinates(y), yOffset, textureScale);
            tree.setDrawScale(scale);
            tree.setTexture(treeTextures[treeIndex - firstTreeIndex]);
            tree.setName("tree" + i);
            // If the tree has no snow
            if(treeIndex - firstTreeIndex == 1) {
                tree.putOnShakeCooldown();
            } else {
                interactableTrees.add(tree);
            }

            addObject(tree);

            makeShadow(treeConstants,tree);

            // ===================
            genericObstacles.add(new GenericObstacle(tree.getX(), tree.getY(),
                    tree.getWidth(), tree.getHeight()));
            // ===================
        }
    }

    /**
     * Renders all the caves into the map by parsing JSON
     */
    private void renderCaves() {
        if (jsonData.get("layers").get(2) == null) {
            return;
        }
        JsonValue caveConstants = constants.get("cave");
        JsonValue caveObjectData = jsonData.get("layers").get(2).get("objects");

        for (int i = 0; i < caveObjectData.size; i++) {
            float yOffset = caveConstants.get("yOffset").asFloat();
            float x = caveObjectData.get(i).get(8).asFloat() + polarCaveTexture.getRegionWidth() / 2f;
            float y = maxY - caveObjectData.get(i).get(9).asFloat() + yOffset;
            x = drawToScreenCoordinates(x);
            y = drawToScreenCoordinates(y);
            Vector2 caveBubblePos = new Vector2(drawToScreenCoordinates(caveConstants.get("bubbleX").asFloat()), drawToScreenCoordinates(caveConstants.get("bubbleY").asFloat()));

            Cave obj = new Cave(getVertices(caveConstants), x, y, yOffset,textureScale, caveBubblePos);
            obj.setZZZTexture(caveZZZLoopFilm);
            obj.setDrawScale(scale);
            obj.setTexture(polarCaveTexture);
            obj.setName("cave" + i);

            addObject(obj);
            interactableCaves.add(obj);

            makeShadow(caveConstants,obj);

            // ===================g
            genericObstacles.add(new GenericObstacle(obj.getX(), obj.getY(),
                    obj.getWidth(), obj.getHeight()));
            // ===================
        }
    }

    /**
     * Renders all the decorations into the map by parsing JSON
     * If you change the sprite sheets, you need to adjust these numbers (ask William)
     */
    private void renderDecorations() {
        if(jsonData.get("layers").get(1) == null) { return; }
        JsonValue decorationData = jsonData.get("layers").get(1).get("objects");

        for(int i = 0; i < decorationData.size; i++) {
            float x = decorationData.get(i).get("x").asFloat();
            float y = maxY - decorationData.get(i).get("y").asFloat();
            int decorationIndex = decorationData.get(i).get("gid").asInt();
            int textureIndex;
            if(decorationIndex - firstSmallDecorationIndex < 12) {
                textureIndex = decorationIndex - firstSmallDecorationIndex;
            } else if(decorationIndex - firstLargeDecorationIndex < 2) {
                textureIndex = decorationIndex - firstLargeDecorationIndex + 12;
                Decoration decoration = new Decoration(decorationTextures[textureIndex], scale, drawToScreenCoordinates(x),drawToScreenCoordinates(y), decorationIndex + 12, textureScale);
                groundDecorations.add(decoration);
                continue;
            } else if((decorationIndex - firstLargeDecorationIndex - 2) < 5) {
                textureIndex = decorationIndex - firstLargeDecorationIndex + 12;
                Decoration decoration = new Decoration(decorationTextures[textureIndex], scale, drawToScreenCoordinates(x),drawToScreenCoordinates(y), decorationIndex + 12, textureScale);
                oceanDecorations.add(decoration);
                continue;
            } else if(decorationIndex - firstLargeOceanDecorationIndex < 2){
                textureIndex = decorationIndex - firstLargeOceanDecorationIndex + 19;
                Decoration decoration = new Decoration(decorationTextures[textureIndex], scale, drawToScreenCoordinates(x),drawToScreenCoordinates(y), decorationIndex + 12, textureScale);
                oceanDecorations.add(decoration);
                continue;
            } else {
                System.out.println("Unidentified decoration.");
                continue;
            }


            Decoration decoration = new Decoration(decorationTextures[textureIndex], scale, drawToScreenCoordinates(x),drawToScreenCoordinates(y), decorationIndex, textureScale);
            JsonValue propertyData = decorationData.get(i).get("properties");
            if(propertyData != null) {
                for(int j = 0; j < propertyData.size; j++) {
                    if(propertyData.get(j).get("name").asString().equals("dialogue_num")) {
                        int dialogueIndex = propertyData.get(j).get("value").asInt();
                        if(dialogueIndex >= 0 && dialogueIndex < dialogueTextures.length) {
                                decoration.setDialogueTexture(dialogueTextures[dialogueIndex]);
                        }
                    }
                }
            }


            decorations.add(decoration);
        }
        // Since decoration positions never change, we only need to sort once
        decorations.sort(decorationComparator);
    }

    /**
     * Gets vertices for the object corresponding to constants
     * @param constants constants for game object
     * @return vertices scaled according to textureScale
     */
    private float[] getVertices(JsonValue constants){
            if(constants.get("vertices") == null) {
                System.out.println("Undefined constants.");
                return null;
            }
            float[] vertices = constants.get("vertices").asFloatArray();
            for(int i = 0; i < vertices.length; i++) {
                vertices[i] *= textureScale / 0.75f;
            }
            return vertices;
    }

    /**
     * Initializes and adds a shadow into the game world.
     * @param constants constants corresponding to the object
     * @param obj object we are making the shadow for
     */
    private void makeShadow(JsonValue constants, PolygonObstacle obj) {
        float[] shadowVertices;
        if(constants.get("shadowVertices") == null) {
            shadowVertices = new float[]{0, -4, -4, -1, -6, 0.6f, 0, 20, 6, 0.6f, 4, -1};
        } else {
            shadowVertices = constants.get("shadowVertices").asFloatArray();
        }

        for(int i = 0; i < shadowVertices.length; i++) {
            shadowVertices[i] *= textureScale / 0.75f;
        }

        float xOffset = 0f;
        float yOffset = 0f;
        boolean moving = true;
        if(constants.get("shadowXOffset") != null) {
            xOffset = constants.get("shadowXOffset").asFloat();
        }
        if(constants.get("shadowYOffset") != null) {
            yOffset = constants.get("shadowYOffset").asFloat();
        }
        if(constants.get("doesShadowMove") != null) {
            moving = constants.get("doesShadowMove").asBoolean();
        }

        ShadowModel shadow = new ShadowModel(shadowVertices, obj.getX(), obj.getY(), xOffset, yOffset, moving);
        shadow.setDrawScale(scale);
        shadowController.addShadow(shadow);

        addObject(shadow);
        shadow.rotateDirection(shadowStartingRotation - 90);
    }

    public boolean updateAggro(AIController i) {


        for (AIController c : controls) {
            if (i.checkAggroNear(c)) {
                return true;
            }
        }

        return false;
    }

}